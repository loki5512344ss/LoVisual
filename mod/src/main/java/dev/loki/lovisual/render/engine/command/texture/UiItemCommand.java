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
