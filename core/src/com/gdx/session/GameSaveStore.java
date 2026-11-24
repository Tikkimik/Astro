package com.gdx.session;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.gdx.utils.GameLogger;

public final class GameSaveStore {
    private static final String DEFAULT_PATH = "saves/session.json";

    private final FileHandle file;
    private final FileHandle temporaryFile;
    private final FileHandle backupFile;
    private final Json json = new Json();
    private final GameSessionSaveMapper mapper = new GameSessionSaveMapper();

    public GameSaveStore(FileHandle file) {
        this.file = file;
        this.temporaryFile = file.sibling(file.name() + ".tmp");
        this.backupFile = file.sibling(file.name() + ".bak");
        json.setUsePrototypes(false);
    }

    public static GameSaveStore createDefault() {
        return new GameSaveStore(Gdx.files.local(DEFAULT_PATH));
    }

    public boolean save(GameSaveData data) {
        try {
            mapper.validate(data);
            file.parent().mkdirs();
            temporaryFile.writeString(json.prettyPrint(data), false, "UTF-8");
            mapper.validate(read(temporaryFile));

            if (backupFile.exists()) backupFile.delete();
            if (file.exists()) file.moveTo(backupFile);
            temporaryFile.moveTo(file);
            return true;
        } catch (RuntimeException error) {
            GameLogger.error("Не удалось сохранить игру: " + error.getMessage());
            if (!file.exists() && backupFile.exists()) backupFile.moveTo(file);
            return false;
        } finally {
            if (temporaryFile.exists()) temporaryFile.delete();
        }
    }

    public GameSaveData load() {
        GameSaveData primary = tryRead(file);
        return primary != null ? primary : tryRead(backupFile);
    }

    public boolean hasValidSave() {
        return load() != null;
    }

    public void delete() {
        if (file.exists()) file.delete();
        if (temporaryFile.exists()) temporaryFile.delete();
        if (backupFile.exists()) backupFile.delete();
    }

    public String path() {
        return file.path();
    }

    private GameSaveData tryRead(FileHandle source) {
        if (!source.exists()) return null;
        try {
            GameSaveData data = read(source);
            mapper.validate(data);
            return data;
        } catch (RuntimeException error) {
            GameLogger.warn("Сохранение не прочитано " + source.path() + ": " + error.getMessage());
            return null;
        }
    }

    private GameSaveData read(FileHandle source) {
        return json.fromJson(GameSaveData.class, source.readString("UTF-8"));
    }
}
