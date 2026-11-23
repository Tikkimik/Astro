package com.gdx.presentation;

public record GameDebugView(
        Performance performance,
        PlayerState player,
        CameraState camera,
        WorldState world,
        CullingState culling,
        ProgressionState progression,
        SystemState system
) {
    public record Performance(
            int renderFps,
            int simulationUps,
            int ticksPerFrame,
            String renderCap,
            int targetLogicFps,
            float fixedDeltaMillis,
            long simulationTickNanos,
            long collisionNanos,
            long cameraNanos
    ) {}

    public record PlayerState(float x, float y, float dx, float dy, float radians) {}

    public record CameraState(float zoom, float x, float y) {}

    public record WorldState(
            int activeObjects,
            int createdObjects,
            int projectiles,
            int obstacles,
            int enemies,
            int sectors,
            int gridCells
    ) {}

    public record Culling(int active, int visible, int drawn, int culled) {}

    public record CullingState(
            Culling objects,
            Culling projectiles,
            Culling obstacles,
            Culling enemies,
            Culling particles
    ) {}

    public record ProgressionState(int level, int experience, int nextLevelExperience,
                                   int score, int highScore) {}

    public record SystemState(long usedMemoryMb, long maxMemoryMb,
                              int viewportWidth, int viewportHeight, String glVersion) {}
}
