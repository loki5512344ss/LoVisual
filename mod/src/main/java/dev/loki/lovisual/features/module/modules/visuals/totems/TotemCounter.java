package dev.loki.lovisual.features.module.modules.visuals.totems;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Totems of Undying in your inventory, drawn next to the hotbar; red when running low. */
@ModuleInfo(
        id = "totemcounter",
        displayName = "Totem Counter",
        aliases = {"totemcounter", "totems"},
        category = ModuleCategory.VISUALS
)
public class TotemCounter extends Module {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int LOW = 0xFFFF5555;

    private final BooleanValue showZero = bool("totemCounterShowZero", "show_zero", true);
    private final NumberValue<Integer> lowAt = num("totemCounterLowAt", "low_at", 2, 0, 10);
    private final NumberValue<Integer> offsetX = num("totemCounterOffsetX", "offset_x", 0, -300, 300);
    private final NumberValue<Integer> offsetY = num("totemCounterOffsetY", "offset_y", 0, -300, 300);

    private ItemStack icon;

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.AFTER_SUBTITLES;
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer,
                                  GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null) return;

        int count = count(mc.player.getInventory());
        String label = label(count, showZero.get());
        if (label == null) return;

        if (icon == null) icon = new ItemStack(Items.TOTEM_OF_UNDYING);
        int x = ctx.guiWidth() / 2 + 98 + offsetX.get();
        int y = ctx.guiHeight() - 20 + offsetY.get();
        ctx.item(icon, x, y);
        ctx.text(mc.font, label, x + 18, y + 4, color(count, lowAt.get()), true);
    }

    private static int count(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) total += stack.getCount();
        }
        return total;
    }

    /** Text shown beside the icon, or null when nothing should be drawn. */
    static String label(int count, boolean showZero) {
        if (count <= 0 && !showZero) return null;
        return "x" + Math.max(0, count);
    }

    /** Red once the stock is at or below the threshold (threshold 0 only flags an empty stock). */
    static int color(int count, int lowAt) {
        return count <= lowAt ? LOW : WHITE;
    }
}
