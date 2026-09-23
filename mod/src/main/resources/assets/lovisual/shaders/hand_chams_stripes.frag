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

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);
    if (mask <= 0.02) {
        fragColor = base;
        return;
    }

    vec2 effectUv = v_TexCoord;
    effectUv.x *= u_View.x / max(u_View.y, 1.0);

    // Плавные анимированные диагональные полосы с мягкими краями
    float angle = 0.65;
    float d = (effectUv.x * cos(angle) + effectUv.y * sin(angle)) + u_View.z * u_Common.w;
    float stripe = abs(sin(d * 3.14159265 / max(u_Common.z, 0.001)));
    stripe = pow(stripe, 1.5);

    vec3 col = mix(u_ColorB.rgb, u_ColorA.rgb, stripe);
    col += u_ColorA.rgb * (0.12 + 0.10 * sin(d * 6.0 + u_View.z * 2.0));

    float strength = u_Mix.x * (0.35 + 0.65 * stripe) * smoothstep(0.02, 0.3, mask);
    vec3 finalColor = mix(base.rgb, col, strength);
    fragColor = vec4(finalColor, 1.0);
}
