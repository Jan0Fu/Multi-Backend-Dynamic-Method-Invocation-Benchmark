import express from 'express';
import cors from "cors";
import { VM } from 'vm2';
import { performance } from 'perf_hooks';

const app = express();
app.use(cors());
app.use(express.json());

app.post('/run', (req, res) => {
  const { script, variables } = req.body;

  if (!script) {
    return res.status(400).json({ error: 'Missing script' });
  }

  try {
    const vm = new VM({
      timeout: 2000,
      sandbox: { vars: variables }
    });

    const start = performance.now();
    const result = vm.run(script);
    const end = performance.now();

    res.json({
      result,
      durationMs: Math.round(end - start), // celé ms pre jednotný formát
      success: true
    });

  } catch (error) {
    res.json({
      error: error.toString(),
      result: null,
      durationMs: -1,
      success: false
    });
  }
});

app.listen(3004, () => console.log('🟢 Node backend running on port 3004'));