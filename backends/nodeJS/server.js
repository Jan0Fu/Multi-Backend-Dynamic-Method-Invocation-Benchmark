const express = require('express');
const cors = require('cors');
const { VM } = require('vm2');

const app = express();
app.use(cors());
app.use(express.json()); // <- JSON parser, lebo frontend posiela JSON

app.post('/node/run-js', (req, res) => {
  const script = req.body?.script;

  if (typeof script !== 'string') {
    return res.status(400).json({
      result: 'Chyba: neplatný formát požiadavky (očakáva sa JSON s "script")',
      durationMs: -1
    });
  }

  const vm = new VM();
  const start = Date.now();

  try {
    const result = vm.run(script);
    const durationMs = Date.now() - start;

    res.json({
      result: String(result),
      durationMs
    });
  } catch (e) {
    res.json({
      result: 'Chyba: ' + e.message,
      durationMs: -1
    });
  }
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Node.js backend beží na http://localhost:${PORT}`);
});