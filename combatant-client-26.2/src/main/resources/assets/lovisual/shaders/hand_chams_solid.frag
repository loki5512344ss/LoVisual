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

vec3 createVerticalGradient(vec2 coord, vec3 color1, vec3 color2, float t) {
    float factor = coord.y + t;
    factor = sin(factor * 3.14159 * 2.0) * 0.5 + 0.5;
    return mix(color1, color2, factor);
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
    vec3 gradientColor = createVerticalGradient(v_TexCoord, u_ColorA.rgb, u_ColorB.rgb, time);
    float brightness = 0.85 + sin(time * 6.28318) * 0.15;
    gradientColor *= brightness;

    vec3 finalColor = mix(base.rgb, gradientColor, u_Mix.x * maskGain);
    fragColor = vec4(finalColor, 1.0);
}
