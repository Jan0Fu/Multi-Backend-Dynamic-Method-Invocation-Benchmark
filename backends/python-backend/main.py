from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import time
import execjs

app = FastAPI()

# Povolenie CORS pre frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # pre testovanie môže byť "*"
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/python/run-js")
async def run_js(request: Request):
    data = await request.json()
    script = data.get("script", "")
    variables = data.get("variables", {})  # očakávame dict s premennými

    try:
        start = time.time()

        # Posielame premenné do JS kódu
        vars_code = f"const vars = {variables};\n"

        # Rozdelíme skript po riadkoch
        lines = script.strip().splitlines()
        if len(lines) == 0:
            return {"result": "(žiadny)", "variables": variables, "durationMs": 0}

        # Posledný riadok je výraz na vyhodnotenie
        expression = lines[-1].rstrip(";")
        body = "\n".join(lines[:-1])

        ctx = execjs.get().compile(vars_code + body)
        result = ctx.eval(expression)

        duration = int((time.time() - start) * 1000)
        return {"result": str(result), "variables": variables, "durationMs": duration}

    except Exception as e:
        return {"result": f"Chyba: {str(e)}", "variables": variables, "durationMs": -1}