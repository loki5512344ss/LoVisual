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
    vec2 vUV = v_TexCoord;
    float time = u_Time;

    float distToEdge = min(min(vUV.x, 1.0 - vUV.x), min(vUV.y, 1.0 - vUV.y));
    float edgeGlow = exp(-distToEdge * 16.0);
    float edgeFrame = smoothstep(0.03, 0.0, distToEdge);

    vec2 gridUV = vUV * 6.0;
    float gridAnim = sin(gridUV.x + gridUV.y + time * 2.0) * 0.5 + 0.5;
    vec2 f = abs(fract(gridUV - 0.5) - 0.5);
    float gridLines = smoothstep(0.05, 0.0, min(f.x, f.y));
    gridLines *= (0.2 + 0.8 * gridAnim);

    vec2 p = vUV * 2.0 - 1.0;
    float rLen = length(p);
    float wave = sin(rLen * 10.0 - time * 3.5) * 0.5 + 0.5;
    wave *= smoothstep(0.8, 0.1, rLen);

    float pulse = sin(time * 2.5) * 0.12 + 0.88;

    vec3 baseColor = u_Color.rgb;
    vec3 accentColor = u_PatternColor.rgb;
    vec3 borderRGB = baseColor * (edgeGlow * 1.6 + edgeFrame * 2.2);
    vec3 gridRGB = mix(baseColor, accentColor, gridAnim) * (gridLines * 0.9);
    vec3 waveRGB = accentColor * (wave * 0.35);
    vec3 finalRGB = (borderRGB + gridRGB + waveRGB) * pulse;

    float finalAlpha = clamp(u_Color.w * (0.15 + edgeGlow * 0.85 + gridLines * 0.5 + wave * 0.2), 0.0, 1.0);
    color = vec4(finalRGB, finalAlpha);
}
