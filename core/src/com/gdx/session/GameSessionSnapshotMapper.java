package com.gdx.session;

/**
 * Единая граница преобразования живой сессии в runtime checkpoint и обратно.
 * Текущий снимок сохраняет идентичность игровых объектов и не является
 * форматом для записи на диск.
 */
public final class GameSessionSnapshotMapper {

    public GameSessionSnapshot capture(GameSession session) {
        return new GameSessionSnapshot(
                session.getObjects().createSnapshot(),
                session.getProgression().createSnapshot(),
                session.getLevel(),
                session.getTotalAsteroids(),
                session.getNumAsteroidsLeft(),
                session.getScore(),
                session.getHighScore(),
                session.getAutoRocketTimer(),
                session.getElapsedTime());
    }

    public void restore(GameSession session, GameSessionSnapshot snapshot) {
        session.getObjects().restore(snapshot.objects());
        session.getProgression().restore(snapshot.progression());
        session.applyMappedState(snapshot.level(), snapshot.totalAsteroids(),
                snapshot.numAsteroidsLeft(), snapshot.score(), snapshot.highScore(),
                snapshot.autoRocketTimer(), snapshot.elapsedTime());
        session.getWorld().updateReferences(session.getObjects(), session.getPlayer());
    }
}
