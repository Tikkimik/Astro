package com.gdx.managers;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Player;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import java.util.ArrayList;
import java.util.List;

public class ProgressionManager {
    
    // Система уровней
    private int currentLevel = 1;
    private int currentExperience = 0;
    private int experienceToNextLevel = 100; // Базовый опыт для следующего уровня
    
    // Статистики игрока
    private float playerSpeed = 300f;
    private float bulletSpeed = 350f;
    private float bulletDamage = 1f;
    private float fireRate = 1f;
    private int maxHealth = 3;
    private float healthRegen = 0f;
    private float criticalChance = 0.05f;
    private float criticalDamage = 2f;
    private float autoRocketCooldown = 1f;
    private float autoRocketDamage = 1f;
    
    // Доступные улучшения
    private List<Upgrade> availableUpgrades;
    private List<Upgrade> selectedUpgrades;
    private GameStateManager gameStateManager;
    
    public ProgressionManager() {
        availableUpgrades = new ArrayList<>();
        selectedUpgrades = new ArrayList<>();
        initializeUpgrades();
    }
    
    private void initializeUpgrades() {
        // Скорость игрока
        availableUpgrades.add(new Upgrade("Speed Boost", "Move speed +20%", 
            () -> playerSpeed *= 1.2f, "SPEED"));
        
        // Скорость пуль
        availableUpgrades.add(new Upgrade("Bullet Velocity", "Bullet speed +25%", 
            () -> bulletSpeed *= 1.25f, "BULLET_SPEED"));
        
        // Урон пуль
        availableUpgrades.add(new Upgrade("Bullet Damage", "Bullet damage +30%", 
            () -> bulletDamage *= 1.3f, "BULLET_DAMAGE"));
        
        // Скорость стрельбы
        availableUpgrades.add(new Upgrade("Rapid Fire", "Fire rate +20%", 
            () -> fireRate *= 1.2f, "FIRE_RATE"));
        
        // Максимальное здоровье
        availableUpgrades.add(new Upgrade("Health Boost", "Max health +1", 
            () -> maxHealth++, "MAX_HEALTH"));
        
        // Регенерация здоровья
        availableUpgrades.add(new Upgrade("Health Regen", "Health regen +0.5", 
            () -> healthRegen += 0.5f, "HEALTH_REGEN"));
        
        // Шанс критического удара
        availableUpgrades.add(new Upgrade("Critical Strike", "Crit chance +10%", 
            () -> criticalChance += 0.1f, "CRIT_CHANCE"));
        
        // Критический урон
        availableUpgrades.add(new Upgrade("Critical Damage", "Crit damage +50%", 
            () -> criticalDamage *= 1.5f, "CRIT_DAMAGE"));
        
        // Автоматические ракеты
        availableUpgrades.add(new Upgrade("Auto Rockets", "Rocket cooldown -15%", 
            () -> autoRocketCooldown *= 0.85f, "ROCKET_COOLDOWN"));
        
        // Урон ракет
        availableUpgrades.add(new Upgrade("Rocket Damage", "Rocket damage +40%", 
            () -> autoRocketDamage *= 1.4f, "ROCKET_DAMAGE"));
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
        
        // Увеличиваем опыт для следующего уровня (как в Magic Survival)
        experienceToNextLevel = (int)(experienceToNextLevel * 1.15f);
        
        // Показываем экран выбора улучшений
        showUpgradeScreen();
    }
    
    private void showUpgradeSelection() {
        // Здесь будет логика показа экрана выбора улучшений
        // Пока что просто выбираем случайное улучшение
        selectRandomUpgrade();
    }
    
    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }
    
    public void showUpgradeScreen() {
        if (gameStateManager != null) {
            gameStateManager.setState(GameStateManager.UPGRADE_SELECTION, this);
        }
    }
    
    public void selectRandomUpgrade() {
        if (availableUpgrades.isEmpty()) return;
        
        int randomIndex = MathUtils.random(availableUpgrades.size() - 1);
        Upgrade selectedUpgrade = availableUpgrades.get(randomIndex);
        
        // Применяем улучшение
        selectedUpgrade.apply();
        selectedUpgrades.add(selectedUpgrade);
        
        // НЕ удаляем из доступных - позволяем выбирать снова
        // availableUpgrades.remove(randomIndex);
    }
    
    // Метод для применения улучшений к игровым объектам
    public void applyUpgradesToGameObjects(Player player, ArrayList<AutoRocket> autoRockets, ArrayList<Bullet> bullets) {
        // Применяем улучшения к игроку
        if (player != null) {
            player.setSpeed(playerSpeed);
            player.setFireRate(fireRate);
            player.setMaxHealth(maxHealth);
            player.setHealthRegen(healthRegen);
        }
        
        // Применяем улучшения к существующим ракетам
        if (autoRockets != null) {
            for (AutoRocket rocket : autoRockets) {
                rocket.setDamage(autoRocketDamage);
            }
        }
        
        // Применяем улучшения к существующим пулям
        if (bullets != null) {
            for (Bullet bullet : bullets) {
                bullet.setDamage(bulletDamage);
            }
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
    
    // Геттеры для системы уровней
    public int getCurrentLevel() { return currentLevel; }
    public int getCurrentExperience() { return currentExperience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public float getExperienceProgress() { 
        return (float)currentExperience / experienceToNextLevel; 
    }
    
    public List<Upgrade> getAvailableUpgrades() { return availableUpgrades; }
    public List<Upgrade> getSelectedUpgrades() { return selectedUpgrades; }
    
    // Класс для улучшений
    public static class Upgrade {
        private String name;
        private String description;
        private Runnable effect;
        private String type;
        
        public Upgrade(String name, String description, Runnable effect, String type) {
            this.name = name;
            this.description = description;
            this.effect = effect;
            this.type = type;
        }
        
        public void apply() {
            effect.run();
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
    }
}
