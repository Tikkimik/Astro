package com.gdx.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.gdx.game.MyGdxGame;
import com.gdx.utils.PerformanceMonitor;

public class FlameShader {
    
    // Статический кэш шейдеров для всех экземпляров
    private static ShaderProgram cachedShader;
    private static boolean shaderInitialized = false;
    private static final Object shaderLock = new Object();
    
    private ShaderProgram shader;
    private float time;
    private Vector2 flameDirection;
    private float flameIntensity;
    
    public FlameShader() {
        // Используем кэшированный шейдер
        synchronized (shaderLock) {
            if (!shaderInitialized) {
                try {
                    // Загружаем шейдеры только один раз
                    String vertexShader = Gdx.files.internal("shaders/flame.vert").readString();
                    String fragmentShader = Gdx.files.internal("shaders/flame.frag").readString();
                    
                    cachedShader = new ShaderProgram(vertexShader, fragmentShader);
                    
                    if (!cachedShader.isCompiled()) {
                        throw new RuntimeException("Shader compilation failed: " + cachedShader.getLog());
                    }
                    
                    shaderInitialized = true;
                    PerformanceMonitor.incrementShadersCreated();
                    if (MyGdxGame.DEBUG_MODE) {
                        System.out.println("FlameShader: Шейдер успешно скомпилирован и кэширован");
                    }
                } catch (Exception e) {
                    System.err.println("FlameShader: Ошибка загрузки шейдера: " + e.getMessage());
                    throw new RuntimeException("Failed to load shader", e);
                }
            } else {
                PerformanceMonitor.incrementShadersReused();
            }
        }
        
        this.shader = cachedShader;
        flameDirection = new Vector2(0, -1);
        flameIntensity = 1.0f;
    }
    
    public void begin() {
        shader.begin();
    }
    
    public void end() {
        shader.end();
    }
    
    public void setProjectionMatrix(Matrix4 matrix) {
        shader.setUniformMatrix("u_projTrans", matrix);
    }
    
    public void setTime(float time) {
        this.time = time;
        shader.setUniformf("u_time", time);
    }
    

    
    public void setFlameIntensity(float intensity) {
        this.flameIntensity = intensity;
        shader.setUniformf("u_flameIntensity", intensity);
    }
    
    public ShaderProgram getShader() {
        return shader;
    }
    
    public void dispose() {
        // Не удаляем кэшированный шейдер, так как он используется другими экземплярами
        // shader = null;
    }
    
    // Статический метод для очистки кэша шейдера (вызывать только при завершении игры)
    public static void disposeCache() {
        synchronized (shaderLock) {
            if (cachedShader != null) {
                cachedShader.dispose();
                cachedShader = null;
                shaderInitialized = false;
            }
        }
    }
}

