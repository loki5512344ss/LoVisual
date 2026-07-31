/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public enum TextureStorage {
    ;
    public static final Identifier BLOOM =
            Identifier.fromNamespaceAndPath("lovisual", "textures/bloom.png");
    public static final Identifier FIRE_FLY =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/firefly.png");
    public static final Identifier DEFAULT_CIRCLE =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/default_circle.png");
    public static final Identifier JUMP_CIRCLE =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/jump_circle.png");
    public static final Identifier BUBBLE =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/bubble.png");
    public static final Identifier FUNNEL_EYE =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/funnel/funnel_eye.png");
    public static final Identifier FUNNEL_DISTORTION =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/funnel/distortion.png");
    public static final Identifier PARTICLE_STARS =
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/star.png");
    public static final Identifier[] RANDOM_PARTICLES = new Identifier[]{
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p1.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p2.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p3.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p4.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p5.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p7.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p8.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p9.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p10.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p11.png"),
            Identifier.fromNamespaceAndPath("lovisual", "textures/particles/p12.png")
    };
    public static final Identifier CAPTURE =
            Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/capture.png");

    public static void preload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        tm.getTexture(BLOOM);
        tm.getTexture(FIRE_FLY);
        tm.getTexture(DEFAULT_CIRCLE);
        tm.getTexture(JUMP_CIRCLE);
        tm.getTexture(BUBBLE);
        tm.getTexture(FUNNEL_EYE);
        tm.getTexture(FUNNEL_DISTORTION);
        tm.getTexture(PARTICLE_STARS);
        tm.getTexture(CAPTURE);
        for (Identifier id : RANDOM_PARTICLES) {
            tm.getTexture(id);
        }
    }
}
