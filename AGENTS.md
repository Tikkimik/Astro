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
