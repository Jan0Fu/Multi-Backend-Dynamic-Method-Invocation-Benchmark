package com.example.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.*;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin(origins = "*")
public class ScriptController {

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/run-js")
    public Map<String, Object> runJs(@RequestBody Map<String, Object> request) {
        return runScript(request, "js");
    }

    @PostMapping("/run-python")
    public Map<String, Object> runPython(@RequestBody Map<String, Object> request) {
        return runScript(request, "python");
    }

    private Map<String, Object> runScript(Map<String, Object> request, String lang) {
        Map<String, Object> response = new HashMap<>();
        long start = System.currentTimeMillis();

        String script = (String) request.get("script");
        Map<String, Object> inputVars = safeMap(request.get("variables"));
        Map<String, Object> vars = new HashMap<>(inputVars);

        ObjectMapper mapper = new ObjectMapper();

        try (Context context = Context.newBuilder(lang).allowAllAccess(true).build()) {

            if ("js".equals(lang)) {
                // --- JavaScript ---
                String json = mapper.writeValueAsString(vars);
                Value result = context.eval("js",
                        "var vars = JSON.parse('" + json.replace("'", "\\'") + "');\n" +
                                script + "\n" +
                                "JSON.stringify(vars);"
                );
                String varsJson = result.asString();
                Map<String, Object> updated = mapper.readValue(varsJson, new TypeReference<Map<String, Object>>() {});
                vars.putAll(updated);

            } else if ("python".equals(lang)) {
                // --- Python ---
                StringBuilder init = new StringBuilder("vars = {}\n");
                for (Map.Entry<String, Object> e : vars.entrySet()) {
                    Object val = e.getValue();
                    if (val instanceof Number) {
                        init.append(String.format("vars['%s'] = %s\n", e.getKey(), val));
                    } else {
                        init.append(String.format("vars['%s'] = '%s'\n", e.getKey(), val));
                    }
                }

                String fullScript = init + "\n" + script + "\n" + "import json\nvars_json = json.dumps(vars)\n";
                context.eval("python", fullScript);

                Value bindings = context.getBindings("python");
                Value varsValue = bindings.getMember("vars_json");

                String varsJson = (varsValue != null && !varsValue.isNull())
                        ? varsValue.asString()
                        : mapper.writeValueAsString(vars); // fallback

                Map<String, Object> updated = mapper.readValue(varsJson, new TypeReference<Map<String, Object>>() {});
                vars.putAll(updated);
            }

            long duration = System.currentTimeMillis() - start;
            response.put("result", vars);
            response.put("variables", vars);
            response.put("durationMs", duration);
            response.put("status", "OK");

        } catch (Exception e) {
            response.put("result", "Chyba: " + e.getMessage());
            response.put("variables", vars);
            response.put("durationMs", -1);
            response.put("status", "Chyba");
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        return new HashMap<>();
    }
}