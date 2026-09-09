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

float hexDist(vec2 p) {
    p = abs(p);
    float c = dot(p, normalize(vec2(1.0, 1.73205)));
    c = max(c, p.x);
    return c;
}

vec2 hexCoords(vec2 uv) {
    vec2 r = vec2(1.0, 1.73205);
    vec2 h = r * 0.5;
    vec2 a = mod(uv, r) - h;
    vec2 b = mod(uv - h, r) - h;
    return dot(a, a) < dot(b, b) ? a : b;
}

void main() {
    vec2 uv = v_TexCoord;
    float time = u_Time;

    float distToEdge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeFrame = smoothstep(0.02, 0.0, distToEdge);
    float edgeGlow = exp(-distToEdge * 14.0);

    vec2 hexUv = uv * 8.0;
    vec2 cell = hexCoords(hexUv);
    float hex = hexDist(cell);
    float hexLine = smoothstep(0.5, 0.46, hex) - smoothstep(0.46, 0.40, hex);

    float flicker = 0.85 + 0.15 * sin(time * 6.0 + uv.x * 20.0);
    float rise = fract(uv.y * 2.0 - time * 0.5);
    float riseBand = smoothstep(0.0, 0.08, rise) * smoothstep(0.24, 0.08, rise);

    vec3 baseColor = u_Color.rgb;
    vec3 accentColor = u_PatternColor.rgb;

    vec3 finalRGB = baseColor * (edgeFrame * 2.0 + edgeGlow * 1.2)
                  + accentColor * hexLine * flicker
                  + accentColor * riseBand * 0.8;

    float finalAlpha = clamp(u_Color.w * (0.10 + edgeFrame * 0.9 + hexLine * 0.6 + riseBand * 0.4), 0.0, 1.0);
    if (finalAlpha < 0.01) discard;
    color = vec4(finalRGB, finalAlpha);
}
