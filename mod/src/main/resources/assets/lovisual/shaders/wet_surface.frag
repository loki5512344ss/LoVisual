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

layout (std140) uniform WetSurface {
    mat4 u_Projection;
    mat4 u_InvView;   // camera-space -> world-space rotation (camera-to-world)
    vec4 u_CameraPos; // world-space camera position, w unused
    vec4 u_Screen;    // invW, invH, time, mainDepthEnabled
    vec4 u_Params;    // wetness, puddleCoverage, reflectionStrength, rippleStrength
    vec4 u_Params2;   // maxDistance, reflectionSteps, rainAmount, ripplesEnabled
    vec4 u_Params3;   // binarySteps, darkeningFactor, unused, unused
};

in vec2 v_TexCoord;

const float DEPTH_NEAR_EPS = 0.000001;
const float DEPTH_FAR_EPS = 0.999999;

bool validRawDepth(float d) {
    return d == d && d > DEPTH_NEAR_EPS && d < DEPTH_FAR_EPS;
}

vec3 viewPositionFromDepth(vec2 uv, float rawDepth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, rawDepth, 1.0);
    vec4 view = inverse(u_Projection) * clip;
    return view.xyz / view.w;
}

bool projectToScreen(vec3 viewPos, out vec2 uv, out float ndcZ) {
    vec4 clip = u_Projection * vec4(viewPos, 1.0);
    if (clip.w <= 0.0001) return false;
    vec3 ndc = clip.xyz / clip.w;
    uv = ndc.xy * 0.5 + 0.5;
    ndcZ = ndc.z;
    return uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 4; i++) {
        v += valueNoise(p) * a;
        p = rot * p * 2.02 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

float puddleNoise(vec2 worldXZ) {
    return valueNoise(worldXZ * 0.18) * 0.65 + valueNoise(worldXZ * 0.45 + 17.0) * 0.35;
}

float getDither(vec2 fragCoord) {
    return fract(52.9829189 * fract(dot(fragCoord, vec2(0.06711056, 0.00583715))));
}

// Реконструкция нормали по 4 соседним сэмплам буфера глубины вместо dFdx/dFdy от view-position:
// screen-space derivative ломается на границах глубины (силуэты, дальний план) и даёт шумную/
// блочную нормаль на почти плоской поверхности -> дрожащая/рваная граница лужи. Сэмплинг соседей
// по глубине даёт гладкую нормаль и не зависит от размера треугольника меша чанка.
vec3 normalFromDepth(vec2 uv, vec3 viewPos) {
    vec2 texel = vec2(u_Screen.x, u_Screen.y);

    float dL = texture(u_MainDepth, clamp(uv - vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dR = texture(u_MainDepth, clamp(uv + vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dD = texture(u_MainDepth, clamp(uv - vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;
    float dU = texture(u_MainDepth, clamp(uv + vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;

    vec3 pL = viewPositionFromDepth(uv - vec2(texel.x, 0.0), dL);
    vec3 pR = viewPositionFromDepth(uv + vec2(texel.x, 0.0), dR);
    vec3 pD = viewPositionFromDepth(uv - vec2(0.0, texel.y), dD);
    vec3 pU = viewPositionFromDepth(uv + vec2(0.0, texel.y), dU);

    vec3 vL = viewPos - pL;
    vec3 vR = pR - viewPos;
    vec3 vD = viewPos - pD;
    vec3 vU = pU - viewPos;

    // берём более короткую (менее вероятно пересекающую разрыв глубины) сторону по каждой оси
    vec3 dX = (abs(vL.z) < abs(vR.z)) ? vL : vR;
    vec3 dY = (abs(vD.z) < abs(vU.z)) ? vD : vU;

    vec3 n = cross(dX, dY);
    float len = length(n);
    if (len > 1e-6) {
        n /= len;
    } else {
        n = normalize(-viewPos);
    }
    if (dot(n, -viewPos) < 0.0) n = -n;
    return n;
}

void main() {
    vec2 uv = v_TexCoord;
    vec4 base = texture(u_Texture, uv);

    if (u_Screen.w < 0.5) {
        color = base;
        return;
    }

    float rawDepth = texture(u_MainDepth, uv).r;
    if (!validRawDepth(rawDepth)) {
        color = base;
        return;
    }

    vec3 viewPos = viewPositionFromDepth(uv, rawDepth);
    vec3 viewNormal = normalFromDepth(uv, viewPos);
    vec3 viewDir = normalize(viewPos);

    vec3 worldNormal = normalize(mat3(u_InvView) * viewNormal);
    float groundFactor = clamp(dot(worldNormal, vec3(0.0, 1.0, 0.0)), 0.0, 1.0);
    groundFactor = smoothstep(0.6, 0.92, groundFactor);
    if (groundFactor <= 0.001) {
        color = base;
        return;
    }

    vec3 worldPos = u_CameraPos.xyz + (mat3(u_InvView) * viewPos);

    float wetness = clamp(u_Params.x, 0.0, 2.0);
    float puddleCoverage = clamp(u_Params.y, 0.0, 1.0);
    float reflectionStrength = clamp(u_Params.z, 0.0, 2.0);
    float rippleStrength = clamp(u_Params.w, 0.0, 2.0);
    float maxDistance = max(u_Params2.x, 1.0);
    int steps = int(clamp(floor(u_Params2.y + 0.5), 1.0, 96.0));
    float rainAmount = clamp(u_Params2.z, 0.0, 2.0);
    bool ripplesEnabled = u_Params2.w > 0.5;
    int binarySteps = int(clamp(floor(u_Params3.x + 0.5), 0.0, 8.0));
    float darkeningFactor = clamp(u_Params3.y, 0.0, 1.0);

    float noise = puddleNoise(worldPos.xz);
    float wetMask = smoothstep(0.5 - puddleCoverage * 0.55, 0.5 + puddleCoverage * 0.35, noise);
    wetMask *= groundFactor;
    if (wetMask <= 0.003) {
        color = base;
        return;
    }

    vec3 reflNormal = worldNormal;
    if (ripplesEnabled && rippleStrength > 0.001) {
        // Аналитический градиент бегущей волны вместо мёртвого dFdx(ripple)*0.0 из старой версии
        // (там перемножение на 0 полностью убивало рябь) — считаем волну в двух соседних точках
        // и берём разность как наклон поверхности, а не саму высоту волны.
        float t = u_Screen.z;
        float eps = 0.35;
        vec2 wp = worldPos.xz;
        float h0 = sin(wp.x * 3.1 + t * 2.4) * 0.5 + sin((wp.x - wp.y * 1.7) * 4.3 - t * 3.1) * 0.5;
        float hx = sin((wp.x + eps) * 3.1 + t * 2.4) * 0.5 + sin(((wp.x + eps) - wp.y * 1.7) * 4.3 - t * 3.1) * 0.5;
        float hy = sin(wp.x * 3.1 + t * 2.4) * 0.5 + sin((wp.x - (wp.y + eps) * 1.7) * 4.3 - t * 3.1) * 0.5;
        vec2 slope = vec2(hx - h0, hy - h0) / eps;
        vec3 perturbedWorld = normalize(vec3(-slope.x, 1.0, -slope.y) * vec3(rippleStrength * 0.35, 1.0, rippleStrength * 0.35));
        reflNormal = normalize(mix(worldNormal, perturbedWorld, groundFactor));
    }
    // world-space нормаль обратно в view-space через транспонированную (обратную) чистую ротацию u_InvView
    vec3 reflNormalView = normalize(transpose(mat3(u_InvView)) * reflNormal);
    if (dot(reflNormalView, -viewDir) < 0.0) reflNormalView = -reflNormalView;

    vec3 reflDir = reflect(viewDir, reflNormalView);

    vec3 skyPitch3 = normalize(mat3(u_InvView) * reflDir);
    float skyPitch = clamp(skyPitch3.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 horizonColor = vec3(0.30, 0.36, 0.45);
    vec3 zenithColor = vec3(0.10, 0.16, 0.28);
    vec3 reflectionColor = mix(horizonColor, zenithColor, pow(skyPitch, 0.75));
    float cloud = fbm(skyPitch3.xz * 1.5 / max(abs(skyPitch3.y) + 0.18, 0.08));
    reflectionColor = mix(reflectionColor, vec3(0.42, 0.48, 0.56), cloud * 0.28 * smoothstep(-0.1, 0.5, skyPitch3.y));
    vec3 lightDir = normalize(vec3(0.35, 0.75, 0.25));
    float sunSpec = pow(max(dot(skyPitch3, lightDir), 0.0), 32.0);
    reflectionColor += vec3(0.85, 0.88, 0.95) * sunSpec * 0.5;

    bool hit = false;
    float dither = getDither(gl_FragCoord.xy);
    float stepLen = maxDistance / float(max(steps, 1));
    vec3 rayOrigin = viewPos + reflNormalView * 0.02;

    for (int i = 0; i < 96; i++) {
        if (i >= steps) break;

        float t = (float(i) + dither) * stepLen + stepLen * 0.5;
        vec3 marchPos = rayOrigin + reflDir * t;

        vec2 sampleUv;
        float sampleNdcZ;
        if (!projectToScreen(marchPos, sampleUv, sampleNdcZ)) break;

        float sceneRawDepth = texture(u_MainDepth, sampleUv).r;
        if (!validRawDepth(sceneRawDepth)) continue;

        vec3 scenePos = viewPositionFromDepth(sampleUv, sceneRawDepth);
        float marchDist = length(marchPos);
        float sceneDist = length(scenePos);
        float thickness = max(0.15, marchDist * 0.03);

        if (marchDist > sceneDist && marchDist - sceneDist < thickness) {
            // Бинарный рефайнмент: сужаем интервал вокруг найденного пересечения, чтобы убрать
            // блочность отражений на резких перепадах глубины (старая версия брала грубый sampleUv как есть).
            float tMin = max(0.0, t - stepLen);
            float tMax = t;
            vec2 refinedUv = sampleUv;
            for (int b = 0; b < binarySteps; b++) {
                float tMid = (tMin + tMax) * 0.5;
                vec3 midPos = rayOrigin + reflDir * tMid;
                vec2 midUv;
                float midNdcZ;
                if (!projectToScreen(midPos, midUv, midNdcZ)) break;
                float midDepth = texture(u_MainDepth, midUv).r;
                if (!validRawDepth(midDepth)) { tMin = tMid; continue; }
                vec3 midScenePos = viewPositionFromDepth(midUv, midDepth);
                if (length(midPos) > length(midScenePos)) {
                    tMax = tMid;
                    refinedUv = midUv;
                } else {
                    tMin = tMid;
                }
            }
            reflectionColor = texture(u_Texture, refinedUv).rgb;
            hit = true;
            break;
        }
    }

    float fresnel = pow(1.0 - clamp(dot(-viewDir, reflNormalView), 0.0, 1.0), 3.0);
    fresnel = mix(0.25, 1.0, fresnel);

    float hitBoost = hit ? 1.0 : 0.55;
    float reflectionAmount = wetMask * reflectionStrength * fresnel * hitBoost
            * mix(0.6, 1.0, clamp(rainAmount, 0.0, 1.0)) * wetness;
    reflectionAmount = clamp(reflectionAmount, 0.0, 1.0);

    vec3 darkened = base.rgb * (1.0 - darkeningFactor * wetMask);
    vec3 result = mix(darkened, reflectionColor, reflectionAmount);

    color = vec4(clamp(result, 0.0, 1.0), 1.0);
}
