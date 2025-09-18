package com.example.benchmark;

import org.graalvm.polyglot.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin
public class PolyglotController {

    @PostMapping("/run")
    public Map<String, Object> runScript(@RequestBody Map<String, String> body) {
        String script = body.get("script");
        String language = body.get("language"); // "js", "groovy", "python"

        try (Context context = Context.newBuilder(language).allowAllAccess(true).build()) {
            Instant start = Instant.now();
            Value result = context.eval(language, script);
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
