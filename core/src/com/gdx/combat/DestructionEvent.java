package com.gdx.combat;

import com.gdx.entities.SpaceObject;

public record DestructionEvent(
        Kind kind,
        SpaceObject target,
        float x,
        float y
) {
    public enum Kind {
        ASTEROID_STAGE,
        ENEMY
    }
}
