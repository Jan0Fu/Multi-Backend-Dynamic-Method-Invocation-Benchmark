using System.Diagnostics;
using Acornima.Ast;
using Jint;
using Microsoft.AspNetCore.Mvc;

namespace JsExecutor.Controllers
{
    [ApiController]
    [Route("api/js")]
    public class JsController : ControllerBase
    {
        public class ScriptRequest
        {
            public string Script { get; set; }
        }
        
        [HttpPost]
        public IActionResult RunScript([FromBody] ScriptRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Script))
            {
                return BadRequest(new { error = "Script is required" });
            }

            try
            {
                var engine = new Engine();

                var stopwatch = Stopwatch.StartNew();
                var result = engine.Evaluate(request.Script);
                stopwatch.Stop();

                return Ok(new
                {
                    result = result.ToObject()?.ToString(),
                    durationMs = stopwatch.ElapsedMilliseconds
                });
            }
            catch (Exception ex)
            {
                return BadRequest(new
                {
                    error = ex.Message,
                    durationMs = -1
                });
            }
        }
    }
}