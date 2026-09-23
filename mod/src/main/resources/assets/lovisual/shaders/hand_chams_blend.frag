#version 330 core

in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D u_Shader1;
uniform sampler2D u_Shader2;
uniform sampler2D u_Mask;

layout (std140) uniform HandBlend {
    vec4 u_Blend; // x = blendMode (0=Mix,1=Add,2=Multiply,3=Screen,4=Overlay,5=Difference), y = blendIntensity, z = shader1Strength, w = shader2Strength
};

float maskAt(vec2 uv) {
    vec4 m = texture(u_Mask, uv);
    return max(m.a, max(m.r, max(m.g, m.b)));
}

vec3 blendAdd(vec3 base, vec3 blend) {
    return min(base + blend, vec3(1.0));
}

vec3 blendScreen(vec3 base, vec3 blend) {
    return vec3(1.0) - (vec3(1.0) - base) * (vec3(1.0) - blend);
}

vec3 blendOverlay(vec3 base, vec3 blend) {
    vec3 result;
    result.r = base.r < 0.5 ? (2.0 * base.r * blend.r) : (1.0 - 2.0 * (1.0 - base.r) * (1.0 - blend.r));
    result.g = base.g < 0.5 ? (2.0 * base.g * blend.g) : (1.0 - 2.0 * (1.0 - base.g) * (1.0 - blend.g));
    result.b = base.b < 0.5 ? (2.0 * base.b * blend.b) : (1.0 - 2.0 * (1.0 - base.b) * (1.0 - blend.b));
    return result;
}

void main() {
    vec4 shader1Color = texture(u_Shader1, v_TexCoord);
    vec4 shader2Color = texture(u_Shader2, v_TexCoord);

    float mask = maskAt(v_TexCoord);
    if (mask <= 0.01) {
        fragColor = shader1Color;
        return;
    }

    vec3 color1 = shader1Color.rgb * u_Blend.z;
    vec3 color2 = shader2Color.rgb * u_Blend.w;
    float intensity = u_Blend.y;

    vec3 blendedColor;
    int mode = int(u_Blend.x);
    if (mode == 0) {
        blendedColor = mix(color1, color2, intensity);
    } else if (mode == 1) {
        blendedColor = mix(color1, blendAdd(color1, color2), intensity);
    } else if (mode == 2) {
        blendedColor = mix(color1, color1 * color2, intensity);
    } else if (mode == 3) {
        blendedColor = mix(color1, blendScreen(color1, color2), intensity);
    } else if (mode == 4) {
        blendedColor = mix(color1, blendOverlay(color1, color2), intensity);
    } else if (mode == 5) {
        blendedColor = mix(color1, abs(color1 - color2), intensity);
    } else {
        blendedColor = color1;
    }

    fragColor = vec4(blendedColor, 1.0);
}
