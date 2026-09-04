/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;

public enum LoVisualVertexFormats {
    ;
    public static final VertexFormat POS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .build();

    public static final VertexFormat POS3_TEXTURE_COLOR_PARAMS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS3)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .build();

    public static final VertexFormat POS2_COLOR = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .build();

    /**
     * World-space wide line vertex.
     * Position is the current endpoint, Color is endpoint color, Line packs the opposite endpoint in xyz
     * and signed screen-space line width in w. The vertex shader expands the endpoint in clip space.
     */
    public static final VertexFormat POS3_COLOR_LINE = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS3)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Line", LoVisualVertexFormatElements.PARAMS)
            .build();

    public static final VertexFormat POS2_TEXTURE_COLOR = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .build();

    public static final VertexFormat POS2_COLOR_RECT_PARAMS = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .build();

    public static final VertexFormat POS2_COLOR_RECT_PARAMS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .build();

    public static final VertexFormat POS2_LOCAL_COLOR_RECT_PARAMS = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Local", LoVisualVertexFormatElements.LOCAL)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .build();

    public static final VertexFormat POS2_LOCAL_COLOR_RECT_PARAMS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Local", LoVisualVertexFormatElements.LOCAL)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .build();

    public static final VertexFormat POS2_TEXTURE_COLOR_RECT_PARAMS = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .build();

    public static final VertexFormat POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Local", LoVisualVertexFormatElements.LOCAL)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .build();

    public static final VertexFormat POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Local", LoVisualVertexFormatElements.LOCAL)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .build();

    public static final VertexFormat POS2_TEXTURE_COLOR_RECT_PARAMS2 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("UV0", LoVisualVertexFormatElements.TEXTURE)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .build();

    public static final VertexFormat POS2_LOCAL_COLOR_RECT_PARAMS5 = VertexFormat.builder(0)
            .addAttribute("Position", LoVisualVertexFormatElements.POS2)
            .addAttribute("Local", LoVisualVertexFormatElements.LOCAL)
            .addAttribute("Color", LoVisualVertexFormatElements.COLOR)
            .addAttribute("Rect", LoVisualVertexFormatElements.RECT)
            .addAttribute("Params", LoVisualVertexFormatElements.PARAMS)
            .addAttribute("Params2", LoVisualVertexFormatElements.PARAMS2)
            .addAttribute("Params3", LoVisualVertexFormatElements.PARAMS3)
            .addAttribute("Params4", LoVisualVertexFormatElements.PARAMS4)
            .addAttribute("Params5", LoVisualVertexFormatElements.PARAMS5)
            .build();

}
