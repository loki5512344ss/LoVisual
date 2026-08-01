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
    vec4 u_ColorD; // rgb = colorD (r = contrast, g = lighting)
    vec4 u_Common; // x = snowIntensity, y = snowSpeed, z = stripesWidth, w = stripesSpeed
    vec4 u_Glow;   // x = fillGlow, y = glowRadius, z = glowPower, w = glowDispersion
    vec4 u_Flags;  // x = hideHand
};

float handMaskAt(vec2 uv) {
    vec4 m = texture(u_Mask, uv);
    return max(m.a, max(m.r, max(m.g, m.b)));
}

#define SPIN_ROTATION -2.0
#define SPIN_SPEED 7.0
#define OFFSET vec2(0.0)
#define SPIN_AMOUNT 0.25
#define PIXEL_FILTER 745.0
#define SPIN_EASE 1.0
#define PI 3.14159265359
#define IS_ROTATE false

vec4 effect(vec2 screenSize, vec2 screen_coords, float contrast, float lighting) {
    float pixel_size = length(screenSize.xy) / PIXEL_FILTER;
    vec2 effectUv = (floor(screen_coords.xy*(1./pixel_size))*pixel_size - 0.5*screenSize.xy)/length(screenSize.xy) - OFFSET;
    float uv_len = length(effectUv);

    float speed = (SPIN_ROTATION*SPIN_EASE*0.2);
    if(IS_ROTATE){
       speed = u_View.z * speed;
    }
    speed += 302.2;
    float new_pixel_angle = atan(effectUv.y, effectUv.x) + speed - SPIN_EASE*20.*(1.*SPIN_AMOUNT*uv_len + (1. - 1.*SPIN_AMOUNT));
    vec2 mid = (screenSize.xy/length(screenSize.xy))/2.;
    effectUv = (vec2((uv_len * cos(new_pixel_angle) + mid.x), (uv_len * sin(new_pixel_angle) + mid.y)) - mid);

    effectUv *= 30.;
    speed = u_View.z*(SPIN_SPEED);
    vec2 uv2 = vec2(effectUv.x+effectUv.y);

    for(int i=0; i < 5; i++) {
        uv2 += sin(max(effectUv.x, effectUv.y)) + effectUv;
        effectUv  += 0.5*vec2(cos(5.1123314 + 0.353*uv2.y + speed*0.131121),sin(uv2.x - 0.113*speed));
        effectUv  -= 1.0*cos(effectUv.x + effectUv.y) - 1.0*sin(effectUv.x*0.711 - effectUv.y);
    }

    float contrast_mod = (0.25*contrast + 0.5*SPIN_AMOUNT + 1.2);
    float paint_res = min(2., max(0.,length(effectUv)*(0.035)*contrast_mod));
    float c1p = max(0.,1. - contrast_mod*abs(1.-paint_res));
    float c2p = max(0.,1. - contrast_mod*abs(paint_res));
    float c3p = 1. - min(1., c1p + c2p);
    float light = (lighting - 0.2)*max(c1p*5. - 4., 0.) + lighting*max(c2p*5. - 4., 0.);

    vec4 colour1 = vec4(u_ColorA.rgb, 1.0);
    vec4 colour2 = vec4(u_ColorB.rgb, 1.0);
    vec4 colour3 = vec4(u_ColorC.rgb, 1.0);

    return (0.3/contrast)*colour1 + (1. - 0.3/contrast)*(colour1*c1p + colour2*c2p + vec4(c3p*colour3.rgb, c3p*colour1.a)) + light;
}

void main() {
    vec4 base = texture(u_Src, v_TexCoord);
    float mask = handMaskAt(v_TexCoord);
    if (mask <= 0.02) {
        fragColor = base;
        return;
    }

    float contrast = clamp(u_ColorD.r, 0.1, 1.0) * 10.0;
    float lighting = clamp(u_ColorD.g, 0.0, 1.0);

    vec4 effectColor = effect(u_View.xy, v_TexCoord * u_View.xy, contrast, lighting);

    vec3 finalColor = mix(base.rgb, effectColor.rgb, u_Mix.x * smoothstep(0.02, 0.3, mask));
    fragColor = vec4(finalColor, 1.0);
}
