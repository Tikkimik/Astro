package com.gdx.scenarios;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.gamestates.PlayState;
import com.gdx.utils.GameLogger;

public final class ScenarioRuntime {

    private static ScenarioDefinition definition;
    private static ScenarioRunner runner;
    private static String mode;
    private static String outputDirectory;
    private static String legacyOutput;
    private static Float legacyCaptureTime;
    private static boolean finalized;

    private ScenarioRuntime() {}

    public static void prepareFromProperties() {
        String name = System.getProperty("astro.scenario", "");
        String legacyName = System.getProperty("astro.visualTest", "");
        if (name.isEmpty()) name = legacyName;
        if (name.isEmpty()) return;

        definition = ScenarioRegistry.require(name);
        mode = System.getProperty("astro.scenarioMode", definition.visual() ? "visual" : "logic");
        if (!"logic".equalsIgnoreCase(mode) && !"visual".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Unknown scenario mode '" + mode
                    + "'. Expected logic or visual");
        }
        outputDirectory = System.getProperty("astro.scenarioOutputDir",
                Gdx.files.getLocalStoragePath() + "build/scenarios");
        legacyOutput = System.getProperty("astro.visualOutput", "");
        String captureTime = System.getProperty("astro.visualCaptureTime", "");
        legacyCaptureTime = legacyName.isEmpty() || captureTime.isEmpty()
                ? null
                : Float.parseFloat(captureTime);
        MathUtils.random.setSeed(definition.seed());
    }

    public static ScenarioRunner attach(PlayState playState) {
        if (definition == null) return null;
        if (runner != null) throw new IllegalStateException("Scenario runner is already attached");
        runner = new ScenarioRunner(definition, new ScenarioContext(playState),
                isVisualMode(), legacyCaptureTime);
        runner.setup();
        return runner;
    }

    public static boolean isActive() { return definition != null; }
    public static boolean isLogicMode() { return "logic".equalsIgnoreCase(mode); }
    public static boolean isVisualMode() { return !isLogicMode(); }
    public static long seed() { return definition != null ? definition.seed() : 0L; }
    public static ScenarioRunner runner() { return runner; }

    public static String pollCapturePath() {
        if (runner == null) return null;
        ScenarioRunner.CaptureRequest request = runner.pollCapture();
        if (request == null) return null;
        if (!legacyOutput.isEmpty() && runner.result().scenario().equals(System.getProperty("astro.visualTest", ""))) {
            return legacyOutput;
        }
        return outputDirectory + "/" + runner.result().scenario() + "/" + request.name() + ".png";
    }

    public static void captureCompleted(String path) {
        runner.result().addScreenshot(path);
    }

    public static boolean shouldFinalize() {
        return runner != null && runner.isComplete() && !runner.hasPendingCaptures() && !finalized;
    }

    public static void finalizeAndExit() {
        finalized = true;
        runner.result().write(outputDirectory);
        String report = runner.result().toText();
        if (runner.result().passed()) {
            GameLogger.info(report.trim());
            Gdx.app.exit();
        } else {
            throw new IllegalStateException(report.trim());
        }
    }
}
