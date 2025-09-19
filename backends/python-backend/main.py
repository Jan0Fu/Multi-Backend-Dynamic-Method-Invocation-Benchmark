from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import time
import execjs

app = FastAPI()

# Povolenie CORS, aby frontend mohol volať API
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

    try:
        start = time.time()
        ctx = execjs.get().compile(script)

        # Posledný riadok by mal byť volanie funkcie
        lines = [line.strip() for line in script.strip().splitlines() if line.strip()]
        last_line = lines[-1]

        # Predpokladáme, že posledný riadok je volanie: fib(10)
        func_name = last_line.split("(")[0]
        args = last_line[last_line.find("(")+1:last_line.find(")")]
        args = [int(a.strip()) for a in args.split(",") if a.strip()]

        result = ctx.call(func_name, *args)
        duration = int((time.time() - start) * 1000)

        return {"result": str(result), "durationMs": duration}

    except Exception as e:
        return {"result": f"Chyba: {str(e)}", "durationMs": -1}
    
@app.post("/python/run-py")
async def run_python(request: Request):
    data = await request.json()
    script = data.get("script", "")
    local_vars = {}

    try:
        local_vars = {}
        start = time.time()
        exec(script, local_vars) 

        # posledný výraz
        lines = [line.strip() for line in script.strip().splitlines() if line.strip()]
        result = None
        for line in reversed(lines):
            if not line.startswith("def "):
                result = eval(line, local_vars)
                break
        duration = int((time.time() - start) * 1000)
        return {"result": str(result), "durationMs": duration}
    except Exception as e:
        return {"result": f"Chyba: {str(e)}", "durationMs": -1}