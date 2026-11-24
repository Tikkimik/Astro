package com.gdx.scenarios;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.combat.DestructionEvent;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.OrkAsteroid;
import com.gdx.managers.GameKeys;

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
        registerSessionScenarios();
        registerInteractionScenarios();
        registerCollisionAndTargetingScenarios();
        registerApprovedCombatScenarios();
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
        register(scenario("frame-input-persists-across-fixed-ticks")
                .tags("player", "input", "fixed-timestep", "logic")
                .duration(0.15f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.setPlayerAngle(MathUtils.HALF_PI);
                    context.rememberPosition("start", "player");
                    GameKeys.setKey(GameKeys.UP, true);
                })
                .checkAt(0.1f, "frame input reaches every fixed tick",
                        context -> context.expectMovedAtLeast("player", "start", 0.01f))
                .build());

        register(scenario("player-fire-creates-projectile")
                .tags("player", "weapon", "projectile", "logic")
                .duration(0.12f)
                .setup(context -> GameKeys.setKey(GameKeys.SPACE, true))
                .checkAt(0.08f, "fire creates bounded player projectiles",
                        context -> context.expectPlayerProjectiles(4))
                .build());

        register(scenario("modal-state-preserves-gameplay-world")
                .tags("state", "modal", "upgrade", "architecture", "logic")
                .duration(0.05f)
                .checkAt(0.01f, "modal reuses gameplay world",
                        ScenarioContext::expectModalStatePreservesGameplayHandler)
                .build());

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

        register(scenario("debug-overlay")
                .tags("debug", "ui", "visual")
                .duration(1.4f)
                .setup(context -> {
                    context.disablePlayerShield();
                    context.showDebugOverlay();
                })
                .captureAt(1.2f, "main")
                .build());

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

    private static void registerSessionScenarios() {
        register(scenario("persistent-save-round-trip")
                .tags("session", "save", "persistence", "logic")
                .duration(0.05f)
                .checkAt(0.01f, "persistent save handles round trip, corruption and version",
                        ScenarioContext::expectPersistentSaveRoundTrip)
                .build());

        register(scenario("player-has-single-owner")
                .tags("player", "ownership", "logic")
                .duration(0.1f)
                .checkAt(0.05f, "player is not duplicated in object wrappers",
                        ScenarioContext::expectPlayerHasSingleOwner)
                .build());

        register(scenario("clear-returns-auto-rocket-to-pool")
                .tags("rocket", "ownership", "pool", "logic")
                .duration(0.1f)
                .checkAt(0.05f, "clear releases rocket without resetting external particle budget",
                        ScenarioContext::expectClearReturnsAutoRocketToPool)
                .build());

        register(scenario("session-snapshot-round-trip")
                .tags("session", "save", "logic")
                .duration(0.1f)
                .setup(context -> {
                    context.rememberSession("before-mutation");
                    context.addScore(15);
                    context.advanceSessionTime(5f);
                    context.asteroid("added-after-snapshot", 300f, 300f, Asteroid.SMALL);
                    context.rocket("added-rocket", 250f, 300f, 0f, "added-after-snapshot");
                    context.rememberAutoRocketPoolFreeCount();
                    context.rememberParticleBudgetActive();
                    context.addRocketTrailParticle();
                    context.restoreSession("before-mutation");
                    context.expectSessionElapsed(0f);
                    context.expectParticleBudgetRestored();
                })
                .checkAt(0.05f, "score and experience restored", context -> {
                    context.expectScore(0);
                    context.expectExperience(0);
                })
                .checkAt(0.05f, "objects added after snapshot removed",
                        context -> context.expectManaged("added-after-snapshot", false))
                .checkAt(0.05f, "pooled additions released on restore", context -> {
                    context.expectManaged("added-rocket", false);
                    context.expectRocketHasNoTarget("added-rocket");
                    context.expectAutoRocketsReturnedToPool(1);
                })
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
                .checkAt(0.2f, "small asteroid reward", context -> context.expectScore(5))
                .checkAt(0.2f, "small asteroid experience", context -> context.expectExperience(5))
                .checkAt(0.2f, "direct projectile destruction metadata",
                        context -> context.expectLastDestruction(
                                DestructionEvent.Cause.DIRECT_PROJECTILE, "bullet", "target"))
                .build());

        register(scenario("asteroid-particles-return-to-pool")
                .tags("asteroid", "particles", "pool", "visual")
                .duration(1.3f)
                .setup(context -> {
                    context.rememberParticlePoolFreeCount();
                    float x = context.player().getX() + 20f;
                    float y = context.player().getY();
                    context.asteroid("target", x, y, Asteroid.SMALL);
                    context.bullet("bullet", x - 10f, y, 0f);
                })
                .checkAt(1.2f, "expired asteroid particles return to pool",
                        context -> context.expectParticlesReturnedToPool(6))
                .captureAt(0.15f, "burst")
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
                .checkAt(1.3f, "enemy damaged", context -> context.expectHealth("target", 1))
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
                .checkAt(0.2f, "score awarded", context -> context.expectScore(25))
                .checkAt(0.2f, "experience awarded", context -> context.expectExperience(25))
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
                .checkAt(0.1f, "player collision destruction metadata",
                        context -> context.expectLastDestruction(
                                DestructionEvent.Cause.PLAYER_COLLISION, "player", "asteroid"))
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
                        context -> context.expectNearestTarget("near", 500f))
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
                        context -> context.expectNearestTarget("near", 500f))
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
                        context -> context.expectNearestTarget("inside", AutoRocket.DETECTION_RANGE))
                .build());
    }

    private static void registerApprovedCombatScenarios() {
        register(scenario("bullet-damages-enemy-ship")
                .tags("bullet", "enemy-ship", "damage", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 20f;
                    float y = context.player().getY();
                    context.enemyShip("target", x, y);
                    context.bullet("bullet", x - 10f, y, 0f);
                })
                .checkAt(0.15f, "bullet deals one damage", context -> context.expectHealth("target", 2))
                .checkAt(0.15f, "hit gives no score", context -> context.expectScore(0))
                .build());

        register(scenario("bullet-damages-ork-asteroid")
                .tags("bullet", "ork-asteroid", "damage", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 20f;
                    float y = context.player().getY();
                    context.orkAsteroid("target", x, y, OrkAsteroid.SMALL);
                    context.bullet("bullet", x - 10f, y, 0f);
                })
                .checkAt(0.15f, "ork asteroid has one health", context -> context.expectHealth("target", 1))
                .checkAt(0.15f, "damage gives no score", context -> context.expectScore(0))
                .build());

        register(scenario("bullet-destroys-ork-asteroid")
                .tags("bullet", "ork-asteroid", "rewards", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 20f;
                    float y = context.player().getY();
                    context.orkAsteroid("target", x, y, OrkAsteroid.SMALL);
                    context.bullet("first", x - 10f, y, 0f);
                    context.bullet("second", x - 10f, y, 0f);
                })
                .checkAt(0.15f, "ork asteroid removed", context -> context.expectManaged("target", false))
                .checkAt(0.15f, "ork asteroid reward", context -> context.expectScore(10))
                .checkAt(0.15f, "ork asteroid experience", context -> context.expectExperience(10))
                .build());

        register(scenario("medium-asteroid-splits-without-reward")
                .tags("bullet", "asteroid", "rewards", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 25f;
                    float y = context.player().getY();
                    context.asteroid("target", x, y, Asteroid.MEDIUM);
                    context.bullet("bullet", x - 10f, y, 0f);
                })
                .checkAt(0.15f, "medium asteroid removed", context -> context.expectManaged("target", false))
                .checkAt(0.15f, "split gives no score", context -> context.expectScore(0))
                .build());

        register(scenario("rocket-destroys-ork-asteroid")
                .tags("rocket", "ork-asteroid", "damage", "rewards", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 30f;
                    float y = context.player().getY();
                    context.orkAsteroid("target", x, y, OrkAsteroid.SMALL);
                    context.rocket("rocket", x - 10f, y, 0f, "target");
                })
                .checkAt(0.15f, "rocket destroys two-health target",
                        context -> context.expectManaged("target", false))
                .checkAt(0.15f, "ork rocket reward", context -> context.expectScore(10))
                .checkAt(0.15f, "ork rocket experience", context -> context.expectExperience(10))
                .build());

        register(scenario("enemy-bullet-stops-on-other-enemy")
                .tags("enemy", "bullet", "friendly-fire", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 100f;
                    float y = context.player().getY();
                    context.enemyShip("target", x, y);
                    context.enemyBullet("bullet", x - 4f, y, 0f);
                })
                .checkAt(0.15f, "enemy bullet removed", context -> context.expectManaged("bullet", false))
                .checkAt(0.15f, "friendly fire deals no damage", context -> context.expectHealth("target", 3))
                .build());

        register(scenario("enemy-bullet-ignores-owner")
                .tags("enemy", "bullet", "friendly-fire", "logic")
                .duration(0.15f)
                .setup(context -> {
                    float x = context.player().getX() + 100f;
                    float y = context.player().getY();
                    context.enemyShip("owner", x, y);
                    context.enemyBullet("bullet", x, y, 0f, "owner");
                })
                .checkAt(0.1f, "owner bullet remains active", context -> context.expectManaged("bullet", true))
                .checkAt(0.1f, "owner takes no damage", context -> context.expectHealth("owner", 3))
                .build());

        register(scenario("enemy-bullet-survives-owner-destruction")
                .tags("enemy", "bullet", "lifetime", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 220f;
                    float y = context.player().getY();
                    context.enemyShip("owner", x, y);
                    context.enemyBullet("bullet", x + 30f, y, 0f, "owner");
                    context.destroyEnemy("owner");
                })
                .checkAt(0.15f, "owner removed", context -> context.expectManaged("owner", false))
                .checkAt(0.15f, "fired bullet remains active",
                        context -> context.expectManaged("bullet", true))
                .build());

        register(scenario("rocket-retargets-nearest-enemy")
                .tags("rocket", "targeting", "retargeting", "logic")
                .duration(0.9f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("lost", x + 100f, y);
                    context.enemyShip("replacement", x + 240f, y + 40f);
                    context.rocket("rocket", x, y, 0f, "lost");
                    context.destroyEnemy("lost");
                })
                .checkAt(0.7f, "rocket acquires replacement",
                        context -> context.expectRocketTarget("rocket", "replacement"))
                .build());

        register(scenario("rocket-expires-without-target")
                .tags("rocket", "targeting", "timeout", "logic")
                .duration(3.8f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("lost", x + 100f, y);
                    context.rocket("rocket", x, y, 0f, "lost");
                    context.destroyEnemy("lost");
                })
                .checkAt(3.65f, "untargeted rocket removed",
                        context -> context.expectManaged("rocket", false))
                .build());

        register(scenario("rocket-explosion-damages-nearby-enemy")
                .tags("rocket", "explosion", "area-damage", "enemy-ship", "logic", "visual")
                .duration(0.25f)
                .setup(context -> {
                    float x = context.player().getX() + 100f;
                    float y = context.player().getY();
                    context.enemyShip("direct", x, y);
                    context.enemyShip("near", x + 35f, y + 20f);
                    context.enemyShip("far", x + 85f, y);
                    context.rocket("rocket", x - 10f, y, 0f, "direct");
                })
                .checkAt(0.12f, "direct target takes two damage",
                        context -> context.expectHealth("direct", 1))
                .checkAt(0.12f, "near target takes splash damage",
                        context -> context.expectHealth("near", 2))
                .checkAt(0.12f, "far target is unharmed",
                        context -> context.expectHealth("far", 3))
                .captureAt(0.08f, "blast")
                .build());

        register(scenario("rocket-splash-destroys-nearby-enemy")
                .tags("rocket", "explosion", "area-damage", "rewards", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 100f;
                    float y = context.player().getY();
                    context.enemyShip("direct", x, y);
                    OrkAsteroid nearby = context.orkAsteroid(
                            "near", x + 35f, y + 20f, OrkAsteroid.SMALL);
                    nearby.setHealth(1);
                    context.rocket("rocket", x - 10f, y, 0f, "direct");
                })
                .checkAt(0.12f, "splash target destroyed",
                        context -> context.expectManaged("near", false))
                .checkAt(0.12f, "splash destruction rewarded",
                        context -> context.expectScore(10))
                .checkAt(0.12f, "splash destruction metadata",
                        context -> context.expectLastDestruction(
                                DestructionEvent.Cause.ROCKET_SPLASH, "rocket", "near"))
                .build());

        register(scenario("enemy-ship-requires-three-bullets")
                .tags("bullet", "enemy-ship", "damage", "rewards", "logic")
                .duration(0.4f)
                .setup(context -> {
                    context.rememberRegisteredParticleCollections();
                    float x = context.player().getX() + 120f;
                    float y = context.player().getY();
                    context.enemyShip("target", x, y);
                    context.bullet("first", x - 4f, y, 0f);
                })
                .tick((context, elapsed, dt) -> {
                    context.runOnce("second-shot", elapsed, 0.1f, () -> {
                        EnemyShip target = context.get("target", EnemyShip.class);
                        context.bullet("second", target.getX() - 4f, target.getY(), 0f);
                    });
                    context.runOnce("third-shot", elapsed, 0.2f, () -> {
                        EnemyShip target = context.get("target", EnemyShip.class);
                        context.bullet("third", target.getX() - 4f, target.getY(), 0f);
                    });
                })
                .checkAt(0.08f, "first bullet leaves two health",
                        context -> context.expectHealth("target", 2))
                .checkAt(0.18f, "second bullet leaves one health",
                        context -> context.expectHealth("target", 1))
                .checkAt(0.18f, "two hits give no score", context -> context.expectScore(0))
                .checkAt(0.3f, "third bullet destroys ship",
                        context -> context.expectManaged("target", false))
                .checkAt(0.3f, "ship destruction gives score", context -> context.expectScore(25))
                .checkAt(0.3f, "ship destruction gives experience",
                        context -> context.expectExperience(25))
                .checkAt(0.3f, "ship particle collections unregistered",
                        ScenarioContext::expectRegisteredParticleCollectionsRestored)
                .build());

        register(scenario("large-asteroid-chain-rewards-twenty")
                .tags("bullet", "asteroid", "splitting", "rewards", "logic")
                .duration(0.55f)
                .setup(context -> {
                    float x = context.player().getX() + 180f;
                    float y = context.player().getY();
                    context.asteroid("large", x, y, Asteroid.LARGE);
                    context.bullet("first", x - 4f, y, 0f);
                })
                .tick((context, elapsed, dt) -> {
                    context.runOnce("destroy-medium", elapsed, 0.12f,
                            () -> context.shootAllAsteroidsOfType(Asteroid.MEDIUM));
                    context.runOnce("destroy-small", elapsed, 0.28f,
                            () -> context.shootAllAsteroidsOfType(Asteroid.SMALL));
                })
                .checkAt(0.1f, "large splits into two medium",
                        context -> context.expectAsteroidCount(Asteroid.MEDIUM, 2))
                .checkAt(0.1f, "large stage gives no score", context -> context.expectScore(0))
                .checkAt(0.25f, "medium split into four small",
                        context -> context.expectAsteroidCount(Asteroid.SMALL, 4))
                .checkAt(0.25f, "medium stages give no score", context -> context.expectScore(0))
                .checkAt(0.45f, "all small asteroids destroyed",
                        context -> context.expectManagedCount(Asteroid.class, 0))
                .checkAt(0.45f, "chain gives twenty score", context -> context.expectScore(20))
                .checkAt(0.45f, "chain gives twenty experience",
                        context -> context.expectExperience(20))
                .build());

        register(scenario("rocket-explosion-does-not-hurt-player")
                .tags("rocket", "explosion", "player", "area-damage", "logic")
                .duration(0.2f)
                .setup(context -> {
                    context.disablePlayerShield();
                    float x = context.player().getX() + 45f;
                    float y = context.player().getY();
                    context.enemyShip("target", x, y);
                    context.rocket("rocket", x - 10f, y, 0f, "target");
                })
                .checkAt(0.15f, "rocket explodes", context -> context.expectManaged("rocket", false))
                .checkAt(0.15f, "player ignores own explosion",
                        context -> context.expectPlayerHit(false))
                .build());

        register(scenario("ork-asteroid-does-not-split")
                .tags("bullet", "ork-asteroid", "splitting", "logic")
                .duration(0.2f)
                .setup(context -> {
                    float x = context.player().getX() + 100f;
                    float y = context.player().getY();
                    context.orkAsteroid("target", x, y, OrkAsteroid.LARGE);
                    context.bullet("first", x - 4f, y, 0f);
                    context.bullet("second", x - 4f, y, 0f);
                })
                .checkAt(0.15f, "ork asteroid destroyed",
                        context -> context.expectManaged("target", false))
                .checkAt(0.15f, "no ork fragments created",
                        context -> context.expectManagedCount(OrkAsteroid.class, 0))
                .build());

        register(scenario("rockets-may-share-target")
                .tags("rocket", "targeting", "multiple-projectiles", "logic")
                .duration(0.75f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("target", x + 600f, y);
                    context.rocket("first", x, y - 10f, 0f, "target");
                    context.rocket("second", x, y + 10f, 0f, "target");
                })
                .checkAt(0.65f, "first rocket keeps shared target",
                        context -> context.expectRocketTarget("first", "target"))
                .checkAt(0.65f, "second rocket keeps shared target",
                        context -> context.expectRocketTarget("second", "target"))
                .build());

        register(scenario("rocket-keeps-flying-after-target-loss")
                .tags("rocket", "targeting", "movement", "logic")
                .duration(0.9f)
                .setup(context -> {
                    float x = context.player().getX();
                    float y = context.player().getY();
                    context.enemyShip("lost", x + 100f, y);
                    context.rocket("rocket", x, y, 0f, "lost");
                    context.rememberPosition("launch", "rocket");
                    context.destroyEnemy("lost");
                })
                .checkAt(0.8f, "rocket has no replacement target",
                        context -> context.expectRocketHasNoTarget("rocket"))
                .checkAt(0.8f, "untargeted rocket remains active",
                        context -> context.expectManaged("rocket", true))
                .checkAt(0.8f, "untargeted rocket keeps moving",
                        context -> context.expectMovedAtLeast("rocket", "launch", 20f))
                .build());
    }
}
