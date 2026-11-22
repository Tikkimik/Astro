# Astro — AGENTS.md

## Project

A LibGDX space game. Multi-module Gradle project (Java 21, Gradle 8.13, LibGDX 1.12.1).

## Modules & entrypoints

| Module | Entrypoint | Run command |
|--------|-----------|-------------|
| `desktop` | `com.mygdx.game.DesktopLauncher` | `./gradlew desktop:run` |
| `android` | `com.mygdx.game.AndroidLauncher` | `./gradlew :android:assembleDebug` |
| `ios` | `com.mygdx.game.IOSLauncher` | (RoboVM, macOS only) |
| `html` (GWT) | `com.mygdx.game.client.HtmlLauncher` | `./gradlew html:superDev` |
| `core` | `com.gdx.game.MyGdxGame` | (library, no direct run) |

Game logic lives under `core/src/com/gdx/`; platform launchers under `com.mygdx.game/`.

## Build quirks

- **Java 21 required** for core/desktop, but **Android toolchain needs Java 17**. Use `JAVA_HOME=... ./gradlew :android:assembleDebug` or the provided `build-android-java17.sh` script.
- **macOS desktop**: requires `-XstartOnFirstThread` JVM flag (already handled in `desktop/build.gradle`).
- **GWT/html**: GWT compiler needs 1G+ heap; run with `./gradlew html:superDev` (serves at `localhost:8080`).
- Core sets `sourceCompatibility = 21` and `targetCompatibility = 21`.
- No CI, no linter, no formatter, no tests defined.

## Architecture

- **Game loop**: fixed timestep via `TimeManager` (core loop: `accumulate → shouldUpdate → update(fixedDt) → render`). Logic runs at configurable FPS (default 60), render runs per frame.
- **State machine**: `ImprovedGameStateManager` with handlers per state (`MenuStateHandler`, `RealPlayStateHandler`, `PausedStateHandler`, `SettingsStateHandler`, `UpgradeSelectionStateHandler`).
- **Config**: `GameConfig` holds all settings; `GameSettings` provides runtime overrides.
- **Object pooling**: `ParticlePool` for particles; `ObjectPools` for other entities.
- **Particle system**: separate entity classes per effect (`FlameParticle`, `RocketParticle`, `ShieldParticle`, `ExplosionParticle`).

## Conventions

- Code comments and commit messages are in **Russian**.
- Package base: `com.gdx` for core game, `com.mygdx.game` for platform launchers.
- Dependencies: `core` → LibGDX only; platform modules depend on `core`.
- Assets directory is `assets/` (shared by desktop, android, html).
- Desktop config sets `vsync(false)` and `foregroundFPS(0)` for max performance.

## Verification of changes

After implementing a change, verify the result in proportion to the type and risk of the change before considering the task complete.

- After changing Java code, run at least `./gradlew core:compileJava` or a more specific relevant build/test command.
- Changes that affect graphics, UI, camera behavior, animation, particles, shaders, object placement, or any other visible runtime behavior require visual verification.
- For visual verification, choose the closest existing scenario and run `./gradlew desktop:run -PvisualTest=<scenario>`. Existing scenarios include `straight`, `diagonal`, `side`, `turnleft`, `turnright`, and `coast`.
- Capture a screenshot at a representative moment, open the resulting image, and inspect the actual rendered result. Merely launching the game or creating a screenshot is not sufficient verification.
- If visual inspection reveals a defect, fix it and repeat the scenario and screenshot inspection before finishing.
- Changes to gameplay mechanics require an automated scenario and assertions or a state/metrics dump when practical; screenshots alone are not sufficient for non-visual behavior.
- Documentation-only changes, comments, and internal refactors that provably do not affect runtime behavior do not require a visual run.
- Choose and run routine verification autonomously without asking the user for permission. Ask only when the expected visual/design result is subjective or when a suitable scenario cannot be determined safely.
- If no suitable scenario exists, add the smallest practical verification scenario or explicitly report why verification could not be completed.
- In the final response, list the checks performed and provide the screenshot path for visual changes.
- Never update a visual baseline merely to make a comparison pass. Baseline changes require a justified intended visual change and user confirmation.
