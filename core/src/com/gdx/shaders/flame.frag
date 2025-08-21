#version 330 core

in vec2 v_texCoord;
in float v_flameIntensity;
in float v_time;

out vec4 fragColor;

// Шумовая функция для создания турбулентности огня
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Фрактальный шум для более реалистичного огня
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for(int i = 0; i < 4; i++) {
        value += amplitude * noise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}

void main() {
    vec2 uv = v_texCoord;
    
    // Создаем турбулентность огня
    float turbulence = fbm(uv * vec2(3.0, 1.0) + v_time * vec2(0.0, -2.0));
    
    // Создаем волны в огне
    float wave = sin(uv.y * 10.0 + v_time * 8.0) * 0.1;
    
    // Комбинируем эффекты
    float flame = turbulence + wave;
    
    // Создаем градиент огня (от центра к краям)
    float gradient = 1.0 - length(uv - vec2(0.5, 0.5)) * 2.0;
    gradient = smoothstep(0.0, 1.0, gradient);
    
    // Добавляем интенсивность огня
    float intensity = flame * gradient * v_flameIntensity;
    
    // Цвета огня (от белого центра к красным краям)
    vec3 innerColor = vec3(1.0, 1.0, 1.0); // Белый центр
    vec3 middleColor = vec3(1.0, 0.6, 0.2); // Оранжевый
    vec3 outerColor = vec3(1.0, 0.2, 0.1); // Красный
    
    // Интерполяция цветов
    vec3 flameColor = mix(outerColor, middleColor, intensity);
    flameColor = mix(flameColor, innerColor, intensity * intensity);
    
    // Добавляем мерцание
    float flicker = 0.8 + 0.2 * sin(v_time * 15.0 + turbulence * 10.0);
    
    // Финальный цвет с прозрачностью
    float alpha = intensity * flicker;
    alpha = smoothstep(0.0, 1.0, alpha);
    
    fragColor = vec4(flameColor * flicker, alpha);
}

