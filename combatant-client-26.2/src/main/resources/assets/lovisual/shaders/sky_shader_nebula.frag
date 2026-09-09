#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 */

out vec4 color;

layout (std140) uniform SkyShaderData {
    vec4 u_Resolution;  // xy = size, z = time, w = speed
    vec4 u_Settings;    // x = intensity, y = alpha, z = FOV, w = spare
    vec4 u_CameraDir;   // orientation quaternion
    vec4 u_SkyZenith;
    vec4 u_SkyHorizon;
    vec4 u_NebColor1;
    vec4 u_NebColor2;
    vec4 u_StarColor;
    vec4 u_FogColor;
    vec4 u_FogRange;
};

float hash21(vec2 p) {
    p = fract(p * vec2(443.8975, 397.2973));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float smoothNoise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5, freq = 1.0;
    for (int k = 0; k < 5; k++) {
        v += a * smoothNoise2D(p * freq);
        freq *= 2.1;
        a *= 0.48;
    }
    return v;
}

vec2 warp(vec2 p, float t) {
    vec2 q = vec2(fbm(p + vec2(0.0, 0.0) + t * 0.12),
                  fbm(p + vec2(5.2, 1.3) + t * 0.09));
    vec2 r = vec2(fbm(p + 4.0 * q + vec2(1.7, 9.2) + t * 0.06),
                  fbm(p + 4.0 * q + vec2(8.3, 2.8) + t * 0.07));
    return p + 1.8 * r;
}

vec3 calcRayDir() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = u_Resolution.x / u_Resolution.y;
    float tanV = tan(radians(u_Settings.z) * 0.5);
    vec3 ray = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, -1.0));

    vec3 qv = u_CameraDir.xyz;
    float qw = u_CameraDir.w;
    mat3 rot = mat3(
        1.0 - 2.0*(qv.y*qv.y + qv.z*qv.z), 2.0*(qv.x*qv.y + qv.z*qw), 2.0*(qv.x*qv.z - qv.y*qw),
        2.0*(qv.x*qv.y - qv.z*qw), 1.0 - 2.0*(qv.x*qv.x + qv.z*qv.z), 2.0*(qv.y*qv.z + qv.x*qw),
        2.0*(qv.x*qv.z + qv.y*qw), 2.0*(qv.y*qv.z - qv.x*qw), 1.0 - 2.0*(qv.x*qv.x + qv.y*qv.y)
    );
    return rot * ray;
}

void main() {
    vec3 dir = normalize(calcRayDir());
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    float time = u_Resolution.z;
    float speed = u_Resolution.w;
    float anim = time * speed;

    float up = clamp(dir.y, 0.0, 1.0);
    float horizonFade = smoothstep(-0.15, 0.05, dir.y);
    if (horizonFade < 0.005) discard;

    float intensity = u_Settings.x;
    float alpha = u_Settings.y;

    vec3 drift = vec3(0.0, anim * 0.025, anim * 0.04);

    vec2 p = dir.xz * 1.0 + drift.xz * 0.9;
    p.y += dir.y * 0.2;

    float t = anim * 0.045;
    vec2 wp = warp(p, t);

    float n1 = fbm(wp * 0.9 + vec2(0.0, 0.0));
    float n2 = fbm(wp * 1.5 + vec2(2.7, 4.3));
    float n3 = fbm(wp * 0.5 + vec2(-1.8, 3.1));
    float n4 = fbm(wp * 2.2 + vec2(-3.0, 1.2));

    float juice = (n1 * 0.4 + n2 * 0.25 + n3 * 0.20 + n4 * 0.15);
    juice = smoothstep(0.15, 0.85, juice);

    float heightMask = 0.7 + 0.3 * smoothstep(0.0, 0.6, up);
    float flicker = 0.8 + 0.2 * sin(anim * 0.5 + dir.x * 14.0 + dir.z * 10.0);
    flicker *= 0.9 + 0.1 * sin(anim * 0.9 + dir.y * 18.0);
    juice = juice * heightMask * flicker;

    float glitter = 0.0;
    for (int i = 0; i < 3; i++) {
        float seed = float(i) * 2.13;
        vec2 pos = vec2(
            0.5 + 0.8 * sin(seed * 1.7 + anim * 0.2 + p.x * 0.5),
            0.5 + 0.8 * cos(seed * 2.3 + anim * 0.15 + p.y * 0.5)
        );
        float d = length(uv - pos);
        float sz = 0.01 + 0.02 * sin(seed * 3.1 + anim * 0.3);
        glitter += exp(-d * d / (2.0 * sz * sz)) * 0.6;
    }
    glitter *= (0.7 + 0.3 * sin(anim * 0.6 + p.x * 5.0));

    float mix1 = 0.5 + 0.5 * sin(anim * 0.06 + p.x * 1.2 + p.y * 0.8 + juice * 1.5);
    float mix2 = 0.5 + 0.5 * cos(anim * 0.08 + p.x * 0.9 - p.y * 1.1 + juice * 2.0);
    vec3 color1 = mix(u_NebColor1.rgb, u_NebColor2.rgb, mix1);
    vec3 color2 = mix(u_NebColor2.rgb, vec3(1.0, 0.9, 0.8), 0.3);
    vec3 nebColor = mix(color1, color2, mix2);

    float hot = exp(-abs(juice - 0.4) * 6.0) * 0.6;
    nebColor += vec3(1.0, 0.95, 0.7) * hot;

    nebColor += u_StarColor.rgb * glitter * 0.8;

    float bloom = exp(-juice * 3.0) * 0.4;
    nebColor += vec3(0.9, 0.8, 1.0) * bloom * 0.5;

    float finalIntensity = juice * 1.3 + hot * 0.3 + glitter * 0.4;
    vec3 finalColor = nebColor * finalIntensity * intensity;

    float finalAlpha = clamp(finalIntensity * 1.5, 0.0, 1.0) * alpha * horizonFade;
    float vignette = 1.0 - length(dir.xz) * 0.15;
    finalAlpha *= clamp(vignette, 0.0, 1.0);

    if (finalAlpha < 0.01) discard;
    color = vec4(clamp(finalColor, 0.0, 1.0), finalAlpha);
}
