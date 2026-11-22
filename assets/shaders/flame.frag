#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
varying float v_flameIntensity;
varying float v_time;
uniform float u_bend;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    float axial = v_texCoord.y;
    float axisNoise = noise(vec2(axial * 4.2 - v_time * 1.25, 2.37)) - 0.5;
    float center = u_bend * 0.65 * axial * axial + axisNoise * axial * 0.032;
    float lateral = abs(v_texCoord.x * 2.0 - 1.0 - center);

    float coarseNoise = noise(vec2(axial * 8.0 - v_time * 3.4,
                                   v_texCoord.x * 5.0 + v_time * 0.9)) - 0.5;
    float fineNoise = noise(vec2(axial * 19.0 + v_time * 5.2,
                                 v_texCoord.x * 11.0 - v_time * 2.0)) - 0.5;

    // Узкий плотный столб, который лишь немного расширяется к распадающемуся
    // концу — форма атмосферного ракетного факела, а не белой капли.
    float radius = mix(0.28, 0.64, smoothstep(0.0, 0.78, axial));
    radius += smoothstep(0.78, 1.0, axial) * 0.10;
    float breathing = noise(vec2(axial * 5.1 - v_time * 1.55, 0.71)) - 0.5;
    radius *= 1.0 + breathing * 0.14 * smoothstep(0.08, 1.0, axial);
    float disturbed = lateral + coarseNoise * (0.012 + axial * 0.032)
                              + fineNoise * axial * 0.008;

    float outer = 1.0 - smoothstep(radius * 0.52, radius * 1.08, disturbed);
    float body = 1.0 - smoothstep(radius * 0.28, radius * 0.78, disturbed);
    float core = 1.0 - smoothstep(radius * 0.06, radius * 0.52, disturbed);

    float breakup = noise(vec2(axial * 10.0 - v_time * 3.1,
                               lateral * 7.0 + v_time * 1.4));
    float endFade = 1.0 - smoothstep(0.72 + breakup * 0.035, 1.0, axial);
    float nozzleFade = smoothstep(0.0, 0.035, axial);
    float coreFade = 1.0 - smoothstep(0.52, 0.80, axial);

    // Слабые светлые кольца внутри ядра имитируют структуру ударных волн,
    // не разбивая струю на отдельные шарики.
    float shock = 0.90 + 0.10 * pow(0.5 + 0.5 * sin(axial * 43.0 - v_time * 2.2), 6.0);

    vec3 edgeColor = mix(vec3(0.92, 0.075, 0.008),
                         vec3(0.55, 0.055, 0.012), axial);
    vec3 bodyColor = mix(vec3(1.0, 0.34, 0.025),
                         vec3(1.0, 0.17, 0.012), axial);
    vec3 coreColor = mix(vec3(1.0, 1.0, 0.86),
                         vec3(1.0, 0.58, 0.10), axial);

    vec3 color = edgeColor * outer;
    color = mix(color, bodyColor * (1.05 + body * 0.35), body);
    color = mix(color, coreColor * (1.25 * shock), core * coreFade);

    float flicker = 0.96 + 0.04 * sin(v_time * 31.0 + axial * 9.0);
    float alpha = max(outer * 0.50, max(body * 0.78, core * 0.98));
    alpha *= endFade * nozzleFade * flicker * v_flameIntensity;
    gl_FragColor = vec4(color, alpha);
}
