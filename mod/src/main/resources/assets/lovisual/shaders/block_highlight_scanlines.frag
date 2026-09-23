#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 */

out vec4 color;

layout (std140) uniform WaterData {
    vec4 u_Color;
    vec4 u_PatternColor;
    float u_Time;
};

in vec2 v_TexCoord;

void main() {
    vec2 uv = v_TexCoord;
    float time = u_Time;

    float distToEdge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeGlow = exp(-distToEdge * 20.0);

    float scanY = fract(uv.y * 3.0 - time * 1.4);
    float scanLine = smoothstep(0.0, 0.06, scanY) * smoothstep(0.18, 0.06, scanY);
    float scanBand = smoothstep(0.0, 1.0, 1.0 - abs(fract(uv.y * 18.0) - 0.5) * 2.0) * 0.15;

    float sweep = smoothstep(0.02, 0.0, abs(fract(time * 0.35) - uv.y));
    float pulse = sin(time * 3.0) * 0.15 + 0.85;

    vec3 baseColor = u_Color.rgb;
    vec3 accentColor = u_PatternColor.rgb;

    vec3 finalRGB = baseColor * (edgeGlow * 1.4)
                  + accentColor * (scanLine * 1.6 + sweep * 0.9)
                  + baseColor * scanBand;
    finalRGB *= pulse;

    float finalAlpha = clamp(u_Color.w * (0.12 + edgeGlow * 0.7 + scanLine * 0.6 + sweep * 0.5), 0.0, 1.0);
    if (finalAlpha < 0.01) discard;
    color = vec4(finalRGB, finalAlpha);
}
