package com.gdx.session;

import com.gdx.managers.GameObjectManager;
import com.gdx.managers.ProgressionManager;

public record GameSessionSnapshot(
        GameObjectManager.Snapshot objects,
        ProgressionManager.Snapshot progression,
        int level,
        int totalAsteroids,
        int numAsteroidsLeft,
        int score,
        int highScore,
        float autoRocketTimer,
        float elapsedTime
) {
}
