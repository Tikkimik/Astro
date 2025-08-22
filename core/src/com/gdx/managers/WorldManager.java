package com.gdx.managers;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.*;
import com.gdx.game.MyGdxGame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.Math;

public class WorldManager {
    
    // Размер сектора мира
    private static final int SECTOR_SIZE = 2000;
    
    // Радиус генерации вокруг игрока
    private static final int GENERATION_RADIUS = 3000;
    
    // Радиус очистки объектов
    private static final int CLEANUP_RADIUS = 5000;
    
    // Карта загруженных секторов
    private Map<String, Boolean> loadedSectors;
    
    // Ссылки на списки объектов
    private ArrayList<Asteroid> asteroids;
    private ArrayList<OrkAsteroid> orkAsteroids;
    private ArrayList<OrkBullet> orkBullets;
    private Player player;
    
    public WorldManager(ArrayList<Asteroid> asteroids, ArrayList<OrkAsteroid> orkAsteroids, 
                       ArrayList<OrkBullet> orkBullets, Player player) {
        this.asteroids = asteroids;
        this.orkAsteroids = orkAsteroids;
        this.orkBullets = orkBullets;
        this.player = player;
        this.loadedSectors = new HashMap<>();
    }
    
    public void update(float dt) {
        // Получаем позицию игрока
        float playerX = player.getX();
        float playerY = player.getY();
        
        // Генерируем новые секторы вокруг игрока
        generateSectorsAroundPlayer(playerX, playerY);
        
        // Очищаем объекты, которые ушли слишком далеко
        cleanupDistantObjects(playerX, playerY);
    }
    
    private void generateSectorsAroundPlayer(float playerX, float playerY) {
        // Определяем сектор игрока
        int playerSectorX = (int)(playerX / SECTOR_SIZE);
        int playerSectorY = (int)(playerY / SECTOR_SIZE);
        
        // Проверяем секторы в радиусе генерации
        int sectorsToCheck = GENERATION_RADIUS / SECTOR_SIZE;
        
        for (int sx = playerSectorX - sectorsToCheck; sx <= playerSectorX + sectorsToCheck; sx++) {
            for (int sy = playerSectorY - sectorsToCheck; sy <= playerSectorY + sectorsToCheck; sy++) {
                String sectorKey = sx + "," + sy;
                
                // Если сектор еще не загружен
                if (!loadedSectors.containsKey(sectorKey)) {
                    generateSector(sx, sy);
                    loadedSectors.put(sectorKey, true);
                }
            }
        }
    }
    
    private void generateSector(int sectorX, int sectorY) {
        // Центр сектора
        float centerX = (sectorX + 0.5f) * SECTOR_SIZE;
        float centerY = (sectorY + 0.5f) * SECTOR_SIZE;
        
        // Генерируем астероиды в секторе
        int asteroidCount = MathUtils.random(3, 8);
        for (int i = 0; i < asteroidCount; i++) {
            float x = centerX + MathUtils.random(-SECTOR_SIZE/2, SECTOR_SIZE/2);
            float y = centerY + MathUtils.random(-SECTOR_SIZE/2, SECTOR_SIZE/2);
            
            // Создаем обычный астероид
            if (MathUtils.random() < 0.7f) {
                int type = MathUtils.random(0, 2); // SMALL, MEDIUM, LARGE
                Asteroid asteroid = new Asteroid(x, y, type);
                asteroids.add(asteroid);
            } else {
                // Создаем орк-астероид
                int type = MathUtils.random(0, 2); // SMALL, MEDIUM, LARGE
                OrkAsteroid orkAsteroid = new OrkAsteroid(x, y, type, player, orkBullets);
                orkAsteroids.add(orkAsteroid);
            }
        }
    }
    
    private void cleanupDistantObjects(float playerX, float playerY) {
        // Очищаем астероиды
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            Asteroid asteroid = asteroids.get(i);
            float distance = (float) Math.sqrt(
                (asteroid.getX() - playerX) * (asteroid.getX() - playerX) +
                (asteroid.getY() - playerY) * (asteroid.getY() - playerY)
            );
            
            if (distance > CLEANUP_RADIUS) {
                asteroids.remove(i);
            }
        }
        
        // Очищаем орк-астероиды
        for (int i = orkAsteroids.size() - 1; i >= 0; i--) {
            OrkAsteroid orkAsteroid = orkAsteroids.get(i);
            float distance = (float) Math.sqrt(
                (orkAsteroid.getX() - playerX) * (orkAsteroid.getX() - playerX) +
                (orkAsteroid.getY() - playerY) * (orkAsteroid.getY() - playerY)
            );
            
            if (distance > CLEANUP_RADIUS) {
                orkAsteroids.remove(i);
            }
        }
    }
    
    // Получить информацию о мире для отладки
    public String getWorldInfo() {
        return String.format("Sectors: %d | Asteroids: %d | OrkAsteroids: %d", 
            loadedSectors.size(), asteroids.size(), orkAsteroids.size());
    }
}
