package com.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.gdx.entities.SpaceObject;
import java.util.HashMap;
import java.util.Map;

public class SpatialGrid {
    
    private final float cellSize;
    private final Map<String, Array<SpaceObject>> grid;
    private final Array<SpaceObject> tempObjects;
    
    public SpatialGrid(float cellSize) {
        this.cellSize = cellSize;
        this.grid = new HashMap<>();
        this.tempObjects = new Array<>();
    }
    
    public void clear() {
        grid.clear();
    }
    
    public void add(SpaceObject object) {
        String key = getGridKey(object.getX(), object.getY());
        Array<SpaceObject> cell = grid.get(key);
        if (cell == null) {
            cell = new Array<>();
            grid.put(key, cell);
        }
        cell.add(object);
    }
    
    public void remove(SpaceObject object) {
        String key = getGridKey(object.getX(), object.getY());
        Array<SpaceObject> cell = grid.get(key);
        if (cell != null) {
            cell.removeValue(object, true);
        }
    }
    
    public Array<SpaceObject> getNearbyObjects(float x, float y, float radius) {
        tempObjects.clear();
        
        int minCellX = (int)((x - radius) / cellSize);
        int maxCellX = (int)((x + radius) / cellSize);
        int minCellY = (int)((y - radius) / cellSize);
        int maxCellY = (int)((y + radius) / cellSize);
        
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                String key = cellX + "," + cellY;
                Array<SpaceObject> cell = grid.get(key);
                if (cell != null) {
                    tempObjects.addAll(cell);
                }
            }
        }
        
        return tempObjects;
    }
    
    public Array<SpaceObject> getObjectsInCell(float x, float y) {
        String key = getGridKey(x, y);
        Array<SpaceObject> cell = grid.get(key);
        return cell != null ? cell : new Array<>();
    }
    
    private String getGridKey(float x, float y) {
        int cellX = (int)(x / cellSize);
        int cellY = (int)(y / cellSize);
        return cellX + "," + cellY;
    }
    
    public void updateObjectPosition(SpaceObject object, float oldX, float oldY) {
        String oldKey = getGridKey(oldX, oldY);
        String newKey = getGridKey(object.getX(), object.getY());
        
        if (!oldKey.equals(newKey)) {
            // Удаляем из старой ячейки
            Array<SpaceObject> oldCell = grid.get(oldKey);
            if (oldCell != null) {
                oldCell.removeValue(object, true);
            }
            
            // Добавляем в новую ячейку
            add(object);
        }
    }
    
    public int getCellCount() {
        return grid.size();
    }
    
    public int getTotalObjectCount() {
        int count = 0;
        for (Array<SpaceObject> cell : grid.values()) {
            count += cell.size;
        }
        return count;
    }
}
