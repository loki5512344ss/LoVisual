/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command.texture;

import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiItemCommand(ItemStack stack, int x, int y, float scale) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.ITEM;
    }
}
