package com.example.benchmark;

import org.graalvm.polyglot.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin // umožní CORS pre frontend
public class JsController {

    public static class ScriptRequest {
        public String script;  // musí byť public a mať getter/setter alebo byť public pre Jackson
    }

    @PostMapping("/run-js")
    public Map<String, Object> runScript(@RequestBody ScriptRequest request) {
        String script = request.script;
        try (Context context = Context.newBuilder("js").allowAllAccess(false).build()) {
            Instant start = Instant.now();
            Value result = context.eval("js", script);
            Instant end = Instant.now();

            return Map.of(
                    "result", result.toString(),
                    "durationMs", Duration.between(start, end).toMillis()
            );
        } catch (Exception e) {
            return Map.of(
                    "result", "Chyba: " + e.getMessage(),
                    "durationMs", -1
            );
        }
    }
}
