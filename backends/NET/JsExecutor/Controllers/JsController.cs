using Microsoft.AspNetCore.Mvc;
using Jint;
using System.Diagnostics;
using System.Text.Json;

namespace DynamicBenchmark.Controllers
{
    [ApiController]
    [Route("api/js")]
    public class JsController : ControllerBase
    {
        public record ScriptRequest(string Script, JsonElement Variables);

        [HttpPost]
        public IActionResult RunScript([FromBody] ScriptRequest request)
        {
            try
            {
                var engine = new Engine();

                // Deserialize na Dictionary<string, object> a pretypovať na čísla
                var varsDict = new Dictionary<string, object>();
                foreach (var prop in request.Variables.EnumerateObject())
                {
                    if (prop.Value.ValueKind == JsonValueKind.Number)
                        varsDict[prop.Name] = prop.Value.GetDouble(); // alebo GetInt32() ak sú celé čísla
                    else if (prop.Value.ValueKind == JsonValueKind.String)
                    {
                        if (double.TryParse(prop.Value.GetString(), out var num))
                            varsDict[prop.Name] = num;
                        else
                            varsDict[prop.Name] = prop.Value.GetString();
                    }
                    else
                        varsDict[prop.Name] = prop.Value.GetString();
                }

                engine.SetValue("vars", varsDict);

                var sw = Stopwatch.StartNew();
                var result = engine.Evaluate(request.Script);
                sw.Stop();

                return Ok(new
                {
                    result = result.ToObject(),
                    variables = varsDict,
                    durationMs = sw.ElapsedMilliseconds
                });
            }
            catch (Exception ex)
            {
                return Ok(new
                {
                    result = $"Chyba: {ex.Message}",
                    variables = request.Variables,
                    durationMs = -1
                });
            }
        }
    }
}