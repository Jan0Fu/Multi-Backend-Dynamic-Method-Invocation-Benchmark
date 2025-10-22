package com.example.benchmark;

import org.springframework.web.bind.annotation.*;
import org.graalvm.polyglot.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin(origins = "*")
public class ScriptController {

    @PostMapping("/run-js")
    public Map<String, Object> runJs(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
            String script = (String) request.get("script");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");

            // --- Vytvorenie JS objektu vars z premenných ---
            StringBuilder jsInit = new StringBuilder("var vars = {");
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        jsInit.append(entry.getKey()).append(": '").append(value).append("',");
                    } else {
                        jsInit.append(entry.getKey()).append(": ").append(value).append(",");
                    }
                }
            }
            jsInit.append("};\n");

            // --- Spustenie JS kódu ---
            Value output = context.eval("js", jsInit.toString() + script);
            long duration = System.currentTimeMillis() - start;

            result.put("result", output.isNull() ? "(žiadny)" : output.toString());
            result.put("variables", variables);
            result.put("durationMs", duration);
            result.put("status", "OK");

        } catch (Exception e) {
            result.put("result", "Chyba: " + e.getMessage());
            result.put("variables", request.get("variables"));
            result.put("durationMs", -1);
            result.put("status", "Chyba");
        }

        return result;
    }
}