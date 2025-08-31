package com.gdx.managers;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.gdx.entities.*;
import com.gdx.game.MyGdxGame;
import com.gdx.utils.SpatialGrid;

public class WorldManager {
    
    // Размер сектора мира
    private static final int SECTOR_SIZE = 2000;
    
    // Радиус генерации вокруг игрока
    private static final int GENERATION_RADIUS = 3000;
    
    // Радиус очистки объектов
    private static final int CLEANUP_RADIUS = 5000;
    
    // Карта загруженных секторов (используем ObjectMap вместо HashMap)
    private ObjectMap<String, Boolean> loadedSectors;
    
    // Ссылки на списки объектов (используем Array вместо ArrayList)
    private Array<Asteroid> asteroids;
    private Array<OrkAsteroid> orkAsteroids;
    private Array<OrkBullet> orkBullets;
    private Array<EnemyShip> enemyShips;
    private Player player;
    
    // Система пространственного разделения для оптимизации коллизий
    private SpatialGrid spatialGrid;
    
    public WorldManager(Array<Asteroid> asteroids, Array<OrkAsteroid> orkAsteroids, 
                       Array<OrkBullet> orkBullets, Array<EnemyShip> enemyShips, Player player) {
        this.asteroids = asteroids;
        this.orkAsteroids = orkAsteroids;
        this.orkBullets = orkBullets;
        this.enemyShips = enemyShips;
        this.player = player;
        this.loadedSectors = new ObjectMap<>();
        
        // Инициализируем пространственную сетку с размером ячейки 500
        this.spatialGrid = new SpatialGrid(500);
    }
    
    public void update(float dt, boolean isRestoredFromSave) {
        // Получаем позицию игрока
        float playerX = player.getX();
        float playerY = player.getY();
        
        // Генерируем новые секторы вокруг игрока (только если это не восстановление из сохранения)
        if (!isRestoredFromSave) {
            generateSectorsAroundPlayer(playerX, playerY);
        }
        
        // Очищаем объекты, которые ушли слишком далеко
        cleanupDistantObjects(playerX, playerY);
        
        // Обновляем пространственную сетку
        updateSpatialGrid();
    }
    
    // Метод для обновления ссылок после восстановления состояния
    public void updateReferences(Array<Asteroid> asteroids, Array<OrkAsteroid> orkAsteroids, 
                                Array<OrkBullet> orkBullets, Array<EnemyShip> enemyShips, Player player) {
        this.asteroids = asteroids;
        this.orkAsteroids = orkAsteroids;
        this.orkBullets = orkBullets;
        this.enemyShips = enemyShips;
        this.player = player;
        
        // Инициализируем loadedSectors на основе существующих астероидов
        initializeLoadedSectors();
        
        // Пересоздаем пространственную сетку
        this.spatialGrid = new SpatialGrid(500);
        rebuildSpatialGrid();
    }
    
    // Инициализируем loadedSectors на основе существующих объектов
    private void initializeLoadedSectors() {
        loadedSectors.clear();
        
        // Добавляем секторы для существующих астероидов
        for (Asteroid asteroid : asteroids) {
            int sectorX = (int)(asteroid.getX() / SECTOR_SIZE);
            int sectorY = (int)(asteroid.getY() / SECTOR_SIZE);
            String sectorKey = sectorX + "," + sectorY;
            loadedSectors.put(sectorKey, true);
        }
        
        // Добавляем секторы для существующих орк-астероидов
        for (OrkAsteroid orkAsteroid : orkAsteroids) {
            int sectorX = (int)(orkAsteroid.getX() / SECTOR_SIZE);
            int sectorY = (int)(orkAsteroid.getY() / SECTOR_SIZE);
            String sectorKey = sectorX + "," + sectorY;
            loadedSectors.put(sectorKey, true);
        }
        
        // Добавляем секторы для существующих вражеских кораблей
        for (EnemyShip enemyShip : enemyShips) {
            int sectorX = (int)(enemyShip.getX() / SECTOR_SIZE);
            int sectorY = (int)(enemyShip.getY() / SECTOR_SIZE);
            String sectorKey = sectorX + "," + sectorY;
            loadedSectors.put(sectorKey, true);
        }
        
        System.out.println("Инициализировано " + loadedSectors.size + " секторов на основе существующих объектов");
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
        System.out.println("Генерируем сектор (" + sectorX + ", " + sectorY + ") с " + asteroidCount + " астероидами");
        
        for (int i = 0; i < asteroidCount; i++) {
            float x = centerX + MathUtils.random(-SECTOR_SIZE/2, SECTOR_SIZE/2);
            float y = centerY + MathUtils.random(-SECTOR_SIZE/2, SECTOR_SIZE/2);
            
            // Создаем обычный астероид
            if (MathUtils.random() < 0.3f) {
                int type = MathUtils.random(0, 2); // SMALL, MEDIUM, LARGE
                Asteroid asteroid = new Asteroid(x, y, type);
                asteroids.add(asteroid);
                System.out.println("Создан астероид в позиции (" + x + ", " + y + ")");
            } else if (MathUtils.random() < 0.3f) { // Уменьшили вероятность с 0.5 до 0.3
                // Создаем орк-астероид (оптимизированный)
                int type = MathUtils.random(0, 2); // SMALL, MEDIUM, LARGE
                OrkAsteroid orkAsteroid = new OrkAsteroid(x, y, type, player, orkBullets);
                orkAsteroids.add(orkAsteroid);
                System.out.println("Создан орк-астероид в позиции (" + x + ", " + y + ")");
            } else if (MathUtils.random() < 0.4f) { // Вернули нормальную вероятность
                // Создаем вражеский корабль
                EnemyShip enemyShip = new EnemyShip(x, y, player, orkBullets);
                enemyShips.add(enemyShip);
                // System.out.println("Создан вражеский корабль в позиции (" + x + ", " + y + ")");
            }
        }
    }
    
