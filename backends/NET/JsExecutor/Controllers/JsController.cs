using System.Diagnostics;
using Jint;
using Microsoft.AspNetCore.Mvc;

namespace JsExecutor.Controllers
{
    [ApiController]
    [Route("api/run")]
    public class JsController : ControllerBase
    {
        public class ScriptRequest
        {
            public string Script { get; set; } = string.Empty;
            public string Language { get; set; } = "js";
        }

        [HttpPost]
        public IActionResult Run([FromBody] ScriptRequest request)
        {
            if (request.Language != "js")
            {
                return BadRequest(new
                {
                    result = $"Nepodporovaný jazyk: {request.Language}",
                    durationMs = -1
                });
            }

            try
            {
                var sw = Stopwatch.StartNew();
                var engine = new Engine();
                var result = engine.Evaluate(request.Script);
                sw.Stop();

                return Ok(new
                {
                    result = result.ToString(),
                    durationMs = sw.ElapsedMilliseconds
                });
            }
            catch (Exception ex)
            {
                return Ok(new
                {
                    result = "Chyba: " + ex.Message,
                    durationMs = -1
                });
            }
        }
    }
}