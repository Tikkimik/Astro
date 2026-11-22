package com.gdx.scenarios;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.OrkAsteroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.gdx.scenarios.ScenarioBuilder.scenario;

public final class ScenarioRegistry {

    private static final Map<String, ScenarioDefinition> SCENARIOS = new LinkedHashMap<>();

    static {
        registerEngineScenarios();
        registerInteractionScenarios();
        registerCollisionAndTargetingScenarios();
    }

    private ScenarioRegistry() {}

    public static ScenarioDefinition require(String name) {
        ScenarioDefinition definition = SCENARIOS.get(name);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown scenario '" + name + "'. Available: "
                    + String.join(", ", SCENARIOS.keySet()));
        }
        return definition;
    }

    public static Collection<ScenarioDefinition> all() {
        return Collections.unmodifiableCollection(SCENARIOS.values());
    }

    public static List<ScenarioDefinition> withTags(Collection<String> tags) {
        if (tags.isEmpty()) return new ArrayList<>(SCENARIOS.values());
        List<ScenarioDefinition> selected = new ArrayList<>();
        for (ScenarioDefinition definition : SCENARIOS.values()) {
            if (definition.tags().containsAll(tags)) selected.add(definition);
        }
        return selected;
    }

    private static void register(ScenarioDefinition definition) {
        if (SCENARIOS.putIfAbsent(definition.name(), definition) != null) {
            throw new IllegalStateException("Duplicate scenario: " + definition.name());
        }
    }

    private static void registerEngineScenarios() {
        register(engineScenario("straight", 3.2f, 3f,
                (context, elapsed, dt) -> {
                    context.setPlayerAngle(MathUtils.HALF_PI);
                    context.controls(true, false, false);
                }));
        register(engineScenario("diagonal", 3.2f, 3f,
                (context, elapsed, dt) -> {
                    context.setPlayerAngle(MathUtils.PI / 4f);
                    context.controls(true, false, false);
                }));
        register(engineScenario("side", 3.2f, 3f,
                (context, elapsed, dt) -> {
                    context.setPlayerAngle(0f);
                    context.controls(true, false, false);
                }));
        register(engineScenario("turnleft", 3.2f, 3f,
                (context, elapsed, dt) -> context.controls(true, true, false)));
        register(engineScenario("turnright", 3.2f, 3f,
                (context, elapsed, dt) -> context.controls(true, false, true)));
        register(engineScenario("coast", 3.7f, 3.5f,
                (context, elapsed, dt) -> {
                    context.setPlayerAngle(MathUtils.HALF_PI);
                    context.controls(elapsed < 1.25f, false, false);
                }));

        register(scenario("rocket")
                .tags("rocket", "visual")
                .duration(2.4f)
                .setup(context -> {
                    context.disablePlayerShield();
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.asteroid("target", x + 260f, y + 120f, Asteroid.MEDIUM);
                    context.rocket("rocket", x, y, MathUtils.HALF_PI, "target");
                })
                .tick((context, elapsed, dt) -> context.controls(false, false, false))
                .captureAt(2.2f, "main")
                .build());

        register(scenario("enemy-smoke")
                .tags("enemy-ship", "particles", "visual")
                .duration(2.8f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.enemyShip("enemy", context.player().getX() + 180f,
                            context.player().getY() + 70f);
                })
                .tick((context, elapsed, dt) -> context.controls(false, false, false))
                .captureAt(2.6f, "main")
                .build());
    }

    private static ScenarioDefinition engineScenario(String name, float duration, float capture,
                                                       ScenarioDefinition.Tick tick) {
        return scenario(name)
                .tags("player", "engine", "particles", "visual")
                .duration(duration)
                .setup(ScenarioContext::disablePlayerShield)
                .tick(tick)
                .captureAt(capture, "main")
                .build();
    }

    private static void registerInteractionScenarios() {
        register(scenario("bullet-hits-asteroid")
                .tags("bullet", "collision", "asteroid", "logic")
                .duration(0.25f)
                .setup(context -> {
                    context.disablePlayerShield();
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.asteroid("target", x + 20f, y, Asteroid.SMALL);
                    context.bullet("bullet", x + 10f, y, 0f);
                })
                .checkAt(0.2f, "asteroid destroyed", context -> context.expectHealth("target", 0))
                .checkAt(0.2f, "bullet removed", context -> context.expectManaged("bullet", false))
                .build());

        register(scenario("rocket-hits-enemy")
                .tags("rocket", "collision", "enemy-ship", "logic", "visual")
                .duration(1.5f)
                .setup(context -> {
                    context.disablePlayerShield();
                    float x = context.player().getX() + 140f;
                    float y = context.player().getY();
                    context.enemyShip("target", x, y);
                    context.rocket("rocket", x - 80f, y, 0f, "target");
                })
                .checkAt(1.3f, "enemy damaged", context -> context.expectHealth("target", 2))
                .checkAt(1.3f, "rocket removed", context -> context.expectManaged("rocket", false))
                .captureAt(0.75f, "approach")
                .build());

        register(scenario("enemy-shoots-player")
                .tags("enemy", "bullet", "collision", "player", "logic")
                .duration(0.2f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.enemyBullet("enemy-bullet", context.player().getX() - 4f,
                            context.player().getY(), 0f);
                })
                .checkAt(0.12f, "player hit", context -> context.expectPlayerHit(true))
                .checkAt(0.12f, "enemy bullet removed",
                        context -> context.expectManaged("enemy-bullet", false))
                .build());

        register(scenario("shield-blocks-collision")
                .tags("shield", "bullet", "collision", "player", "logic")
                .duration(0.2f)
                .setup(context -> context.enemyBullet("enemy-bullet",
                        context.player().getX() - 4f, context.player().getY(), 0f))
                .checkAt(0.12f, "shield blocks hit", context -> context.expectPlayerHit(false))
                .checkAt(0.12f, "blocked bullet removed",
                        context -> context.expectManaged("enemy-bullet", false))
                .build());

        register(scenario("enemy-destroyed-rewards-player")
                .tags("enemy-ship", "collision", "rewards", "logic")
                .duration(0.3f)
                .setup(context -> {
                    context.disablePlayerShield();
                    float x = context.player().getX() + 30f;
                    float y = context.player().getY();
                    EnemyShip enemy = context.enemyShip("target", x, y);
                    enemy.setHealth(1);
                    context.bullet("bullet", x - 4f, y, 0f);
                })
                .checkAt(0.2f, "enemy removed", context -> context.expectManaged("target", false))
                .checkAt(0.2f, "score awarded", context -> context.expectScoreAtLeast(1))
                .checkAt(0.2f, "experience awarded", context -> context.expectExperienceAtLeast(1))
                .build());
    }

    private static void registerCollisionAndTargetingScenarios() {
        register(scenario("player-hits-asteroid")
                .tags("player", "collision", "asteroid", "logic")
                .duration(0.15f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.asteroid("asteroid", context.player().getX(),
                            context.player().getY(), Asteroid.SMALL);
                })
                .checkAt(0.1f, "player hit by asteroid", context -> context.expectPlayerHit(true))
                .build());

        register(scenario("shield-blocks-asteroid")
                .tags("player", "shield", "collision", "asteroid", "logic")
                .duration(0.15f)
                .setup(context -> context.asteroid("asteroid", context.player().getX(),
                        context.player().getY(), Asteroid.SMALL))
                .checkAt(0.1f, "shield blocks asteroid", context -> context.expectPlayerHit(false))
                .build());

        register(scenario("player-hits-enemy-ship")
                .tags("player", "collision", "enemy-ship", "logic", "visual")
                .duration(0.15f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.enemyShip("enemy", context.player().getX() + 16f,
                            context.player().getY());
                })
                .checkAt(0.1f, "player hit by enemy ship", context -> context.expectPlayerHit(true))
                .captureAt(0.1f, "impact")
                .build());

        register(scenario("shield-blocks-enemy-ship")
                .tags("player", "shield", "collision", "enemy-ship", "logic")
                .duration(0.15f)
                .setup(context -> context.enemyShip("enemy",
                        context.player().getX() + 16f, context.player().getY()))
                .checkAt(0.1f, "shield blocks enemy ship", context -> context.expectPlayerHit(false))
                .build());

        register(scenario("nearest-target-prefers-asteroid")
                .tags("rocket", "targeting", "asteroid", "enemy-ship", "logic")
                .duration(0.1f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.asteroid("near", x + 100f, y, Asteroid.SMALL);
                    context.enemyShip("far", x + 200f, y);
                })
                .checkAt(0.05f, "nearest asteroid selected",
                        context -> context.expectNearestEnemy("near", 500f))
                .build());

        register(scenario("nearest-target-prefers-enemy-ship")
                .tags("rocket", "targeting", "asteroid", "enemy-ship", "logic")
                .duration(0.1f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("near", x + 100f, y);
                    context.orkAsteroid("far", x + 200f, y, OrkAsteroid.SMALL);
                })
                .checkAt(0.05f, "nearest enemy ship selected",
                        context -> context.expectNearestEnemy("near", 500f))
                .build());

        register(scenario("targeting-respects-detection-range")
                .tags("rocket", "targeting", "range", "logic")
                .duration(0.1f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("inside", x + AutoRocket.DETECTION_RANGE - 10f, y);
                    context.enemyShip("outside", x + AutoRocket.DETECTION_RANGE + 50f, y);
                })
                .checkAt(0.05f, "in-range target selected",
                        context -> context.expectNearestEnemy("inside", AutoRocket.DETECTION_RANGE))
                .build());
    }
}
