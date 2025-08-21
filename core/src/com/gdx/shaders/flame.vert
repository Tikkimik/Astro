#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord;

uniform mat4 u_projTrans;
uniform float u_time;
uniform vec2 u_flameDirection;
uniform float u_flameIntensity;

out vec2 v_texCoord;
out float v_flameIntensity;
out float v_time;

void main() {
    gl_Position = u_projTrans * vec4(position, 0.0, 1.0);
    v_texCoord = texCoord;
    v_flameIntensity = u_flameIntensity;
    v_time = u_time;
}

