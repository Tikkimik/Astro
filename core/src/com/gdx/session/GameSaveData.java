package com.gdx.session;

import java.util.ArrayList;
import java.util.List;

public final class GameSaveData {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public long savedAtEpochMillis;
    public SessionData session = new SessionData();
    public PlayerData player = new PlayerData();
    public ProgressionData progression = new ProgressionData();

    public static final class SessionData {
        public int level;
        public int totalAsteroids;
        public int numAsteroidsLeft;
        public int score;
        public int highScore;
        public float autoRocketTimer;
        public float elapsedTime;
    }

    public static final class PlayerData {
        public float x;
        public float y;
        public float dx;
        public float dy;
        public float radians;
    }

    public static final class ProgressionData {
        public int currentLevel;
        public int currentExperience;
        public int experienceToNextLevel;
        public float playerSpeed;
        public float bulletSpeed;
        public float bulletDamage;
        public float fireRate;
        public int maxHealth;
        public float healthRegen;
        public float criticalChance;
        public float criticalDamage;
        public float autoRocketCooldown;
        public float autoRocketDamage;
        public List<String> selectedUpgradeTypes = new ArrayList<>();
    }
}
