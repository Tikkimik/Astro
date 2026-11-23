package com.gdx.presentation;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

public final class GameDebugOverlay {

    private final BitmapFont font;

    public GameDebugOverlay(BitmapFont font) {
        this.font = font;
    }

    public void draw(SpriteBatch spriteBatch, GameDebugView view, float startY) {
        List<DebugEntry> lines = buildEntries(view);
        font.getData().setScale(0.8f);
        float lineHeight = font.getLineHeight() * font.getData().scaleY;

        spriteBatch.begin();
        float y = startY;
        for (int i = 0; i < lines.size(); i++) {
            DebugEntry entry = lines.get(i);
            font.setColor(entry.red(), entry.green(), entry.blue(), 1f);
            font.draw(spriteBatch, entry.text(), 10f, y);
            y -= lineHeight + 3f + (entry.header() && i > 0 ? 7f : 0f);
        }
        spriteBatch.end();

        font.getData().setScale(1f);
    }

    private List<DebugEntry> buildEntries(GameDebugView view) {
        List<DebugEntry> entries = new ArrayList<>();
        GameDebugView.Performance performance = view.performance();
        entries.add(header("--- Debug menu (F чтобы скрыть) ---", 1f, 1f, 0f));
        entries.add(line("Render FPS: " + performance.renderFps() + " | Cap: " + performance.renderCap()));
        entries.add(line(String.format("Simulation UPS: %d | Target: %d Hz | Fixed dt: %.2f ms",
                performance.simulationUps(), performance.targetLogicFps(), performance.fixedDeltaMillis())));
        entries.add(line("Ticks/frame: " + performance.ticksPerFrame()));
        entries.add(line("Render cap controls: ` — increase, 1 — decrease"));
        entries.add(line("Simulation tick: " + performance.simulationTickNanos() / 1000 + " us"));
        entries.add(line("Collisions: " + performance.collisionNanos() / 1000
                + " us | Camera: " + performance.cameraNanos() / 1000 + " us"));

        GameDebugView.PlayerState player = view.player();
        float speed = (float) Math.sqrt(player.dx() * player.dx() + player.dy() * player.dy());
        entries.add(header("--- Player ---", 0f, 1f, 1f));
        entries.add(line(String.format("Pos: x=%.1f y=%.1f", player.x(), player.y())));
        entries.add(line(String.format("Vel: dx=%.1f dy=%.1f  speed=%.1f", player.dx(), player.dy(), speed)));
        float degrees = player.radians() * 180f / (float) Math.PI;
        float normalized = ((degrees % 360f) + 360f) % 360f;
        String[] directions = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
        String facing = directions[(((int) normalized + 22) / 45) % directions.length];
        entries.add(line(String.format("Facing: %.1f deg (%s)", normalized, facing)));
        GameDebugView.CameraState camera = view.camera();
        entries.add(line("Zoom: " + camera.zoom() + "  (camera: "
                + (int) camera.x() + "," + (int) camera.y() + ")"));

        GameDebugView.WorldState world = view.world();
        entries.add(header("--- World / Objects ---", 0f, 1f, 0f));
        entries.add(line("Active objects: " + world.activeObjects()
                + " / Created total: " + world.createdObjects()));
        entries.add(line("Projectiles: " + world.projectiles()));
        entries.add(line("Obstacles: " + world.obstacles()));
        entries.add(line("Enemies: " + world.enemies()));
        entries.add(line("Sectors: " + world.sectors()));
        entries.add(line("Grid cells: " + world.gridCells()));

        GameDebugView.CullingState culling = view.culling();
        entries.add(header("--- Rendering / Culling ---", 1f, 1f, 0.5f));
        entries.add(line(cullingLine("Objects", culling.objects())));
        entries.add(line(cullingLine("Projectiles", culling.projectiles())));
        entries.add(line(cullingLine("Obstacles", culling.obstacles())));
        entries.add(line(cullingLine("Enemies", culling.enemies())));
        entries.add(line(cullingLine("Particles", culling.particles())));

        GameDebugView.ProgressionState progression = view.progression();
        entries.add(header("--- Progression ---", 1f, 0.5f, 0f));
        entries.add(line("Level: " + progression.level()));
        entries.add(line("XP: " + progression.experience() + "/" + progression.nextLevelExperience()));
        entries.add(line("Score: " + progression.score() + "   High: " + progression.highScore()));

        GameDebugView.SystemState system = view.system();
        entries.add(header("--- System ---", 0f, 1f, 0.5f));
        entries.add(line("Mem: " + system.usedMemoryMb() + " MB / " + system.maxMemoryMb() + " MB"));
        entries.add(line("HUD: " + system.viewportWidth() + "x" + system.viewportHeight()));
        entries.add(line("GL: " + system.glVersion()));
        return entries;
    }

    private String cullingLine(String name, GameDebugView.Culling values) {
        return name + ": active " + values.active() + " | visible " + values.visible()
                + " | drawn " + values.drawn() + " | culled " + values.culled();
    }

    private DebugEntry line(String text) {
        return new DebugEntry(text, 1f, 1f, 1f, false);
    }

    private DebugEntry header(String text, float red, float green, float blue) {
        return new DebugEntry(text, red, green, blue, true);
    }

    private record DebugEntry(String text, float red, float green, float blue, boolean header) {}
}
