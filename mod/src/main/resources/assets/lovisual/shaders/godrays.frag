#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

out vec4 color;

uniform sampler2D u_Texture;
uniform sampler2D u_MainDepth;

layout (std140) uniform GodRays {
    vec4 u_Sun;       // sunUv.xy, reach, reserved
    vec4 u_Controls;  // samples, sampleWeight, strength, decay
    vec4 u_Tint;      // rgb tint, fade
    vec4 u_Screen;    // invW, invH, width, height
};

in vec2 v_TexCoord;

const float SKY_LOW = 0.00001;
const float SKY_HIGH = 0.99999;
const float MIN_STEP = 1.0e-5;
const float SUN_RADIUS = 0.30;
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float interleavedGradient(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

float skyMask(vec2 uv) {
    float depth = texture(u_MainDepth, uv).r;
    return step(depth, SKY_LOW) + step(SKY_HIGH, depth);
}

float sunProximity(vec2 uv, float aspect) {
    vec2 offset = (uv - u_Sun.xy) * vec2(aspect, 1.0);
    return 1.0 - smoothstep(0.0, SUN_RADIUS, length(offset));
}

void main() {
    vec4 scene = texture(u_Texture, v_TexCoord);
    int samples = int(u_Controls.x);
    float aspect = u_Screen.z / max(u_Screen.w, 1.0);

    vec2 delta = (v_TexCoord - u_Sun.xy) * (u_Sun.z / float(samples));
    if (dot(delta, delta) < MIN_STEP * MIN_STEP) {
        color = vec4(scene.rgb, 1.0);
        return;
    }

    vec2 uv = v_TexCoord - delta * interleavedGradient(gl_FragCoord.xy);
    float illumination = 1.0;
    float accum = 0.0;
    for (int i = 0; i < samples; i++) {
        uv -= delta;
        vec2 clamped = clamp(uv, vec2(0.0), vec2(1.0));
        float emission = dot(texture(u_Texture, clamped).rgb, LUMA)
                * skyMask(clamped) * sunProximity(clamped, aspect);
        accum += emission * illumination * u_Controls.y;
        illumination *= u_Controls.w;
    }
    accum /= float(samples);

    vec3 rays = vec3(accum) * u_Tint.rgb * u_Controls.z * u_Tint.w;
    color = vec4(scene.rgb + rays, 1.0);
}
