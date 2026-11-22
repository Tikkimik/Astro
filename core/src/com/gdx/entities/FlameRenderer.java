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

    private static final int CURVE_SEGMENTS = 12;
    private static final float CURVE_STRENGTH = 0.78f;

    private FlameShader flameShader;
    private Mesh flameMesh;
    private final float[] flameVertices = new float[(CURVE_SEGMENTS + 1) * 2 * 4];
    private float time;

    // Переиспользуемые матрицы, чтобы не аллоцировать по 3 объекта на кадр
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();

    public FlameRenderer() {
        flameShader = new FlameShader();
        createFlameMesh();
    }

    private void createFlameMesh() {
        short[] indices = new short[CURVE_SEGMENTS * 6];
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            int vertex = i * 2;
            int index = i * 6;
            indices[index] = (short) vertex;
            indices[index + 1] = (short) (vertex + 1);
            indices[index + 2] = (short) (vertex + 3);
            indices[index + 3] = (short) vertex;
            indices[index + 4] = (short) (vertex + 3);
            indices[index + 5] = (short) (vertex + 2);
        }

        flameMesh = new Mesh(true, (CURVE_SEGMENTS + 1) * 2, indices.length,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"));

        flameMesh.setIndices(indices);
        updateFlameMesh(0f, 1f, 1f);
    }

    /**
     * Изгибаем геометрию, а не только маску внутри прямого прямоугольника.
     * Благодаря этому дальний край факела может физически сместиться на
     * десятки пикселей и продолжить инерционный след при резком манёвре.
     */
    private void updateFlameMesh(float bend, float length, float width) {
        float aspect = length / Math.max(1f, width);
        int cursor = 0;
        for (int i = 0; i <= CURVE_SEGMENTS; i++) {
            float t = i / (float) CURVE_SEGMENTS;
            float centerY = bend * aspect * CURVE_STRENGTH * t * t;
            float localX = -t;

            flameVertices[cursor++] = localX;
            flameVertices[cursor++] = centerY - 0.5f;
            flameVertices[cursor++] = 0f;
            flameVertices[cursor++] = t;

            flameVertices[cursor++] = localX;
            flameVertices[cursor++] = centerY + 0.5f;
            flameVertices[cursor++] = 1f;
            flameVertices[cursor++] = t;
        }
        flameMesh.setVertices(flameVertices);
    }

    public void renderFlame(float x, float y, float angle, float intensity,
            float width, float length, float bend, Camera camera) {
        if (intensity <= 0) return;

        updateFlameMesh(bend, length, width);

        // Настраиваем шейдер
        flameShader.begin();
        flameShader.setTime(time);
        flameShader.setFlameIntensity(intensity);
        // Основная кривизна уже находится в меше; малую долю оставляем
        // шейдеру только для живого смещения горячего ядра внутри оболочки.
        flameShader.setBend(bend * 0.12f);



        // Матрица проекции (переиспользуемая)
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        flameShader.setProjectionMatrix(projectionMatrix);

        // Включаем блендинг для прозрачности
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Преобразуем мировые координаты в экранные
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);

        // Матрица трансформации (переиспользуемая)
        transformMatrix.idt();
        transformMatrix.translate(screenX, screenY, 0);

        // Правильный угол - огонь должен быть направлен точно назад от корабля
        float flameAngle = angle; // Используем угол корабля напрямую
        transformMatrix.rotate(0, 0, 1, flameAngle * MathUtils.radiansToDegrees);

        float zoom = camera.getCurrentZoom();
        transformMatrix.scale(length * zoom, width * zoom, 1);

        // Применяем трансформацию
        ShaderProgram shader = flameShader.getShader();
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        shader.setUniformMatrix("u_projTrans", combinedMatrix);

        // Рендерим меш
        flameMesh.render(shader, GL20.GL_TRIANGLES);

        flameShader.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Продвинуть общую фазу один раз за кадр, независимо от числа слоёв. */
    public void advanceTime() {
        time += Gdx.graphics.getDeltaTime();
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
