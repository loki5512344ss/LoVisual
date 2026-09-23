#version 330 core

in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D u_Src;
uniform sampler2D u_Mask;

layout (std140) uniform HandChams {
    vec4 u_View;   // x = resX, y = resY, z = time
    vec4 u_Mix;    // x = effectAlpha
    vec4 u_ColorA;
    vec4 u_ColorB;
    vec4 u_ColorC;
    vec4 u_ColorD;
    vec4 u_Common;
    vec4 u_Glow;
    vec4 u_Flags;  // x = hideHand
};

float handMaskAt(vec2 uv) {
    vec4 m = texture(u_Mask, uv);
    return max(m.a, max(m.r, max(m.g, m.b)));
}

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);
    if (mask <= 0.02 || u_Flags.x > 0.5) {
        fragColor = base;
        return;
    }

    float maskGain = smoothstep(0.02, 0.3, mask);
    vec3 inverted = vec3(1.0) - base.rgb;
    vec3 finalColor = mix(base.rgb, inverted, u_Mix.x * maskGain);
    fragColor = vec4(finalColor, 1.0);
}
