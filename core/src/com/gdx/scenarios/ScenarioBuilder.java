package com.gdx.scenarios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ScenarioBuilder {

    private final String name;
    private final Set<String> tags = new LinkedHashSet<>();
    private final List<ScenarioDefinition.Checkpoint> checkpoints = new ArrayList<>();
    private final List<ScenarioDefinition.Capture> captures = new ArrayList<>();
    private long seed = 0x415354524fL;
    private float duration = 1f;
    private ScenarioDefinition.Setup setup = context -> {};
    private ScenarioDefinition.Tick tick = (context, elapsed, dt) -> {};

    private ScenarioBuilder(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scenario name is required");
        this.name = name;
    }

    public static ScenarioBuilder scenario(String name) {
        return new ScenarioBuilder(name);
    }

    public ScenarioBuilder tags(String... values) {
        for (String value : Arrays.asList(values)) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Scenario tag is required");
            tags.add(value);
        }
        return this;
    }

    public ScenarioBuilder seed(long value) {
        seed = value;
        return this;
    }

    public ScenarioBuilder duration(float seconds) {
        if (seconds <= 0f) throw new IllegalArgumentException("Scenario duration must be positive");
        duration = seconds;
        return this;
    }

    public ScenarioBuilder setup(ScenarioDefinition.Setup value) {
        setup = requireValue(value, "Scenario setup is required");
        return this;
    }

    public ScenarioBuilder tick(ScenarioDefinition.Tick value) {
        tick = requireValue(value, "Scenario tick is required");
        return this;
    }

    public ScenarioBuilder checkAt(float time, String checkName, ScenarioDefinition.Assertion assertion) {
        checkpoints.add(new ScenarioDefinition.Checkpoint(requireTime(time), requireName(checkName, "check"),
                requireValue(assertion, "Scenario assertion is required")));
        return this;
    }

    public ScenarioBuilder captureAt(float time, String captureName) {
        captures.add(new ScenarioDefinition.Capture(requireTime(time), requireName(captureName, "capture")));
        return this;
    }

    public ScenarioDefinition build() {
        for (ScenarioDefinition.Checkpoint checkpoint : checkpoints) {
            if (checkpoint.time() > duration) {
                throw new IllegalArgumentException("Check '" + checkpoint.name()
                        + "' occurs after scenario duration");
            }
        }
        for (ScenarioDefinition.Capture capture : captures) {
            if (capture.time() > duration) {
                throw new IllegalArgumentException("Capture '" + capture.name()
                        + "' occurs after scenario duration");
            }
        }
        if (tags.contains("logic") && checkpoints.isEmpty()) {
            throw new IllegalArgumentException("Logic scenario requires at least one check");
        }
        checkpoints.sort((a, b) -> Float.compare(a.time(), b.time()));
        captures.sort((a, b) -> Float.compare(a.time(), b.time()));
        return new ScenarioDefinition(name, tags, seed, duration, setup, tick, checkpoints, captures);
    }

    private static float requireTime(float time) {
        if (!Float.isFinite(time) || time < 0f) {
            throw new IllegalArgumentException("Scenario time must be finite and non-negative");
        }
        return time;
    }

    private static String requireName(String value, String kind) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Scenario " + kind + " name is required");
        }
        return value;
    }

    private static <T> T requireValue(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