    private void cleanupDistantObjects(float playerX, float playerY) {
        // Очищаем астероиды (оптимизированная итерация)
        for (int i = asteroids.size - 1; i >= 0; i--) {
            Asteroid asteroid = asteroids.get(i);
            float distanceSq = (asteroid.getX() - playerX) * (asteroid.getX() - playerX) +
                              (asteroid.getY() - playerY) * (asteroid.getY() - playerY);
            
            if (distanceSq > CLEANUP_RADIUS * CLEANUP_RADIUS) {
                asteroids.removeIndex(i);
            }
        }
        
        // Очищаем орк-астероиды
        for (int i = orkAsteroids.size - 1; i >= 0; i--) {
            OrkAsteroid orkAsteroid = orkAsteroids.get(i);
            float distanceSq = (orkAsteroid.getX() - playerX) * (orkAsteroid.getX() - playerX) +
                              (orkAsteroid.getY() - playerY) * (orkAsteroid.getY() - playerY);
            
            if (distanceSq > CLEANUP_RADIUS * CLEANUP_RADIUS) {
                orkAsteroids.removeIndex(i);
            }
        }
        
        // Очищаем вражеские корабли (больший радиус для более долгой жизни)
        for (int i = enemyShips.size - 1; i >= 0; i--) {
            EnemyShip enemyShip = enemyShips.get(i);
            float distanceSq = (enemyShip.getX() - playerX) * (enemyShip.getX() - playerX) +
                              (enemyShip.getY() - playerY) * (enemyShip.getY() - playerY);
            
            // Вражеские корабли живут дольше - больший радиус очистки
            if (distanceSq > (CLEANUP_RADIUS * 1.5f) * (CLEANUP_RADIUS * 1.5f)) {
                enemyShips.removeIndex(i);
            }
        }
    }
    
    // Обновляем пространственную сетку
    private void updateSpatialGrid() {
        spatialGrid.clear();
        
        // Добавляем все объекты в сетку
        for (Asteroid asteroid : asteroids) {
            spatialGrid.add(asteroid);
        }
        for (OrkAsteroid orkAsteroid : orkAsteroids) {
            spatialGrid.add(orkAsteroid);
        }
        for (EnemyShip enemyShip : enemyShips) {
            spatialGrid.add(enemyShip);
        }
        for (OrkBullet orkBullet : orkBullets) {
            spatialGrid.add(orkBullet);
        }
    }
    
    // Пересоздаем пространственную сетку
    private void rebuildSpatialGrid() {
        spatialGrid.clear();
        updateSpatialGrid();
    }
    
    // Получить объекты в радиусе (для оптимизации коллизий)
    public Array<SpaceObject> getObjectsInRadius(float x, float y, float radius) {
        return spatialGrid.getNearbyObjects(x, y, radius);
    }
    
    // Получить информацию о мире для отладки
    public String getWorldInfo() {
        return String.format("Sectors: %d | Asteroids: %d | OrkAsteroids: %d | EnemyShips: %d | Grid Cells: %d", 
            loadedSectors.size, asteroids.size, orkAsteroids.size, enemyShips.size, spatialGrid.getCellCount());
    }
}
