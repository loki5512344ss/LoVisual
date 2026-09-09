package dev.loki.lovisual.features.module.modules.visuals.cosmetics;

import dev.loki.lovisual.features.cosmetic.CosmeticSlot;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticDisplay;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Cosmetics editor: a live 3D player preview on the left (drag to rotate, scroll to
 * zoom) with clickable body-zone hotspots, and a per-slot picker/offset panel on the
 * right. Opened via the "Open Cosmetics" action / right-click on the module card.
 *
 * The preview uses {@link InventoryScreen#extractEntityInInventoryFollowsMouse}, which
 * derives the entity's yaw/pitch from a "mouse position" argument — we don't feed it the
 * real cursor, we feed it a synthetic position offset by {@link #previewYawPx}/
 * {@link #previewPitchPx} so the pose only changes while the user is dragging the preview.
 */
public final class CosmeticsScreen extends Screen {
    private static final float DEFAULT_YAW_PX = -40f;
    private static final float DEFAULT_PITCH_PX = 15f;
    private static final float MAX_PITCH_PX = 60f;
    private static final float PANEL_WIDTH = 160f;
    private static final float MARGIN = 12f;

    private final CosmeticsSlotPanel slotPanel;

    private float previewYawPx = DEFAULT_YAW_PX;
    private float previewPitchPx = DEFAULT_PITCH_PX;
    private float previewScale = 1.0f;
    private boolean backView = false;
    private CosmeticSlot selectedSlot = CosmeticSlot.HEAD;

    private boolean draggingPreview = false;
    private double pressX;
    private double pressY;

    public CosmeticsScreen() {
        super(Component.literal("Cosmetics"));
        CosmeticDisplay module = Modules.get(CosmeticDisplay.class);
        this.slotPanel = new CosmeticsSlotPanel(module);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int w = context.guiWidth();
        int h = context.guiHeight();

        Renderer2D.COLOR.begin();
        try {
            Renderer2D.COLOR.roundedRect(0, 0, w, h, 0f, Theme.theme().windowBg());

            float previewW = w - PANEL_WIDTH - MARGIN * 3f;
            float previewH = h - MARGIN * 2f;
            renderPreview(context, MARGIN, MARGIN, previewW, previewH, delta);
            renderHotspots(MARGIN, MARGIN, previewW, previewH, mouseX, mouseY);

            float panelX = MARGIN * 2f + previewW;
            slotPanel.render(panelX, MARGIN, PANEL_WIDTH, previewH, selectedSlot, mouseX, mouseY);
        } finally {
            Renderer2D.COLOR.render();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    private void renderPreview(GuiGraphicsExtractor context, float x, float y, float width, float height, float tickDelta) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = (int) (x + width);
        int y2 = (int) (y + height);
        int size = (int) (Math.min(width, height) * 0.45f * previewScale);
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        InventoryScreen.extractEntityInInventoryFollowsMouse(
                context, x1, y1, x2, y2, size,
                centerX + previewYawPx, centerY + previewPitchPx, tickDelta, player
        );
    }

    private void renderHotspots(float x, float y, float width, float height, double mouseX, double mouseY) {
        CosmeticsHotspot[] zones = CosmeticsHotspot.forRegion(x, y, width, height, backView);
        int accent = Theme.theme().accent();
        for (CosmeticsHotspot zone : zones) {
            boolean hovered = zone.contains(mouseX, mouseY);
            boolean active = zone.slot() == selectedSlot;
            if (hovered || active) {
                int alpha = active ? 70 : 35;
                Renderer2D.COLOR.roundedRect(zone.x(), zone.y(), zone.width(), zone.height(), 3f,
                        (accent & 0x00FFFFFF) | (alpha << 24));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        pressX = click.x();
        pressY = click.y();

        if (handlePanelClick(click.x(), click.y())) return true;

        if (click.button() == 0 && isInsidePreview((float) click.x(), (float) click.y())) {
            draggingPreview = true;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (!draggingPreview) return false;
        previewYawPx += (float) dragX;
        previewPitchPx = Math.clamp(previewPitchPx + (float) dragY, -MAX_PITCH_PX, MAX_PITCH_PX);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean wasDragging = draggingPreview;
        draggingPreview = false;

        double moved = Math.hypot(click.x() - pressX, click.y() - pressY);
        if (!wasDragging || moved > 4.0) return true;
        if (moved <= 4.0) {
            handlePreviewClick((float) click.x(), (float) click.y());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        previewScale = Math.clamp(previewScale + (float) verticalAmount * 0.1f, 0.5f, 2.0f);
        return true;
    }

    private void handlePreviewClick(float mouseX, float mouseY) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float previewW = w - PANEL_WIDTH - MARGIN * 3f;
        float previewH = h - MARGIN * 2f;

        CosmeticsHotspot[] zones = CosmeticsHotspot.forRegion(MARGIN, MARGIN, previewW, previewH, backView);
        CosmeticSlot hit = CosmeticsHotspot.hitTest(zones, mouseX, mouseY);
        if (hit == CosmeticSlot.BACK || hit == CosmeticSlot.SHOULDERS) {
            backView = !backView;
        }
        if (hit != null) {
            selectSlot(hit);
        }
    }

    private boolean handlePanelClick(double mouseX, double mouseY) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float previewW = w - PANEL_WIDTH - MARGIN * 3f;
        float previewH = h - MARGIN * 2f;
        float panelX = MARGIN * 2f + previewW;

        return slotPanel.handleClick(panelX, MARGIN, PANEL_WIDTH, selectedSlot, mouseX, mouseY);
    }

    private void selectSlot(CosmeticSlot slot) {
        selectedSlot = slot;
        CosmeticDisplay module = Modules.get(CosmeticDisplay.class);
        if (module != null && slot.isAccessory()) {
            module.setActiveSlot(slot);
        }
    }

    private boolean isInsidePreview(float mouseX, float mouseY) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float previewW = w - PANEL_WIDTH - MARGIN * 3f;
        float previewH = h - MARGIN * 2f;
        return mouseX >= MARGIN && mouseX <= MARGIN + previewW && mouseY >= MARGIN && mouseY <= MARGIN + previewH;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
