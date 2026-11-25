package com.gdx.managers;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.session.GameSaveData;
import com.gdx.utils.GameSettings;

import java.util.ArrayList;
import java.util.List;

public class ProgressionManager {

    public record Snapshot(
            int currentLevel,
            int currentExperience,
            int experienceToNextLevel,
            float playerSpeed,
            float bulletSpeed,
            float bulletDamage,
            float fireRate,
            int maxHealth,
            float healthRegen,
            float criticalChance,
            float criticalDamage,
            float autoRocketCooldown,
            float autoRocketDamage,
            int pendingUpgradeChoices,
            List<Upgrade> selectedUpgrades
    ) {}
    
    // Система уровней
    private int currentLevel = 1;
    private int currentExperience = 0;
    private int experienceToNextLevel = 100; // Базовый опыт для следующего уровня
    
    // Статистики игрока
    private float playerSpeed;
    private float bulletSpeed;
    private float bulletDamage = 1f;
    private float fireRate = 1f;
    private int maxHealth = 3;
    private float healthRegen = 0f;
    private float criticalChance = 0.05f;
    private float criticalDamage = 2f;
    private float autoRocketCooldown = 1f;
    private float autoRocketDamage = 1f;
    private int pendingUpgradeChoices;
    private float configuredPlayerSpeed;
    private float configuredBulletSpeed;
    
    // Доступные улучшения
    private List<Upgrade> availableUpgrades;
    private List<Upgrade> selectedUpgrades;
    
    public ProgressionManager() {
        configuredPlayerSpeed = GameSettings.getPlayerSpeed();
        configuredBulletSpeed = GameSettings.getBulletSpeed();
        playerSpeed = configuredPlayerSpeed;
        bulletSpeed = configuredBulletSpeed;
        availableUpgrades = new ArrayList<>();
        selectedUpgrades = new ArrayList<>();
        initializeUpgrades();
    }
    
    private void initializeUpgrades() {
        // Скорость игрока
        availableUpgrades.add(new Upgrade("SPEED", "Форсаж", "Скорость корабля +20%"));
        
        // Скорость пуль
        availableUpgrades.add(new Upgrade("BULLET_SPEED", "Ускоритель", "Скорость пуль +25%"));
        
        // Урон пуль
        availableUpgrades.add(new Upgrade("BULLET_DAMAGE", "Тяжёлый заряд", "Урон пуль +30%"));
        
        // Скорость стрельбы
        availableUpgrades.add(new Upgrade("FIRE_RATE", "Быстрая очередь", "Скорострельность +20%"));
        
        // Автоматические ракеты
        availableUpgrades.add(new Upgrade("ROCKET_COOLDOWN", "Ракетный конвейер", "Перезарядка ракет −15%"));
        
        // Урон ракет
        availableUpgrades.add(new Upgrade("ROCKET_DAMAGE", "Боеголовка", "Урон ракет +40%"));
    }
    
    public void addExperience(int amount) {
        currentExperience += amount;
        
        // Проверяем, достигли ли следующего уровня
        while (currentExperience >= experienceToNextLevel) {
            levelUp();
        }
    }
    
    private void levelUp() {
        currentExperience -= experienceToNextLevel;
        currentLevel++;
        pendingUpgradeChoices++;
        
        // Увеличиваем опыт для следующего уровня (как в Magic Survival)
        experienceToNextLevel = (int)(experienceToNextLevel * 1.15f);
        
    }
    
    public List<Upgrade> createUpgradeOffer(int maximum) {
        List<Upgrade> candidates = new ArrayList<>(availableUpgrades);
        List<Upgrade> offer = new ArrayList<>(Math.min(maximum, candidates.size()));
        while (offer.size() < maximum && !candidates.isEmpty()) {
            offer.add(candidates.remove(MathUtils.random(candidates.size() - 1)));
        }
        return List.copyOf(offer);
    }

    public boolean selectUpgrade(String type) {
        if (pendingUpgradeChoices <= 0) return false;
        Upgrade upgrade = findUpgrade(type);
        if (upgrade == null) return false;

        apply(upgrade.getType());
        selectedUpgrades.add(upgrade);
        pendingUpgradeChoices--;
        return true;
    }

    private void apply(String type) {
        switch (type) {
            case "SPEED" -> playerSpeed *= 1.2f;
            case "BULLET_SPEED" -> bulletSpeed *= 1.25f;
            case "BULLET_DAMAGE" -> bulletDamage *= 1.3f;
            case "FIRE_RATE" -> fireRate *= 1.2f;
            case "ROCKET_COOLDOWN" -> autoRocketCooldown *= 0.85f;
            case "ROCKET_DAMAGE" -> autoRocketDamage *= 1.4f;
            default -> throw new IllegalArgumentException("Неизвестное улучшение: " + type);
        }
    }

