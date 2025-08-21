package com.gdx.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class FlameShader {
    
    private ShaderProgram shader;
    private float time;
    private Vector2 flameDirection;
    private float flameIntensity;
    
    public FlameShader() {
        // Загружаем шейдеры
        String vertexShader = Gdx.files.internal("shaders/flame.vert").readString();
        String fragmentShader = Gdx.files.internal("shaders/flame.frag").readString();
        
        shader = new ShaderProgram(vertexShader, fragmentShader);
        
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }
        
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
        if (shader != null) {
            shader.dispose();
        }
    }
}

