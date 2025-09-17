from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import time
import execjs

app = FastAPI()

# povolenie CORS (aby frontend mohol volať API)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # pre testovanie môže byť "*"
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/python/run-js")
async def run_script(request: Request):
    data = await request.json()
    script = data.get("script", "")

    try:
        start = time.time()

        # Rozdelí skript po riadkoch
        lines = script.strip().splitlines()

        # posledný riadok je výraz na vyhodnotenie (napr. fib(20))
        expression = lines[-1].rstrip(";")
        body = "\n".join(lines[:-1])

        ctx = execjs.get().compile(body)
        result = ctx.eval(expression)

        duration = int((time.time() - start) * 1000)
        return {"result": str(result), "durationMs": duration}

    except Exception as e:
        return {"result": f"Chyba: {str(e)}", "durationMs": -1}