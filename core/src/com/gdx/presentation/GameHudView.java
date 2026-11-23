package com.gdx.presentation;

public record GameHudView(
        int score,
        int highScore,
        int level,
        int experience,
        int experienceToNextLevel,
        float experienceProgress,
        int viewportWidth,
        int viewportHeight
) {
}
