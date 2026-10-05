#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

// Animated theme backdrop inside a rounded rect. Payload (shared with the smoke batch):
//   v_Color.rgb / v_CornerExtentX.rgb / v_CornerExtentY.rgb = the three accent colours, v_Color.a = opacity
//   v_CornerModes = radius, softness, mode (1..6), intensity
//   v_EdgeModes   = time (seconds), pattern scale, speed, 0
//   v_EdgeData    = wipe origin (fraction of the rect), wipe progress, wipe kind (0 none, 1 reveal, 2 erase)

in vec4 v_Local;
in vec4 v_Color;
in vec4 v_Rect;
in vec4 v_CornerModes;
in vec4 v_CornerExtentX;
in vec4 v_CornerExtentY;
in vec4 v_EdgeModes;
in vec4 v_EdgeData;

out vec4 fragColor;

layout (std140) uniform UIBatch {
    vec4 uScreen; // xy = framebuffer size, zw = logical size
};

const float TAU = 6.2831853;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += valueNoise(p) * amp;
        p = p * 2.03 + vec2(17.1, 9.7);
        amp *= 0.5;
    }
    return sum;
}

float roundedBoxSDF(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

vec2 warpedLocal(vec4 local) {
    float invW = abs(local.z) > 0.000001 ? local.z : 1.0;
    return local.xy / invW;
}

vec3 accentAt(float position) {
    float t = clamp(position, 0.0, 1.0);
    if (t <= 0.5) return mix(v_Color.rgb, v_CornerExtentX.rgb, t * 2.0);
    return mix(v_CornerExtentX.rgb, v_CornerExtentY.rgb, (t - 0.5) * 2.0);
}

void iridescence(vec2 uv, float time, out vec3 color, out float weight) {
    vec2 warp = vec2(fbm(uv * 2.1 + vec2(time * 0.07, 0.0)), fbm(uv * 2.1 + vec2(5.2, 1.3) - vec2(0.0, time * 0.07)));
    float film = fbm(uv * 1.55 + warp * 1.15);
    float phase = film * 2.6 + dot(uv, vec2(0.62, 0.34)) + time * 0.12;
    float bands = 0.5 + 0.5 * cos(TAU * phase);
    color = accentAt(bands) + vec3(pow(bands, 14.0) * 0.18);
    weight = 0.04 + 0.96 * bands * bands * bands;
}

void magma(vec2 uv, float time, out vec3 color, out float weight) {
    vec2 field = uv * 2.6 + vec2(0.0, -time * 0.12);
    vec2 flow = vec2(fbm(field + vec2(time * 0.09, 0.0)), fbm(field + vec2(3.7, 8.1) - vec2(time * 0.09, 0.0)));
    float crust = fbm(field + flow * 1.35);
    float ridge = pow(clamp(1.0 - abs(crust * 2.0 - 1.0), 0.0, 1.0), 5.0);
    color = accentAt(ridge * 1.4);
    weight = pow(ridge, 1.15) * 0.95;
}

void digitalRain(vec2 uv, float time, out vec3 color, out float weight) {
    vec2 cell = uv * vec2(44.0, 27.0);
    float column = floor(cell.x);
    float seed = hash12(vec2(column, 17.3));
    float speed = 3.0 + seed * 7.0;
    float stream = cell.y + time * speed + seed * 23.0;
    float trail = fract(stream / 9.0);
    float glow = pow(trail, 3.0);
    float head = smoothstep(0.86, 1.0, trail);
    float lit = step(0.30, hash12(vec2(column, floor(stream) + floor(time * 7.0))));
    float alive = step(0.22, hash12(vec2(column, 91.7)));
    color = accentAt(glow) + vec3(head * 0.55);
    weight = glow * lit * alive * 0.95;
}

void gridHorizon(vec2 uv, float time, out vec3 color, out float weight) {
    vec2 p = vec2(uv.x, -uv.y);
    float depth = -0.06 - p.y;
    vec2 toSun = vec2(p.x, p.y + 0.16);
    float reach = length(toSun);
    float disc = 1.0 - smoothstep(0.28, 0.30, reach);
    float slats = step(0.42, fract(toSun.y * 26.0));
    float carved = mix(1.0, slats, smoothstep(0.15, 0.30, toSun.y));
    float halo = exp(-max(reach - 0.30, 0.0) * 9.0) * 0.30;
    if (depth > 0.008) {
        float perspective = 0.35 / depth;
        vec2 lane = vec2(p.x * perspective * 6.0, perspective * 1.6 + time * 0.55);
        vec2 blur = max(fwidth(lane), vec2(0.004));
        vec2 span = abs(fract(lane) - 0.5);
        vec2 rails = smoothstep(0.06 + blur, 0.06 - blur, span);
        float fade = smoothstep(0.0, 0.20, depth);
        color = accentAt(1.0 - fade);
        weight = max(rails.x, rails.y) * fade * 0.85;
        return;
    }
    color = accentAt(0.5 - toSun.y / 0.30 * 0.5);
    weight = max(disc * carved, halo) * 0.85;
}

void singularity(vec2 uv, float time, out vec3 color, out float weight) {
    float radius = max(length(uv), 0.05);
    float swirl = atan(uv.y, uv.x) + 2.4 / radius - time * 0.9;
    float arms = 0.5 + 0.5 * cos(swirl * 3.0);
    float turbulence = fbm(vec2(cos(swirl), sin(swirl)) * radius * 3.0 + 4.0);
    float body = pow(arms, 2.2) * mix(1.0, turbulence * 2.0, 0.55);
    float disc = smoothstep(0.14, 0.42, radius) * (1.0 - smoothstep(0.95, 1.23, radius));
    float offset = (radius - 0.14) / 0.05;
    float ring = exp(-offset * offset) * 0.90;
    color = accentAt(body + ring);
    weight = (body * disc + ring) * 0.90;
}

void ember(vec2 uv, float time, out vec3 color, out float weight) {
    vec2 field = uv * 1.2;
    float t = time * 0.35;
    for (int i = 1; i <= 6; i++) {
        float order = float(i);
        field.x += 0.5 / order * cos(t + order * 1.5 * field.y);
        field.y += 0.5 / order * cos(t + order * field.x);
    }
    float heat = clamp(0.5 + 0.5 * sin(field.x * 2.1) * cos(field.y * 2.1) + fbm(field * 1.7) * 0.35 - 0.15, 0.0, 1.0);
    float bloom = exp(-dot(uv, uv) * 1.6);
    float level = clamp(heat * 0.85 + bloom * 0.35, 0.0, 1.0);
    color = mix(accentAt(level * 0.8), vec3(1.0), smoothstep(0.82, 1.0, level) * 0.5);
    weight = 0.25 + 0.75 * smoothstep(0.15, 0.85, level);
}

void main() {
    vec2 logicalScale = uScreen.zw / uScreen.xy;
    vec2 frag = warpedLocal(v_Local);

    vec2 halfSize = v_Rect.zw * 0.5;
    vec2 center = v_Rect.xy + halfSize;
    float radius = clamp(v_CornerModes.x, 0.0, min(halfSize.x, halfSize.y));
    float aa = max(max(logicalScale.x, logicalScale.y), 0.0001) + max(v_CornerModes.y, 0.0);
    float shape = clamp(0.5 - roundedBoxSDF(frag - center, halfSize, radius) / aa, 0.0, 1.0);
    float intensity = max(v_CornerModes.w, 0.0);
    if (shape <= 0.001 || intensity <= 0.0) discard;

    float mode = v_CornerModes.z;
    float time = v_EdgeModes.x * v_EdgeModes.z;
    vec2 local = frag - v_Rect.xy;
    float shortSide = max(min(v_Rect.z, v_Rect.w), 1.0);
    vec2 uv = (local / shortSide - 0.5 * v_Rect.zw / shortSide) * max(v_EdgeModes.y, 0.1);

    vec3 color = vec3(0.0);
    float weight = 0.0;
    if (mode < 1.5) iridescence(uv, time, color, weight);
    else if (mode < 2.5) magma(uv, time, color, weight);
    else if (mode < 3.5) digitalRain(uv, time, color, weight);
    else if (mode < 4.5) gridHorizon(uv, time, color, weight);
    else if (mode < 5.5) singularity(uv, time, color, weight);
    else ember(uv, time, color, weight);

    vec2 offset = abs(local / v_Rect.zw - 0.5) * 2.0;
    float edge = mix(0.22, 1.0, smoothstep(0.18, 1.0, max(offset.x, offset.y)));
    float wipeKind = v_EdgeData.w;
    if (wipeKind > 0.5) {
        vec2 origin = v_Rect.xy + v_EdgeData.xy * v_Rect.zw;
        float reach = length(max(origin - v_Rect.xy, v_Rect.xy + v_Rect.zw - origin)) + 24.0;
        float front = reach * v_EdgeData.z;
        float inside = 1.0 - smoothstep(front - 24.0, front, distance(frag, origin));
        weight *= wipeKind < 1.5 ? inside : 1.0 - inside;
    }
    float alpha = v_Color.a * shape * clamp(intensity * weight * edge, 0.0, 1.0);
    if (alpha < 0.004) discard;
    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
