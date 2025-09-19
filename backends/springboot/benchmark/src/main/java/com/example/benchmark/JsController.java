package com.example.benchmark;

import org.graalvm.polyglot.*;
import org.springframework.web.bind.annotation.*;
import groovy.lang.GroovyShell;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin  // umožní CORS pre frontend
public class JsController {

    // DTO pre prijatie JSON
    public static class ScriptRequest {
        private String script;
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
    }

    @PostMapping("/run-js")
    public Map<String, Object> runJs(@RequestBody ScriptRequest request) {
        String script = request.getScript();
        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
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

    @PostMapping("/run-groovy")
    public Map<String, Object> runGroovy(@RequestBody ScriptRequest request) {
        String script = request.getScript();

        try (Context context = Context.newBuilder("groovy").allowAllAccess(true).build()) {
            GroovyShell shell = new GroovyShell();
            Instant start = Instant.now();
            Object result = shell.evaluate(script);
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

    @PostMapping("/run-python")
    public Map<String, Object> runPython(@RequestBody ScriptRequest request) {
        String script = request.getScript();
        try (Context context = Context.newBuilder("python").allowAllAccess(true).build()) {
            Instant start = Instant.now();
            Value result = context.eval("python", script);
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