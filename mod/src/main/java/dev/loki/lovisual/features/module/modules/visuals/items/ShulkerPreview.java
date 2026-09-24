package dev.loki.lovisual.features.module.modules.visuals.items;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

@ModuleInfo(
        id = "shulkerpreview",
        displayName = "Shulker Preview",
        aliases = {"shulkerpreview", "preview"},
        category = ModuleCategory.VISUALS,
        description = "module.shulkerpreview.description"
)
public final class ShulkerPreview extends Module {

    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int PAD = 4;
    private static final int HEADER_HEIGHT = 10;
    private static final int HEADER_GAP = 3;
    private static final int CURSOR_OFFSET = 14;
    private static final int BG_ALPHA = 0xE6;
    private static final int STROKE_ALPHA = 0xDC;
    private static final int CELL_ALPHA = 0x8C;

    private final NumberValue<Float> scale =
            num("shulkerpreview_scale", "scale", 1.0f, 0.5f, 2.0f);

    /**
     * Called from AbstractContainerScreenMixin at the end of screen extraction.
     */
    public static void onScreenExtract(GuiGraphicsExtractor ctx, ItemStack stack, int mouseX, int mouseY) {
        ShulkerPreview module = Modules.get(ShulkerPreview.class);
        if (module == null || !module.isEnabled()) return;
        module.drawPreview(ctx, stack, mouseX, mouseY);
    }

    private void drawPreview(GuiGraphicsExtractor ctx, ItemStack stack, int mouseX, int mouseY) {
        if (ctx == null || !isShulkerBox(stack)) return;

        NonNullList<ItemStack> contents = collectContents(stack);
        if (contents == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        float panelScale = Mth.clamp(scale.get(), 0.5f, 2.0f);
        int width = PAD * 2 + COLS * SLOT_SIZE;
        int height = PAD * 2 + HEADER_HEIGHT + HEADER_GAP + ROWS * SLOT_SIZE;

        int x = horizontalOrigin(mouseX, width * panelScale, ctx.guiWidth());
        int y = verticalOrigin(mouseY, height * panelScale, ctx.guiHeight());

        Themes.Theme theme = Theme.theme();
        int bg = HudRenderUtil.setAlpha(theme.windowBg(), BG_ALPHA);
        int stroke = HudRenderUtil.setAlpha(
                HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.34f),
                STROKE_ALPHA);
        int headerColor = HudRenderUtil.setAlpha(theme.textPrimary(), 0xFF);
        int cellColor = HudRenderUtil.setAlpha(theme.surface(), CELL_ALPHA);

        Font font = mc.font;
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(panelScale, panelScale);
        try {
            ctx.fill(RenderPipelines.GUI, 0, 0, width, height, bg);
            ctx.outline(0, 0, width, height, stroke);
            ctx.text(font, stack.getHoverName(), PAD, PAD, headerColor);

            int gridY = PAD + HEADER_HEIGHT + HEADER_GAP;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int cellX = PAD + col * SLOT_SIZE;
                    int cellY = gridY + row * SLOT_SIZE;
                    ctx.fill(RenderPipelines.GUI,
                            cellX, cellY, cellX + SLOT_SIZE, cellY + SLOT_SIZE,
                            cellColor);

                    ItemStack item = contents.get(row * COLS + col);
                    if (item == null || item.isEmpty()) continue;
                    ctx.item(item, cellX + 1, cellY + 1);
                    ctx.itemDecorations(font, item, cellX + 1, cellY + 1);
                }
            }
        } finally {
            ctx.pose().popMatrix();
        }
    }

    private static NonNullList<ItemStack> collectContents(ItemStack stack) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container == null) return null;

        NonNullList<ItemStack> contents = NonNullList.withSize(COLS * ROWS, ItemStack.EMPTY);
        container.copyInto(contents);

        for (ItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                return contents;
            }
        }
        return null;
    }

    private static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int horizontalOrigin(int mouseX, float width, int bounds) {
        int right = (int) (mouseX + CURSOR_OFFSET);
        if (right + width <= bounds) return right;

        int left = (int) (mouseX - CURSOR_OFFSET - width);
        if (left >= 0) return left;

        int clamped = (int) (bounds - width);
        return Math.max(0, Math.min(right, clamped));
    }

    private static int verticalOrigin(int mouseY, float height, int bounds) {
        int top = (int) (mouseY - height / 2f);
        if (top + height >= bounds) {
            top = (int) (bounds - height);
        }
        return Math.max(0, top);
    }
}
