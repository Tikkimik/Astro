package com.gdx.combat;

import com.gdx.entities.Asteroid;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.OrkAsteroid;

public final class CombatRewardRules {

    private CombatRewardRules() {}

    public static int pointsFor(DestructionEvent event) {
        if (event.target() instanceof Asteroid asteroid) {
            return asteroid.getType() == Asteroid.SMALL ? 5 : 0;
        }
        if (event.target() instanceof OrkAsteroid) return 10;
        if (event.target() instanceof EnemyShip) return 25;
        return 0;
    }
}
