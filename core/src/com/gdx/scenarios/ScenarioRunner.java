package com.gdx.scenarios;

import java.util.ArrayDeque;
import java.util.Queue;

public final class ScenarioRunner {

    public record CaptureRequest(String name) {}

    private final ScenarioDefinition definition;
    private final ScenarioContext context;
    private final ScenarioResult result;
    private final boolean capturesEnabled;
    private final Float captureTimeOverride;
    private final float duration;
    private final Queue<CaptureRequest> pendingCaptures = new ArrayDeque<>();
    private float elapsed;
    private int nextCheckpoint;
    private int nextCapture;
    private boolean complete;

    ScenarioRunner(ScenarioDefinition definition, ScenarioContext context, boolean capturesEnabled,
                   Float captureTimeOverride) {
        this.definition = definition;
        this.context = context;
        this.capturesEnabled = capturesEnabled;
        this.captureTimeOverride = captureTimeOverride;
        this.duration = captureTimeOverride == null
                ? definition.duration()
                : Math.max(definition.duration(), captureTimeOverride + 0.2f);
        this.result = new ScenarioResult(definition.name(), definition.seed());
    }

    public void setup() {
        try {
            definition.setup().run(context);
            result.pass("setup");
        } catch (Throwable error) {
            result.fail("setup", error);
            complete = true;
        }
    }

    public void beforeUpdate(float dt) {
        if (complete) return;
        try {
            definition.tick().run(context, elapsed, dt);
        } catch (Throwable error) {
            result.fail("tick", error);
            complete = true;
        }
    }

    public void afterUpdate(float dt) {
        if (complete) return;
        elapsed += dt;

        while (nextCheckpoint < definition.checkpoints().size()
                && elapsed >= definition.checkpoints().get(nextCheckpoint).time()) {
            ScenarioDefinition.Checkpoint checkpoint = definition.checkpoints().get(nextCheckpoint++);
            try {
                checkpoint.assertion().verify(context);
                result.pass(checkpoint.name());
            } catch (Throwable error) {
                result.fail(checkpoint.name(), error);
                complete = true;
                break;
            }
        }

        if (capturesEnabled) {
            while (nextCapture < definition.captures().size()
                    && elapsed >= captureTime(nextCapture)) {
                pendingCaptures.add(new CaptureRequest(definition.captures().get(nextCapture++).name()));
            }
        }

        if (!complete && elapsed >= duration) {
            result.pass("completed");
            complete = true;
        }
        result.setElapsed(elapsed);
    }

    public CaptureRequest pollCapture() {
        return pendingCaptures.poll();
    }

    public boolean hasPendingCaptures() {
        return !pendingCaptures.isEmpty();
    }

    public boolean isComplete() { return complete; }
    public ScenarioResult result() { return result; }
    public ScenarioDefinition definition() { return definition; }

    private float captureTime(int index) {
        if (index == 0 && captureTimeOverride != null) return captureTimeOverride;
        return definition.captures().get(index).time();
    }
}
