attribute vec2 a_position;
attribute vec2 a_texCoord;

uniform mat4 u_projTrans;
uniform float u_time;
uniform float u_flameIntensity;

varying vec2 v_texCoord;
varying float v_flameIntensity;
varying float v_time;

void main() {
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
    v_flameIntensity = u_flameIntensity;
    v_time = u_time;
}
