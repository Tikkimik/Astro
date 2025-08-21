varying vec2 v_texCoord;
varying float v_flameIntensity;
varying float v_time;

void main() {
    vec2 uv = v_texCoord;
    
    // Простой и надежный огонь
    float flame = 1.0 - uv.y; // Градиент снизу вверх
    flame *= (1.0 - uv.x * uv.x); // Сужение к краям
    
    // Применяем интенсивность
    float intensity = flame * v_flameIntensity;
    intensity = smoothstep(0.0, 1.0, intensity);
    
    // Очень яркие цвета огня как у истребителей
    vec3 innerColor = vec3(0.0, 1.0, 1.0); // Ярко-голубой центр
    vec3 middleColor = vec3(1.0, 0.8, 0.0); // Ярко-желтый
    vec3 outerColor = vec3(1.0, 0.0, 0.0); // Ярко-красный край
    
    // Сложная интерполяция для лучшего эффекта
    vec3 flameColor;
    if (intensity > 0.6) {
        flameColor = innerColor; // Голубой центр
    } else if (intensity > 0.3) {
        flameColor = mix(middleColor, innerColor, (intensity - 0.3) / 0.3); // Желтый к голубому
    } else {
        flameColor = mix(outerColor, middleColor, intensity / 0.3); // Красный к желтому
    }
    
    // Очень яркий огонь
    flameColor *= 6.0;
    
    // Добавляем мерцание
    float flicker = 0.9 + 0.1 * sin(v_time * 20.0);
    flameColor *= flicker;
    
    // Финальный цвет
    float alpha = intensity * flicker;
    gl_FragColor = vec4(flameColor, alpha);
}
