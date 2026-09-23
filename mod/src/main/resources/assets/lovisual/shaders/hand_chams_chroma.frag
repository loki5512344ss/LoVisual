#version 330 core

in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D u_Src;
uniform sampler2D u_Mask;

layout (std140) uniform HandChams {
    vec4 u_View;   // x = resX, y = resY, z = time
    vec4 u_Mix;    // x = effectAlpha, y = chromaSpeed, z = chromaSaturation, w = chromaBrightness
    vec4 u_ColorA; // rgb = colorA
    vec4 u_ColorB; // rgb = colorB
    vec4 u_ColorC; // rgb = colorC
    vec4 u_ColorD; // rgb = colorD
    vec4 u_Common; // x = snowIntensity, y = snowSpeed, z = stripesWidth, w = stripesSpeed
    vec4 u_Glow;   // x = fillGlow, y = glowRadius, z = glowPower, w = glowDispersion
    vec4 u_Flags;  // x = hideHand
};

float handMaskAt(vec2 uv) {
    vec4 m = texture(u_Mask, uv);
    return max(m.a, max(m.r, max(m.g, m.b)));
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 createChromaEffect(vec2 coord, float t) {
    float hue = fract(coord.y * 0.5 + coord.x * 0.3 + t * u_Mix.y);
    float wave = sin(coord.y * 10.0 + t * 3.0) * 0.05;
    hue += wave;
    return hsv2rgb(vec3(hue, u_Mix.z, u_Mix.w));
}

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);
    if (mask <= 0.02) {
        fragColor = base;
        return;
    }

    float time = u_View.z;
    float maskGain = smoothstep(0.02, 0.3, mask);
    vec3 chromaColor = createChromaEffect(v_TexCoord, time);
    float pulse = 0.9 + sin(time * 4.0) * 0.1;
    chromaColor *= pulse;

    vec3 finalColor = mix(base.rgb, chromaColor, u_Mix.x * maskGain);
    fragColor = vec4(finalColor, 1.0);
}
