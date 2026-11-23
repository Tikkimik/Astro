package com.gdx.combat;

import com.gdx.entities.SpaceObject;

public record DestructionEvent(
        Kind kind,
        SpaceObject target,
        Cause cause,
        SpaceObject source,
        float x,
        float y
) {
    public enum Kind {
        ASTEROID_STAGE,
        ENEMY
    }

    public enum Cause {
        DIRECT_PROJECTILE,
        ROCKET_SPLASH,
        PLAYER_COLLISION
    }
}
