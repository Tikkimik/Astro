package com.gdx.scenarios;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScenarioResult {

    public record Check(String name, boolean passed, String detail) {}

    private final String scenario;
    private final long seed;
    private final List<Check> checks = new ArrayList<>();
    private final List<String> screenshots = new ArrayList<>();
    private float elapsed;

    ScenarioResult(String scenario, long seed) {
        this.scenario = scenario;
        this.seed = seed;
    }

    public void pass(String name) {
        checks.add(new Check(name, true, "ok"));
    }

    public void fail(String name, Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        checks.add(new Check(name, false, message));
    }

    public void addScreenshot(String path) {
        screenshots.add(path);
    }

    public void setElapsed(float elapsed) {
        this.elapsed = elapsed;
    }

    public boolean passed() {
        for (Check check : checks) if (!check.passed()) return false;
        return true;
    }

    public String scenario() { return scenario; }

    public void write(String outputDirectory) {
        FileHandle directory = Gdx.files.absolute(outputDirectory + "/" + scenario);
        directory.mkdirs();
        directory.child("result.txt").writeString(toText(), false, "UTF-8");
        directory.child("result.json").writeString(toJson(), false, "UTF-8");
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(passed() ? "PASS " : "FAIL ").append(scenario).append('\n');
        text.append("seed: ").append(seed).append('\n');
        text.append(String.format(Locale.ROOT, "elapsed: %.3f%n", elapsed));
        for (Check check : checks) {
            text.append(check.passed() ? "PASS " : "FAIL ")
                    .append(check.name()).append(": ").append(check.detail()).append('\n');
        }
        for (String screenshot : screenshots) text.append("screenshot: ").append(screenshot).append('\n');
        return text.toString();
    }

    private String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"scenario\": \"").append(escape(scenario)).append("\",")
                .append("\n  \"passed\": ").append(passed()).append(',')
                .append("\n  \"seed\": ").append(seed).append(',')
                .append(String.format(Locale.ROOT, "%n  \"elapsed\": %.3f,", elapsed))
                .append("\n  \"checks\": [");
        for (int i = 0; i < checks.size(); i++) {
            Check check = checks.get(i);
            if (i > 0) json.append(',');
            json.append("\n    {\"name\": \"").append(escape(check.name()))
                    .append("\", \"passed\": ").append(check.passed())
                    .append(", \"detail\": \"").append(escape(check.detail())).append("\"}");
        }
        json.append("\n  ],\n  \"screenshots\": [");
        for (int i = 0; i < screenshots.size(); i++) {
            if (i > 0) json.append(", ");
            json.append('\"').append(escape(screenshots.get(i))).append('\"');
        }
        json.append("]\n}\n");
        return json.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
