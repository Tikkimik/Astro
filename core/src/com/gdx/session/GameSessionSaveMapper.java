package com.gdx.session;

import com.gdx.entities.Player;
import com.gdx.managers.ProgressionManager;

public final class GameSessionSaveMapper {

    public GameSaveData capture(GameSession session) {
        GameSaveData data = new GameSaveData();
        data.savedAtEpochMillis = System.currentTimeMillis();

        data.session.level = session.getLevel();
        data.session.totalAsteroids = session.getTotalAsteroids();
        data.session.numAsteroidsLeft = session.getNumAsteroidsLeft();
        data.session.score = session.getScore();
        data.session.highScore = session.getHighScore();
        data.session.autoRocketTimer = session.getAutoRocketTimer();
        data.session.elapsedTime = session.getElapsedTime();

        Player player = session.getPlayer();
        data.player.x = player.getX();
        data.player.y = player.getY();
        data.player.dx = player.getDx();
        data.player.dy = player.getDy();
        data.player.radians = player.getRadians();

        ProgressionManager progression = session.getProgression();
        data.progression.currentLevel = progression.getCurrentLevel();
        data.progression.currentExperience = progression.getCurrentExperience();
        data.progression.experienceToNextLevel = progression.getExperienceToNextLevel();
        data.progression.playerSpeed = progression.getPlayerSpeed();
        data.progression.bulletSpeed = progression.getBulletSpeed();
        data.progression.bulletDamage = progression.getBulletDamage();
        data.progression.fireRate = progression.getFireRate();
        data.progression.maxHealth = progression.getMaxHealth();
        data.progression.healthRegen = progression.getHealthRegen();
        data.progression.criticalChance = progression.getCriticalChance();
        data.progression.criticalDamage = progression.getCriticalDamage();
        data.progression.autoRocketCooldown = progression.getAutoRocketCooldown();
        data.progression.autoRocketDamage = progression.getAutoRocketDamage();
        data.progression.pendingUpgradeChoices = progression.getPendingUpgradeChoices();
        data.progression.selectedUpgradeTypes.addAll(progression.getSelectedUpgradeTypes());
        return data;
    }

    public void restore(GameSession session, GameSaveData data) {
        validate(data);
        session.applyMappedState(data.session.level, data.session.totalAsteroids,
                data.session.numAsteroidsLeft, data.session.score, data.session.highScore,
                data.session.autoRocketTimer, data.session.elapsedTime);
        session.getProgression().restorePersistentState(data.progression);
        session.getPlayer().restorePersistentState(data.player.x, data.player.y,
                data.player.dx, data.player.dy, data.player.radians);
        session.getWorld().updateReferences(session.getObjects(), session.getPlayer());
    }

    public void validate(GameSaveData data) {
        if (data == null || data.version != GameSaveData.CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported save version");
        }
        if (data.session == null || data.player == null || data.progression == null) {
            throw new IllegalArgumentException("Incomplete save data");
        }
        if (data.session.level < 1 || data.session.score < 0 || data.session.highScore < 0
                || data.progression.currentLevel < 1
                || data.progression.experienceToNextLevel < 1
                || data.progression.currentExperience < 0
                || data.progression.pendingUpgradeChoices < 0
                || !Float.isFinite(data.player.x) || !Float.isFinite(data.player.y)
                || !Float.isFinite(data.player.dx) || !Float.isFinite(data.player.dy)
                || !Float.isFinite(data.player.radians)
                || !Float.isFinite(data.progression.playerSpeed)
                || !Float.isFinite(data.progression.bulletSpeed)
                || !Float.isFinite(data.progression.bulletDamage)
                || !Float.isFinite(data.progression.fireRate)
                || data.progression.playerSpeed <= 0f
                || data.progression.bulletSpeed <= 0f
                || data.progression.bulletDamage <= 0f
                || data.progression.fireRate <= 0f) {
            throw new IllegalArgumentException("Invalid save values");
        }
        if (data.progression.selectedUpgradeTypes == null) {
            data.progression.selectedUpgradeTypes = new java.util.ArrayList<>();
        }
    }
}
