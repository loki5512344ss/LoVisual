#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

out vec4 color;

uniform sampler2D u_Src;
uniform sampler2D u_Mask;

layout (std140) uniform HandNoise {
    vec4 u_Color;   // rgb, alpha
    vec4 u_Glow;    // rgb, alpha
    vec4 u_Params0; // edgeWidth, quality, octaves, time
    vec4 u_Params1; // resX, resY, scale, speed
    vec4 u_Params2; // contrast, breathe, glowStrength, density
};

in vec2 v_TexCoord;

float handNoiseHash21(vec2 p) {
    p = fract(p * vec2(127.31, 311.7));
    p += dot(p, p + 41.31);
    return fract(p.x * p.y);
}

float handNoiseValue(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = handNoiseHash21(i);
    float b = handNoiseHash21(i + vec2(1.0, 0.0));
    float c = handNoiseHash21(i + vec2(0.0, 1.0));
    float d = handNoiseHash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float handNoiseFbm(vec2 p, int octaves) {
    float v = 0.0;
    float a = 0.55;
    mat2 r = mat2(0.80, 0.60, -0.60, 0.80);
    int o = clamp(octaves, 1, 3);
    for (int i = 0; i < 3; ++i) {
        if (i >= o) break;
        v += a * handNoiseValue(p);
        p = r * p * 2.07 + vec2(23.3, 41.7);
        a *= 0.5;
    }
    return v;
}

float handMaskAt(vec2 uv) {
    vec4 mask = texture(u_Mask, uv);
    return max(mask.a, max(mask.r, max(mask.g, mask.b)));
}

float handNoiseEdgeDistance(vec2 oneTexel, int quality, float edgeWidth, out float hitAmount) {
    float best = 1.0e5;
    float hit = 0.0;
    int rings = clamp(quality, 1, 4);
    int dirs = quality <= 1 ? 8 : 12;
    float radiusPx = max(edgeWidth, 1.0);

    for (int r = 1; r <= 4; ++r) {
        if (r > rings) continue;
        float rr = float(r) / float(rings);
        float distPx = radiusPx * rr;
        for (int i = 0; i < 12; ++i) {
            if (i >= dirs) continue;
            float angle = (float(i) + float(r) * 0.37) * 6.28318530718 / float(dirs);
            vec2 dir = vec2(cos(angle), sin(angle));
            float a = handMaskAt(v_TexCoord + dir * oneTexel * distPx);
            if (a > 0.02) {
                best = min(best, rr);
                hit = max(hit, a);
            }
        }
    }

    hitAmount = hit;
    return best;
}

float handNoiseField(vec2 fragCoord, vec2 resolution, float time, int octaves) {
    float scale = max(u_Params1.z, 0.05);
    float speed = max(u_Params1.w, 0.0);
    float contrast = max(u_Params2.x, 0.05);
    float breathe = clamp(u_Params2.y, 0.0, 1.0);
    float density = max(u_Params2.w, 0.0);

    vec2 uv = fragCoord / max(resolution, vec2(1.0));
    vec2 aspect = vec2(resolution.x / max(resolution.y, 1.0), 1.0);
    vec2 p = (uv - 0.5) * aspect * scale;

    float t = time * speed;
    vec2 warp = vec2(
        handNoiseFbm(p * 1.3 + vec2(0.0, t * 0.32), octaves),
        handNoiseFbm(p * 1.3 + vec2(5.2, -t * 0.26), octaves)
    ) - 0.5;
    float n = handNoiseFbm(p * 1.9 + warp * 1.35 + vec2(t * 0.21, -t * 0.14), octaves);

    float pulse = 0.5 + 0.5 * sin(t * 1.6 + warp.x * 2.4);
    n = clamp(n * (1.0 + breathe * (pulse - 0.5) * 0.9), 0.0, 1.0);
    return pow(clamp(n * (0.85 + density * 0.3), 0.0, 1.0), 1.0 / contrast);
}

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);

    vec2 resolution = max(u_Params1.xy, vec2(1.0));
    vec2 oneTexel = 1.0 / resolution;
    float edgeWidth = max(u_Params0.x, 0.0);
    int quality = int(u_Params0.y + 0.5);
    int octaves = clamp(int(u_Params0.z + 0.5), 1, 3);
    float time = u_Params0.w;
    float glowStrength = max(u_Params2.z, 0.0);

    if (mask > 0.01) {
        float n = handNoiseField(gl_FragCoord.xy, resolution, time, octaves);

        float nL = handMaskAt(v_TexCoord + vec2(-oneTexel.x, 0.0));
        float nR = handMaskAt(v_TexCoord + vec2(oneTexel.x, 0.0));
        float nU = handMaskAt(v_TexCoord + vec2(0.0, -oneTexel.y));
        float nD = handMaskAt(v_TexCoord + vec2(0.0, oneTexel.y));
        float innerEdge = clamp(1.0 - min(min(nL, nR), min(nU, nD)), 0.0, 1.0);
        innerEdge = pow(innerEdge, 0.72);

        float alpha = clamp(u_Color.a * mask, 0.0, 1.0);
        vec3 shade = u_Color.rgb * (0.55 + 0.75 * n);
        shade = mix(shade, u_Glow.rgb, clamp((n - 0.68) * 1.4, 0.0, 0.35));
        vec3 outCol = mix(base.rgb, shade, alpha);
        outCol += u_Glow.rgb * innerEdge * u_Glow.a * glowStrength * (0.18 + 0.30 * n);
        color = vec4(clamp(outCol, 0.0, 1.0), 1.0);
        return;
    }

    float hitAmount = 0.0;
    float d = handNoiseEdgeDistance(oneTexel, quality, edgeWidth, hitAmount);
    if (hitAmount > 0.01) {
        float n = handNoiseField(gl_FragCoord.xy, resolution, time, octaves);
        float falloff = clamp(1.0 - d, 0.0, 1.0);
        falloff = falloff * falloff * (3.0 - 2.0 * falloff);
        float glowA = pow(falloff, 1.3) * u_Glow.a * glowStrength * (0.30 + 0.40 * n);
        vec3 outCol = mix(base.rgb, u_Glow.rgb, glowA * 0.5);
        outCol += u_Glow.rgb * glowA;
        color = vec4(clamp(outCol, 0.0, 1.0), 1.0);
        return;
    }

    color = vec4(base.rgb, 1.0);
}
