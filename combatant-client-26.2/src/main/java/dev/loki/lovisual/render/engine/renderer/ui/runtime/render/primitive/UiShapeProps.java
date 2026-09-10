/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive;

import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerKind;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiEdgeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;

import java.util.Locale;
import java.util.Map;

/**
 * Parsing of legacy JS shape props into corner/edge specs and flexible-box descriptors.
 */
public final class UiShapeProps {
    private static final String[] BOX_KEYS = {
            "corners", "edges", "cornerTL", "cornerTopLeft", "cornerTR", "cornerTopRight",
            "cornerBR", "cornerBottomRight", "cornerBL", "cornerBottomLeft",
            "edgeTop", "topEdge", "edgeRight", "rightEdge", "edgeBottom", "bottomEdge",
            "edgeLeft", "leftEdge"
    };

    private UiShapeProps() {
    }

    public static boolean isBoxShape(String shape, UiProps props) {
        // Only the flexible-box contract is routed through UiBoxShape; legacy JS shapes
        // such as "rounded-gradient"/"chamfered"/"notched" keep their tuned Renderer2D paths.
        for (String key : BOX_KEYS) {
            if (props.get(key) != null) return true;
        }
        return switch (shape) {
            case "box", "mixed", "flex", "flex-box", "flex_box" -> true;
            default -> false;
        };
    }

    public static Object first(UiProps props, String a, String b) {
        Object value = props.get(a);
        return value != null ? value : props.get(b);
    }

    public static float number(Object value, float fallback) {
        if (value instanceof Number n) return n.floatValue();
        if (value instanceof String s) {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public static float chamferCorner(UiProps props, String shortName, String longName, float fallback) {
        Object value = props.get("cut" + shortName);
        if (value == null) value = props.get("chamfer" + shortName);
        if (value == null) value = props.get("cut" + longName);
        if (value == null) value = props.get("chamfer" + longName);
        return number(value, fallback);
    }

    public static UiCornerSpec corner(UiProps props, String shortName, String longName,
                                      UiCornerSpec fallback, float radius, float cut) {
        Object explicit = cornerValue(props.get("corners"), shortName, longName);
        if (explicit == null) explicit = first(props, "corner" + shortName, "corner" + longName);
        if (explicit != null) return cornerFromObject(explicit, fallback, radius, cut);
        Object mode = first(props, "cornerMode" + shortName, "cornerMode" + longName);
        if (mode != null) return cornerFromObject(mode, fallback, radius, cut);
        float r = props.number("radius" + shortName, props.number("radius" + longName, fallback.radiusX()));
        float c = props.number("cut" + shortName, props.number("cut" + longName,
                props.number("chamfer" + shortName, props.number("chamfer" + longName, fallback.cutX()))));
        if (r > 0.0f && (fallback.kind() == UiCornerKind.ROUNDED || fallback.kind() == UiCornerKind.SQUARE)) {
            return UiCornerSpec.rounded(r, r);
        }
        if (c > 0.0f && fallback.kind() != UiCornerKind.ROUNDED) {
            return UiCornerSpec.chamfered(c);
        }
        return fallback;
    }

    private static UiCornerSpec cornerFromObject(Object value, UiCornerSpec fallback, float radius, float cut) {
        if (value instanceof Map<?, ?> map) {
            Object rawKind = map.get("kind");
            if (rawKind == null) rawKind = map.get("type");
            String kind = String.valueOf(rawKind != null ? rawKind : "").toLowerCase(Locale.ROOT);
            float r = number(map.get("radius"), radius);
            float rx = number(map.get("radiusX"), r);
            float ry = number(map.get("radiusY"), r);
            float c = number(map.get("cut"), number(map.get("chamfer"), cut));
            float cx = number(map.get("cutX"), c);
            float cy = number(map.get("cutY"), c);
            return switch (kind) {
                case "round", "rounded", "radius" -> UiCornerSpec.rounded(rx, ry);
                case "chamfer", "chamfered", "cut", "bevel", "beveled" -> UiCornerSpec.chamfered(cx, cy);
                case "concave", "inverse", "inverse-round", "inverse_round" -> UiCornerSpec.concaveRounded(r);
                case "square", "none" -> UiCornerSpec.square();
                default -> fallback;
            };
        }
        if (value instanceof String s) {
            String normalized = s.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("round")) return UiCornerSpec.rounded(radius, radius);
            if (normalized.startsWith("chamfer") || normalized.startsWith("cut") || normalized.startsWith("bevel"))
                return UiCornerSpec.chamfered(cut);
            if (normalized.startsWith("concave") || normalized.startsWith("inverse"))
                return UiCornerSpec.concaveRounded(radius);
            if (normalized.startsWith("square") || normalized.startsWith("none")) return UiCornerSpec.square();
        }
        return fallback;
    }

    private static Object cornerValue(Object corners, String shortName, String longName) {
        if (!(corners instanceof Map<?, ?> map)) return null;
        Object value = map.get(shortName);
        if (value == null) value = map.get(shortName.toLowerCase(Locale.ROOT));
        if (value == null) value = map.get(longName);
        if (value == null) value = map.get(Character.toLowerCase(longName.charAt(0)) + longName.substring(1));
        return value;
    }

    static Object edgeValue(Object edges, String name) {
        if (!(edges instanceof Map<?, ?> map)) return null;
        Object value = map.get(name);
        if (value == null) value = map.get(name.toUpperCase(Locale.ROOT));
        return value;
    }

    public static UiEdgeSpec edgeFromObject(Object value, double length, boolean horizontal) {
        if (value == null) return UiEdgeSpec.straight();
        if (value instanceof Map<?, ?> map) {
            Object rawKind = map.get("kind");
            if (rawKind == null) rawKind = map.get("type");
            String kind = String.valueOf(rawKind != null ? rawKind : "").toLowerCase(Locale.ROOT);
            if (kind.equals("notch") || kind.equals("notched")) {
                float width = number(map.get("width"), number(map.get("size"), (float) Math.min(length * 0.18, 18.0)));
                float depth = number(map.get("depth"), (float) Math.min(length * 0.10, 8.0));
                Object offset = map.get("offset");
                if (offset == null || "center".equals(String.valueOf(offset)))
                    return UiEdgeSpec.notchedCenter(width, depth);
                return UiEdgeSpec.notched(number(offset, 0.0f), width, depth);
            }
            if (kind.equals("inset")) return UiEdgeSpec.inset(number(map.get("depth"), 0.0f));
            return UiEdgeSpec.straight();
        }
        if (value instanceof String s) {
            String normalized = s.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("notch") || normalized.equals("notched"))
                return UiEdgeSpec.notchedCenter(Math.min(length * 0.18, 18.0), Math.min(length * 0.10, 8.0));
        }
        return UiEdgeSpec.straight();
    }
}
