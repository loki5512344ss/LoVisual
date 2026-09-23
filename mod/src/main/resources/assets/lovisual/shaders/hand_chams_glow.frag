#version 330 core

in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D u_Src;
uniform sampler2D u_Mask;

layout (std140) uniform HandChams {
    vec4 u_View;   // x = resX, y = resY, z = time
    vec4 u_Mix;    // x = effectAlpha, y = chromaSpeed, z = chromaSaturation, w = chromaBrightness
    vec4 u_ColorA; // rgb = colorA
    vec4 u_ColorB; // rgb = colorB
    vec4 u_ColorC; // rgb = colorC
    vec4 u_ColorD; // rgb = colorD
    vec4 u_Common; // x = snowIntensity, y = snowSpeed, z = stripesWidth, w = stripesSpeed
    vec4 u_Glow;   // x = fillGlow, y = glowRadius, z = glowPower, w = glowDispersion
    vec4 u_Flags;  // x = hideHand
};

float handMaskAt(vec2 uv) {
    vec4 m = texture(u_Mask, uv);
    return max(m.a, max(m.r, max(m.g, m.b)));
}

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);
    if (mask <= 0.02 && u_Glow.w <= 1.0) {
        fragColor = base;
        return;
    }

    float time = u_View.z;
    float maskGain = smoothstep(0.02, 0.3, mask);
    vec2 texelSize = 1.0 / max(u_View.xy, vec2(1.0));

    // Маска: 0 = фон, >0 = рука. Инвертируем в depth-семантику (1 = фон).
    float centerDepth = 1.0 - mask;

    float edgeStrength = 0.0;
    float outwardGlow = 0.0;

    int disperseRadius = int(clamp(u_Glow.w, 1.0, 12.0));

    for (int y = -disperseRadius; y <= disperseRadius; y++) {
        for (int x = -disperseRadius; x <= disperseRadius; x++) {
            if (x == 0 && y == 0) continue;

            vec2 offset = vec2(x, y) * texelSize;
            float neighborMask = handMaskAt(v_TexCoord + offset);
            float neighborDepth = 1.0 - neighborMask;

            float dist = length(vec2(x, y));

            // Сосед — фон, а мы — рука: край руки
            if (neighborDepth > 0.9 && centerDepth < 0.9) {
                edgeStrength += (1.0 / (dist + 0.1));
            }

            // Мы — фон рядом с рукой: внешнее свечение
            if (centerDepth > 0.9 && neighborDepth < 0.9) {
                float falloff = 1.0 - (dist / float(disperseRadius));
                falloff = pow(max(falloff, 0.0), 2.5);
                outwardGlow += falloff * 0.2;
            }
        }
    }

    edgeStrength = clamp(edgeStrength * 0.15, 0.0, 1.0) * maskGain;
    outwardGlow = clamp(outwardGlow, 0.0, 1.0);

    float pulse = 0.9 + sin(time * 2.0) * 0.1;
    float fill = u_Glow.x;
    float glowRadius = max(u_Glow.y, 0.01);
    float glowPower = max(u_Glow.z, 0.0);

    // Фон рядом с рукой: внешнее свечение
    if (centerDepth > 0.9 && outwardGlow > 0.01) {
        vec3 dispersedGlow = u_ColorC.rgb * outwardGlow * glowPower * pulse * glowRadius * 10.0;
        fragColor = vec4(dispersedGlow, outwardGlow * 0.8);
        return;
    }

    vec3 finalColor;
    if (fill > 0.5) {
        vec3 handGlow = u_ColorC.rgb * (glowRadius * 10.0);
        vec3 edgeGlow = u_ColorC.rgb * edgeStrength * glowPower * pulse * 2.5;
        float glowMix = u_Flags.x > 0.5 ? 1.0 : 0.75;
        finalColor = mix(base.rgb, handGlow + edgeGlow, glowMix);
    } else {
        vec3 edgeGlow = u_ColorC.rgb * edgeStrength * glowPower * pulse * glowRadius * 25.0;
        finalColor = base.rgb + edgeGlow;
    }

    fragColor = vec4(finalColor, 1.0);
}
