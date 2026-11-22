package com.gdx.scenarios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ScenarioDefinition {

    @FunctionalInterface
    public interface Setup {
        void run(ScenarioContext context);
    }

    @FunctionalInterface
    public interface Tick {
        void run(ScenarioContext context, float elapsed, float dt);
    }

    @FunctionalInterface
    public interface Assertion {
        void verify(ScenarioContext context);
    }

    public record Checkpoint(float time, String name, Assertion assertion) {}
    public record Capture(float time, String name) {}

    private final String name;
    private final Set<String> tags;
    private final long seed;
    private final float duration;
    private final Setup setup;
    private final Tick tick;
    private final List<Checkpoint> checkpoints;
    private final List<Capture> captures;

    ScenarioDefinition(String name, Set<String> tags, long seed, float duration,
                       Setup setup, Tick tick, List<Checkpoint> checkpoints,
                       List<Capture> captures) {
        this.name = name;
        this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        this.seed = seed;
        this.duration = duration;
        this.setup = setup;
        this.tick = tick;
        this.checkpoints = Collections.unmodifiableList(new ArrayList<>(checkpoints));
        this.captures = Collections.unmodifiableList(new ArrayList<>(captures));
    }

    public String name() { return name; }
    public Set<String> tags() { return tags; }
    public long seed() { return seed; }
    public float duration() { return duration; }
    public Setup setup() { return setup; }
    public Tick tick() { return tick; }
    public List<Checkpoint> checkpoints() { return checkpoints; }
    public List<Capture> captures() { return captures; }
    public boolean visual() { return !captures.isEmpty(); }
}
