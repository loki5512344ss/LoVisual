#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 */

out vec4 color;

layout (std140) uniform SkyShaderStarry {
    vec4 u_Resolution;  // xy = size, z = time, w = speed
    vec4 u_Settings;    // x = intensity, y = alpha, z = FOV, w = starDensity
    vec4 u_CameraDir;   // orientation quaternion
    vec4 u_Moon;        // xyz = world-space moon direction, w = angular radius
    vec4 u_MoonExtra;   // x = moon phase (0..1), y = milky way intensity, zw = spare
    vec4 u_MoonColor;
    vec4 u_StarColor;
};

float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

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
    for (int k = 0; k < 4; k++) {
        v += a * smoothNoise2D(p * freq);
        freq *= 2.05;
        a *= 0.5;
    }
    return v;
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

float starField(vec3 dir, float time, float density) {
    vec3 grid = dir * density;
    vec3 cell = floor(grid);
    float h = hash13(cell);
    float exists = step(0.986, h);
    float twinkle = 0.6 + 0.4 * sin(time * (2.0 + h * 6.0) + h * 43.0);
    float brightness = fract(h * 91.7) * 0.7 + 0.3;
    return exists * twinkle * brightness;
}

float milkyWayBand(vec3 dir, float time) {
    vec3 axis = normalize(vec3(0.35, 0.12, 0.93));
    float t = dot(dir, axis);
    float band = exp(-t * t * 7.0);
    vec2 texCoord = vec2(dir.x - dir.z, dir.y) * 2.5 + time * 0.005;
    float texture = fbm(texCoord) * 0.6 + fbm(texCoord * 2.3 + 5.0) * 0.4;
    return band * (0.55 + 0.45 * texture);
}

vec3 basisFromDir(vec3 dir, out vec3 right, out vec3 up) {
    vec3 worldUp = abs(dir.y) > 0.99 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    right = normalize(cross(worldUp, dir));
    up = normalize(cross(dir, right));
    return dir;
}

float moonDisc(vec3 dir, out float lit) {
    vec3 moonDir = normalize(u_Moon.xyz);
    float angularRadius = max(0.002, u_Moon.w);

    vec3 right, up;
    basisFromDir(moonDir, right, up);

    vec3 diff = dir - moonDir;
    vec2 p = vec2(dot(diff, right), dot(diff, up)) / angularRadius;

    float edge = 0.06;
    float distMoon = length(p);
    float moonMask = 1.0 - smoothstep(1.0 - edge, 1.0 + edge, distMoon);
    if (moonMask <= 0.001) {
        lit = 0.0;
        return 0.0;
    }

    float phaseAngle = fract(u_MoonExtra.x) * 6.2831853;
    float side = sin(phaseAngle) >= 0.0 ? 1.0 : -1.0;
    float shadowX = side * (1.0 - cos(phaseAngle));

    float distShadow = length(p - vec2(shadowX, 0.0));
    float shadowMask = 1.0 - smoothstep(1.0 - edge, 1.0 + edge, distShadow);

    float limbDarkening = 1.0 - 0.25 * distMoon * distMoon;
    float litArea = moonMask * (1.0 - shadowMask) * limbDarkening;
    float earthshine = moonMask * shadowMask * 0.10;

    lit = litArea + earthshine;
    return moonMask;
}

void main() {
    vec3 dir = normalize(calcRayDir());
    float time = u_Resolution.z;
    float speed = u_Resolution.w;
    float anim = time * speed;

    float horizonFade = smoothstep(-0.05, 0.12, dir.y);
    if (horizonFade < 0.005) discard;

    float intensity = max(0.0001, u_Settings.x);
    float alpha = u_Settings.y;
    float starDensity = max(1.0, u_Settings.w);
    float milkyWayIntensity = u_MoonExtra.y;

    vec3 skyBase = vec3(0.015, 0.02, 0.05);

    float stars = starField(dir, anim, starDensity);
    float milkyWay = milkyWayBand(dir, anim) * milkyWayIntensity;

    float moonLit;
    float moonMask = moonDisc(dir, moonLit);

    vec3 skyColor = skyBase;
    skyColor += u_StarColor.rgb * stars * intensity;
    skyColor += mix(u_StarColor.rgb, vec3(0.75, 0.8, 1.0), 0.4) * milkyWay * intensity;
    skyColor = mix(skyColor, u_MoonColor.rgb * (0.4 + 0.6 * moonLit), moonMask);
    skyColor += u_MoonColor.rgb * moonLit * 0.6 * intensity;

    float finalAlpha = alpha * horizonFade;
    color = vec4(clamp(skyColor, 0.0, 1.0), finalAlpha);
}