    public void syncConfiguredBaseStats() {
        float newPlayerSpeed = GameSettings.getPlayerSpeed();
        if (newPlayerSpeed != configuredPlayerSpeed) {
            playerSpeed *= newPlayerSpeed / configuredPlayerSpeed;
            configuredPlayerSpeed = newPlayerSpeed;
        }
        float newBulletSpeed = GameSettings.getBulletSpeed();
        if (newBulletSpeed != configuredBulletSpeed) {
            bulletSpeed *= newBulletSpeed / configuredBulletSpeed;
            configuredBulletSpeed = newBulletSpeed;
        }
    }
    
    // Геттеры для статистик
    public float getPlayerSpeed() { return playerSpeed; }
    public float getBulletSpeed() { return bulletSpeed; }
    public float getBulletDamage() { return bulletDamage; }
    public float getFireRate() { return fireRate; }
    public int getMaxHealth() { return maxHealth; }
    public float getHealthRegen() { return healthRegen; }
    public float getCriticalChance() { return criticalChance; }
    public float getCriticalDamage() { return criticalDamage; }
    public float getAutoRocketCooldown() { return autoRocketCooldown; }
    public float getAutoRocketDamage() { return autoRocketDamage; }
    public int getPendingUpgradeChoices() { return pendingUpgradeChoices; }
    public boolean hasPendingUpgradeChoice() { return pendingUpgradeChoices > 0; }
    
    // Геттеры для системы уровней
    public int getCurrentLevel() { return currentLevel; }
    public int getCurrentExperience() { return currentExperience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public float getExperienceProgress() { 
        return (float)currentExperience / experienceToNextLevel; 
    }
    
    public List<Upgrade> getAvailableUpgrades() { return availableUpgrades; }
    public List<Upgrade> getSelectedUpgrades() { return selectedUpgrades; }

    public List<String> getSelectedUpgradeTypes() {
        List<String> types = new ArrayList<>(selectedUpgrades.size());
        for (Upgrade upgrade : selectedUpgrades) types.add(upgrade.getType());
        return types;
    }

    public void restorePersistentState(GameSaveData.ProgressionData data) {
        currentLevel = data.currentLevel;
        currentExperience = data.currentExperience;
        experienceToNextLevel = data.experienceToNextLevel;
        playerSpeed = data.playerSpeed;
        bulletSpeed = data.bulletSpeed;
        bulletDamage = data.bulletDamage;
        fireRate = data.fireRate;
        maxHealth = data.maxHealth;
        healthRegen = data.healthRegen;
        criticalChance = data.criticalChance;
        criticalDamage = data.criticalDamage;
        autoRocketCooldown = data.autoRocketCooldown;
        autoRocketDamage = data.autoRocketDamage;
        pendingUpgradeChoices = data.pendingUpgradeChoices;
        configuredPlayerSpeed = GameSettings.getPlayerSpeed();
        configuredBulletSpeed = GameSettings.getBulletSpeed();
        selectedUpgrades = new ArrayList<>();
        for (String type : data.selectedUpgradeTypes) {
            Upgrade upgrade = findUpgrade(type);
            if (upgrade != null) selectedUpgrades.add(upgrade);
        }
    }

    private Upgrade findUpgrade(String type) {
        for (Upgrade upgrade : availableUpgrades) {
            if (upgrade.getType().equals(type)) return upgrade;
        }
        return null;
    }

    public Snapshot createSnapshot() {
        return new Snapshot(currentLevel, currentExperience, experienceToNextLevel,
                playerSpeed, bulletSpeed, bulletDamage, fireRate, maxHealth,
                healthRegen, criticalChance, criticalDamage, autoRocketCooldown,
                autoRocketDamage, pendingUpgradeChoices, List.copyOf(selectedUpgrades));
    }

    public void restore(Snapshot snapshot) {
        currentLevel = snapshot.currentLevel();
        currentExperience = snapshot.currentExperience();
        experienceToNextLevel = snapshot.experienceToNextLevel();
        playerSpeed = snapshot.playerSpeed();
        bulletSpeed = snapshot.bulletSpeed();
        bulletDamage = snapshot.bulletDamage();
        fireRate = snapshot.fireRate();
        maxHealth = snapshot.maxHealth();
        healthRegen = snapshot.healthRegen();
        criticalChance = snapshot.criticalChance();
        criticalDamage = snapshot.criticalDamage();
        autoRocketCooldown = snapshot.autoRocketCooldown();
        autoRocketDamage = snapshot.autoRocketDamage();
        pendingUpgradeChoices = snapshot.pendingUpgradeChoices();
        selectedUpgrades = new ArrayList<>(snapshot.selectedUpgrades());
    }
    
    // Класс для улучшений
    public static class Upgrade {
        private final String type;
        private final String name;
        private final String description;

        public Upgrade(String type, String name, String description) {
            this.type = type;
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
    }
}
