package com.example.benchmark;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.web.bind.annotation.*;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/java")
@CrossOrigin(origins = "*")
public class ScriptController {

    @PostMapping("/run-js")
    public Map<String, Object> runJs(@RequestBody Map<String, Object> request) {
        return runScript(request, "js");
    }

    @PostMapping("/run-python")
    public Map<String, Object> runPython(@RequestBody Map<String, Object> request) {
        return runScript(request, "python");
    }

    @PostMapping("/run-groovy")
    public Map<String, Object> runGroovy(@RequestBody Map<String, Object> request) {
        return runScript(request, "groovy");
    }

    private Map<String, Object> runScript(Map<String, Object> request, String language) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            String script = (String) request.get("script");
            Map<String, Object> variables = request.get("variables") != null
                    ? (Map<String, Object>) request.get("variables")
                    : new HashMap<>();

            Object output;

            if ("groovy".equalsIgnoreCase(language)) {
                // --- Groovy ---
                Binding binding = new Binding();
                binding.setVariable("vars", variables);

                GroovyShell shell = new GroovyShell(binding);
                output = shell.evaluate(script);

            } else {
                // --- JS / Python cez GraalVM ---
                try (Context context = Context.newBuilder(language).allowAllAccess(true).build()) {
                    StringBuilder init = new StringBuilder();
                    if (variables != null && !variables.isEmpty()) {
                        if ("js".equalsIgnoreCase(language)) {
                            init.append("var vars = {");
                            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                                Object value = entry.getValue();
                                if (value instanceof String) {
                                    init.append(entry.getKey()).append(": '").append(value).append("',");
                                } else {
                                    init.append(entry.getKey()).append(": ").append(value).append(",");
                                }
                            }
                            init.append("};\n");
                        } else if ("python".equalsIgnoreCase(language)) {
                            context.getBindings("python").putMember("vars", variables);
                            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                                Object value = entry.getValue();
                                if (value instanceof String) {
                                    init.append(entry.getKey()).append(" = '").append(value).append("'\n");
                                } else {
                                    init.append(entry.getKey()).append(" = ").append(value).append("\n");
                                }
                            }
                        }
                    }

                    Value outputValue = context.eval(language, init.toString() + script);
                    output = outputValue.isNull() ? "(žiadny)" : outputValue.toString(); // <-- bezpečne vo vnútri kontextu
                }
            }

            long duration = System.currentTimeMillis() - start;
            result.put("result", output == null ? "(žiadny)" : output.toString());
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