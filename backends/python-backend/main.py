from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import time
import execjs
import json

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/python/run-js")
async def run_js(request: Request):
    data = await request.json()
    script = data.get("script", "")
    variables = data.get("variables", {})

    try:
        start = time.time()
        ctx = execjs.get().compile(f"var vars = {json.dumps(variables)};\n{script}\nJSON.stringify(vars);")
        result_json = ctx.eval("JSON.stringify(vars)")
        updated_vars = json.loads(result_json)

        duration = int((time.time() - start) * 1000)
        return {
            "result": result_json,
            "variables": updated_vars,
            "durationMs": duration
        }

    except Exception as e:
        return {
            "result": f"Chyba: {str(e)}",
            "variables": variables,
            "durationMs": -1
        }
    
@app.post("/python/run-python")
async def run_python(request: Request):
    """
    Spúšťa Python kód natívne pomocou exec().
    Premenné z requestu sú prístupné ako 'vars'.
    """
    data = await request.json()
    script = data.get("script", "")
    variables = data.get("variables", {}) or {}

    try:
        start = time.time()
        # 🔧 dôležité: použijeme JEDEN spoločný scope
        scope = {"vars": variables}

        exec(script, scope)  # nie (script, {}, local_vars)
        # teraz fib() a všetky funkcie ostanú dostupné v scope

        result = scope.get("result") or scope["vars"].get("result", "(žiadny)")
        duration = int((time.time() - start) * 1000)

        return {
            "result": str(result),
            "variables": scope.get("vars", variables),
            "durationMs": duration,
            "status": "OK"
        }

    except Exception as e:
        return {
            "result": f"Chyba: {e}",
            "variables": variables,
            "durationMs": -1,
            "status": "Chyba"
        }