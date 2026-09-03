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

#define MAX_ITER 4

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

    // 3D noise from the world ray direction - seamless on the full sphere.
    vec3 p = rayW * scale;
    vec3 i = p;
    float c = 1.0;
    float inten = intensity;

    for (int n = 0; n < MAX_ITER; n++) {
        float t = time * speed * (11.0 - (3.0 / float(n + 1)));
        i = p + vec3(
            cos(t - i.x) + sin(t + i.y),
            sin(t - i.y) + cos(t + i.z),
            cos(t - i.z) + sin(t + i.x)
        );
        c += 1.0 / length(vec3(
            p.x / (sin(i.x + t) / inten),
            p.y / (cos(i.y + t) / inten),
            p.z / (sin(i.z + t) / inten)
        ));
    }

    c /= float(MAX_ITER);
    c = 1.5 - sqrt(c);
    float brightness = c * c * c * c;

    // Softer highlight rolloff and a deeper base tone for a more "liquid" feel.
    vec3 color = baseColor * pow(brightness, 0.85) * 1.15 + baseColor * 0.18;

    float vignette = smoothstep(1.4, 0.15, length(sp));
    color *= mix(0.75, 1.0, vignette);

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
