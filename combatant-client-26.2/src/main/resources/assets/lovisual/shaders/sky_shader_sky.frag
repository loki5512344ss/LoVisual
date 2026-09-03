#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on, adapted from, or implemented
 * with reference to a "SkyShader" module distributed for another
 * Minecraft client. The original attribution was not preserved.
 * This implementation has since been substantially modified for LoVisual.
 *
 * Licensed under the GNU General Public License v3.0.
 */

out vec4 fragColor;

layout (std140) uniform SkyShader {
    vec4 u_Params0; // x = time, y = speed, z = scale, w = intensity
    vec4 u_Params1; // x = resWidth, y = resHeight, z = alpha, w = fov
    vec4 u_Color;   // rgb = base color
    vec4 u_CameraDir; // x = -yawRad, y = pitchRad
};

in vec2 v_TexCoord;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}
mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float wave(vec3 dir, float phase, float scale, float speed, float time) {
    return sin(dir.x * scale + time * speed + phase)
         * sin(dir.z * scale * 0.7 - time * speed * 0.8 + phase * 1.7)
         * sin(dir.y * scale * 1.3 + time * speed * 0.5 + phase * 2.3);
}

void main() {
    float time = u_Params0.x;
    float speed = u_Params0.y;
    float scale = u_Params0.z;
    float intensity = u_Params0.w;

    vec2 res = u_Params1.xy;
    float alpha = u_Params1.z;
    float fov = u_Params1.w;

    vec3 baseColor = u_Color.rgb;

    vec2 sp = v_TexCoord * 2.0 - 1.0;
    float aspect = res.x / max(res.y, 1.0);

    float tanV = tan(radians(fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(u_CameraDir.x) * rotX(u_CameraDir.y) * rayV;

    float h = clamp(rayW.y, -1.0, 1.0);

    // Vertical atmosphere gradient with a warmer, higher-contrast horizon glow.
    vec3 zenith  = baseColor * 0.12;
    vec3 horizon = baseColor * 0.95 + vec3(0.05, 0.03, 0.01);
    vec3 base = mix(horizon, zenith, pow(clamp(h, 0.0, 1.0), 0.55));
    base += baseColor * 0.4 * exp(-abs(h) * 4.5);

    // Flowing aurora bands, seamless on the full sphere (world-space direction).
    float bands = 0.0;
    bands += wave(rayW, 0.0, scale, speed, time);
    bands += wave(rayW * 1.9 + 13.7, 2.1, scale, speed, time) * 0.55;
    bands += wave(rayW * 3.7 + 41.3, 4.7, scale, speed, time) * 0.3;
    bands *= intensity * 40.0;

    // Fade aurora towards the horizon so it hugs the upper sky, softened edge.
    float auroraMask = smoothstep(-0.1, 0.5, h);
    vec3 aurora = baseColor * bands * auroraMask;

    // Subtle star shimmer near the zenith, gently pulsing.
    float star = pow(max(sin(rayW.x * 220.0) * sin(rayW.y * 260.0) * sin(rayW.z * 240.0), 0.0), 26.0);
    star *= smoothstep(0.2, 0.85, h) * (0.55 + 0.45 * sin(time * speed * 2.4));

    vec3 color = base + aurora + vec3(star) * (0.6 + 0.4 * baseColor);

    // Gentle vignette so the effect reads as sky rather than a flat overlay.
    float vignette = smoothstep(1.35, 0.2, length(sp));
    color *= mix(0.82, 1.0, vignette);

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
