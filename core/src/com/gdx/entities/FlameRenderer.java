package com.gdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.gdx.managers.Camera;
import com.gdx.shaders.FlameShader;

public class FlameRenderer {
    
    private FlameShader flameShader;
    private Mesh flameMesh;
    private float time;
    
    public FlameRenderer() {
        flameShader = new FlameShader();
        createFlameMesh();
    }
    
    private void createFlameMesh() {
        // Создаем меш для огня (конус из сопла)
        float[] vertices = {
            // позиция (x, y), текстурные координаты (u, v)
            -0.1f, -1.0f, 0.0f, 1.0f,  // левый нижний (узкий у сопла)
             0.1f, -1.0f, 1.0f, 1.0f,  // правый нижний (узкий у сопла)
             0.5f,  0.0f, 1.0f, 0.0f,  // правый верхний (широкий на конце)
            -0.5f,  0.0f, 0.0f, 0.0f   // левый верхний (широкий на конце)
        };
        
        short[] indices = {
            0, 1, 2,  // первый треугольник
            0, 2, 3   // второй треугольник
        };
        
        flameMesh = new Mesh(true, 4, 6, 
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"));
        
        flameMesh.setVertices(vertices);
        flameMesh.setIndices(indices);
    }
    
    public void renderFlame(float x, float y, float angle, float intensity, Camera camera) {
        if (intensity <= 0) return;
        
        time += Gdx.graphics.getDeltaTime();
        
        // Настраиваем шейдер
        flameShader.begin();
        flameShader.setTime(time);
        flameShader.setFlameIntensity(intensity);
        

        
        // Матрица проекции
        Matrix4 projectionMatrix = new Matrix4();
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        flameShader.setProjectionMatrix(projectionMatrix);
        
        // Включаем блендинг для прозрачности
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Преобразуем мировые координаты в экранные
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Смещаем огонь к задней части корабля (сопло)
        float nozzleOffset = 10.0f; // Смещение к соплу
        float nozzleX = screenX - MathUtils.cos(angle) * nozzleOffset;
        float nozzleY = screenY - MathUtils.sin(angle) * nozzleOffset;
        
        // Создаем матрицу трансформации
        Matrix4 transformMatrix = new Matrix4();
        transformMatrix.translate(nozzleX, nozzleY, 0);
        
        // Правильный угол - огонь должен быть направлен точно назад от корабля
        float flameAngle = angle; // Используем угол корабля напрямую
        transformMatrix.rotate(0, 0, 1, flameAngle * MathUtils.radiansToDegrees);
        
        // Размер огня
        float flameWidth = 20 + intensity * 15;
        float flameLength = flameWidth * 2.0f;
        transformMatrix.scale(flameWidth, flameLength, 1);
        
        // Применяем трансформацию
        ShaderProgram shader = flameShader.getShader();
        shader.setUniformMatrix("u_projTrans", projectionMatrix.cpy().mul(transformMatrix));
        
        // Рендерим меш
        flameMesh.render(shader, GL20.GL_TRIANGLES);
        
        // Отключаем блендинг
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        flameShader.end();
    }
    
    public void dispose() {
        if (flameMesh != null) {
            flameMesh.dispose();
        }
        if (flameShader != null) {
            flameShader.dispose();
        }
    }
}

