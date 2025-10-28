import express from "express";
import cors from "cors";
import { VM } from "vm2";

const app = express();
app.use(cors());
app.use(express.json());

app.post("/run", (req, res) => {
  const { script, variables } = req.body;
  if (!script) return res.status(400).json({ result: "Missing script" });

  try {
    const vm = new VM({
      timeout: 2000,
      sandbox: { vars: { ...variables } }
    });

    const start = Date.now();
    vm.run(script);
    const result = vm.run("JSON.stringify(vars)");
    const end = Date.now();

    const updatedVars = JSON.parse(result);

    res.json({
      result,
      variables: updatedVars,
      durationMs: end - start
    });
  } catch (error) {
    res.json({
      result: "Chyba: " + error.toString(),
      variables: variables,
      durationMs: -1
    });
  }
});

app.listen(3004, () => console.log("🟢 Node backend running on port 3004"));