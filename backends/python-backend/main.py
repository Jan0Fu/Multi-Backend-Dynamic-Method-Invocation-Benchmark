from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import time
import execjs
import traceback

app = FastAPI()

# Povolenie CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Endpoint pre JS skripty ---
@app.post("/python/run-js")
async def run_js(request: Request):
    data = await request.json()
    script = data.get("script", "")
    variables = data.get("variables", {})

    try:
        start = time.time()
        vars_code = f"const vars = {variables};\n"
        lines = script.strip().splitlines()
        expression = lines[-1].rstrip(";") if lines else ""
        body = "\n".join(lines[:-1])
        ctx = execjs.get().compile(vars_code + body)
        result = ctx.eval(expression)
        duration = int((time.time() - start) * 1000)

        return {
            "result": str(result),
            "variables": variables,
            "durationMs": round(duration, 3),
            "status": "OK"
        }

    except Exception as e:
        return {
            "result": f"Chyba: {str(e)}\n{traceback.format_exc()}",
            "variables": variables,
            "durationMs": -1,
            "status": "Chyba"
        }

# --- Endpoint pre JS skripty ---
@app.post("/python/run-python")
async def run_python(request: Request):
    data = await request.json()
    script = data.get("script", "")
    variables = data.get("variables", {})

    # Vytvoríme jedno prostredie pre exec a eval
    env = variables.copy()  # začneme s premennými od frontendu

    try:
        start = time.time()

        lines = script.strip().splitlines()
        if len(lines) == 0:
            return {"result": "(žiadny)", "variables": variables, "durationMs": 0}

        body = "\n".join(lines[:-1])  # všetko okrem posledného riadku
        last_expr = lines[-1]          # posledný riadok

        # Spustíme definície a kód v jednom prostredí
        exec(body, env, env)

        # Vyhodnotíme posledný výraz v tom istom prostredí
        result = eval(last_expr, env, env)

        duration = int((time.time() - start) * 1000)

        return {
            "result": result,
            "variables": variables,
            "durationMs": duration,
            "status": "OK"
        }

    except Exception as e:
        duration = round((time.time() - start) * 1000, 3)
        return {
            "result": f"Chyba: {str(e)}",
            "variables": variables,
            "durationMs": duration,
            "status": "Chyba"
        }