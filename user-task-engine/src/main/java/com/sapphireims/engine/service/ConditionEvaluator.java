package com.sapphireims.engine.service;

import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class ConditionEvaluator {
    private final ScriptEngine engine;

    public ConditionEvaluator() {
        this.engine = new ScriptEngineManager().getEngineByName("freemarker");
    }

    public boolean evaluate(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        try {
            Object result = engine.eval(expression, new SimpleBindings(variables));
            return Boolean.parseBoolean(String.valueOf(result).trim());
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        try {
            Object result = engine.eval(template, new SimpleBindings(variables));
            return result == null ? null : result.toString();
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to render template: " + template, e);
        }
    }
}
