/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.backend;

import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.core.RenderFrameContext;

/**
 * Converts command placement intent into a backend-consumable transform.
 */
public enum TextPlacementResolver {
    ;

    public static TextPlacementTransform resolve(TextDrawCommand command, RenderFrameContext context) {
        if (command == null) return TextPlacementTransform.ui(0f, 0f, 0f, 1f);
        TextPlacementMode mode = command.placement() != null ? command.placement() : TextPlacementMode.UI;
        return switch (mode) {
            case UI -> TextPlacementTransform.ui(command.x(), command.y(), command.z(), command.size());
            case SCREEN_SPACE -> TextPlacementTransform.screen(command.x(), command.y(), command.z(), command.size());
            case WORLD_BILLBOARD ->
                    TextPlacementTransform.worldBillboard(new Vec3(command.x(), command.y(), command.z()), command.size());
            case WORLD_ALIGNED ->
                    TextPlacementTransform.worldAligned(new Vec3(command.x(), command.y(), command.z()), null, command.size());
        };
    }
}
