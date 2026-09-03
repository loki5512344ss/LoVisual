#version 330 core

/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
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

bool projectToScreen(vec3 viewPos, out vec2 uv, out float rawDepth) {
    vec4 clip = u_Projection * vec4(viewPos, 1.0);
    if (clip.w <= 0.0001) return false;
    vec3 ndc = clip.xyz / clip.w;
    uv = ndc.xy * 0.5 + 0.5;
    rawDepth = ndc.z;
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

float puddleNoise(vec2 worldXZ) {
    float n = valueNoise(worldXZ * 0.18) * 0.65
            + valueNoise(worldXZ * 0.45 + 17.0) * 0.35;
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

    vec3 ddxPos = dFdx(viewPos);
    vec3 ddyPos = dFdy(viewPos);
    vec3 viewNormal = normalize(cross(ddxPos, ddyPos));
    vec3 viewDir = normalize(viewPos);
    if (dot(viewNormal, -viewDir) < 0.0) {
        viewNormal = -viewNormal;
    }

    vec3 worldNormal = normalize(mat3(u_InvView) * viewNormal);
    float groundFactor = clamp(dot(worldNormal, vec3(0.0, 1.0, 0.0)), 0.0, 1.0);
    groundFactor = smoothstep(0.82, 0.98, groundFactor);
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
    int steps = int(clamp(floor(u_Params2.y + 0.5), 1.0, 32.0));
    float rainAmount = clamp(u_Params2.z, 0.0, 2.0);
    bool ripplesEnabled = u_Params2.w > 0.5;

    float noise = puddleNoise(worldPos.xz);
    float wetMask = smoothstep(0.5 - puddleCoverage * 0.55, 0.5 + puddleCoverage * 0.35, noise);
    wetMask *= groundFactor;
    if (wetMask <= 0.003) {
        color = base;
        return;
    }

    vec3 reflNormal = viewNormal;
    if (ripplesEnabled && rippleStrength > 0.001) {
        float t = u_Screen.z;
        float ripple = sin((worldPos.x + worldPos.z) * 3.1 + t * 2.4) * 0.5
                + sin((worldPos.x - worldPos.z * 1.7) * 4.3 - t * 3.1) * 0.5;
        vec2 perturb = vec2(dFdx(ripple), dFdy(ripple)) * 0.0;
        reflNormal = normalize(viewNormal + vec3(ripple, 0.0, ripple * 0.6) * rippleStrength * 0.05);
    }

    vec3 reflDir = reflect(viewDir, reflNormal);

    vec3 reflectionColor = mix(vec3(0.45, 0.52, 0.62), vec3(0.75, 0.82, 0.92), clamp(reflDir.y * 0.5 + 0.5, 0.0, 1.0));
    bool hit = false;

    float stepLen = maxDistance / float(steps);
    vec3 marchPos = viewPos + reflNormal * 0.02;
    for (int i = 0; i < 32; i++) {
        if (i >= steps) break;

        marchPos += reflDir * stepLen;

        vec2 sampleUv;
        float sampleClipDepth;
        if (!projectToScreen(marchPos, sampleUv, sampleClipDepth)) break;

        float sceneRawDepth = texture(u_MainDepth, sampleUv).r;
        if (!validRawDepth(sceneRawDepth)) continue;

        vec3 scenePos = viewPositionFromDepth(sampleUv, sceneRawDepth);

        float marchDist = length(marchPos);
        float sceneDist = length(scenePos);
        float thickness = max(0.15, marchDist * 0.03);

        if (marchDist > sceneDist && marchDist - sceneDist < thickness) {
            reflectionColor = texture(u_Texture, sampleUv).rgb;
            hit = true;
            break;
        }
    }

    float fresnel = pow(1.0 - clamp(dot(-viewDir, viewNormal), 0.0, 1.0), 3.0);
    fresnel = mix(0.25, 1.0, fresnel);

    float hitBoost = hit ? 1.0 : 0.55;
    float reflectionAmount = wetMask * reflectionStrength * fresnel * hitBoost
            * mix(0.6, 1.0, clamp(rainAmount, 0.0, 1.0)) * wetness;
    reflectionAmount = clamp(reflectionAmount, 0.0, 1.0);

    vec3 darkened = base.rgb * mix(1.0, 0.82, wetMask * clamp(wetness, 0.0, 1.0));
    vec3 result = mix(darkened, reflectionColor, reflectionAmount);

    color = vec4(clamp(result, 0.0, 1.0), 1.0);
}
