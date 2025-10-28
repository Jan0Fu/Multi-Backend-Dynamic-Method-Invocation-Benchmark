using Microsoft.AspNetCore.Mvc;
using Jint;
using IronPython.Hosting;
using System.Diagnostics;
using System.Text.Json;

namespace DynamicBenchmark.Controllers
{
    [ApiController]
    [Route("api")]
    public class DynamicController : ControllerBase
    {
        public record ScriptRequest(string Script, JsonElement Variables);

        // ===========================================================
        // === JAVASCRIPT ENDPOINT (Jint) =============================
        // ===========================================================
        [HttpPost("js")]
        public IActionResult RunJs([FromBody] ScriptRequest request)
        {
            try
            {
                var engine = new Engine(options =>
                {
                    options.TimeoutInterval(TimeSpan.FromSeconds(2));
                });

                // Premenné (JSON → Dictionary)
                var varsDict = new Dictionary<string, object>();
                foreach (var prop in request.Variables.EnumerateObject())
                {
                    object value;
                    if (prop.Value.ValueKind == JsonValueKind.Number)
                        value = prop.Value.GetDouble();
                    else if (prop.Value.ValueKind == JsonValueKind.String)
                        value = prop.Value.GetString();
                    else
                        value = prop.Value.ToString();
                    varsDict[prop.Name] = value;
                }

                // Poslanie do JS
                engine.SetValue("vars", varsDict);

                // Obnova funkcií z reťazcov
                string restoreFunctions = @"
                    for (let key in vars) {
                      if (typeof vars[key] === 'string' && vars[key].trim().startsWith('function')) {
                        try { vars[key] = eval('(' + vars[key] + ')'); } catch (e) {}
                      }
                    }";

                var sw = Stopwatch.StartNew();
                engine.Execute(restoreFunctions);
                var result = engine.Evaluate(request.Script);
                var updatedVars = engine.GetValue("vars").ToObject() as IDictionary<string, object>;
                sw.Stop();

                return Ok(new
                {
                    result = result?.ToObject(),
                    variables = updatedVars ?? varsDict,
                    durationMs = sw.Elapsed.TotalMilliseconds.ToString("F0"),
                    status = "OK"
                });
            }
            catch (Exception ex)
            {
                return Ok(new
                {
                    result = "Chyba: " + ex.Message,
                    variables = request.Variables,
                    durationMs = -1,
                    status = "Chyba"
                });
            }
        }

        // ===========================================================
        // === PYTHON ENDPOINT (IronPython) ==========================
        // ===========================================================
        [HttpPost("python")]
        public IActionResult RunPython([FromBody] ScriptRequest request)
        {
            try
            {
                var engine = Python.CreateEngine();
                var scope = engine.CreateScope();

                // Premenné (JSON → Python scope)
                var varsDict = new Dictionary<string, object>();
                foreach (var prop in request.Variables.EnumerateObject())
                {
                    object value;
                    if (prop.Value.ValueKind == JsonValueKind.Number)
                        value = prop.Value.GetDouble();
                    else if (prop.Value.ValueKind == JsonValueKind.String)
                        value = prop.Value.GetString();
                    else
                        value = prop.Value.ToString();
                    varsDict[prop.Name] = value;
                }

                scope.SetVariable("vars", varsDict);

                var sw = Stopwatch.StartNew();
                engine.Execute(request.Script, scope);
                sw.Stop();

                // Výsledok z "vars"
                var updatedVars = scope.GetVariable("vars") as IDictionary<string, object>;

                return Ok(new
                {
                    result = updatedVars != null ? JsonSerializer.Serialize(updatedVars) : "(žiadny)",
                    variables = updatedVars ?? varsDict,
                    durationMs = sw.Elapsed.TotalMilliseconds.ToString("F0"),
                    status = "OK"
                });
            }
            catch (Exception ex)
            {
                return Ok(new
                {
                    result = "Chyba: " + ex.Message,
                    variables = request.Variables,
                    durationMs = -1,
                    status = "Chyba"
                });
            }
        }
    }
}