package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Addon manager screen facade: wires the controller and renderer together.
 */
public final class LoVisualAddonManagerScreen extends Screen {
    private final Screen parent;
    private final AddonManagerController controller = new AddonManagerController();
    private AddonManagerRenderer renderer;

    public LoVisualAddonManagerScreen(Screen parent) {
        super(Component.translatable("screen.lovisual.addon_manager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        renderer = AddonManagerRenderer.create();
        controller.ensureSelection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        controller.updateUiMetrics(minecraft, width, height);
        controller.ensureSelection();
        controller.clampScroll();
        renderShaderBackground(context);
        float fx = toFixedX(mouseX);
        float fy = toFixedY(mouseY);
        controller.updateAnimations(fx, fy);
        renderer.drawFrame(context, controller, fx, fy);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return true;
        float mx = toFixedX((float) click.x());
        float my = toFixedY((float) click.y());
        controller.handleClick(mx, my, this::closeToParent);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return controller.scrollBy(toFixedX((float) mouseX), toFixedY((float) mouseY), (float) verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        if (input.key() == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void closeToParent() {
        if (minecraft != null) ClientScreen.show(minecraft, parent);
    }

    private float toFixedX(float screenX) {
        Minecraft mc = minecraft;
        if (mc == null) return screenX;
        float hudScale = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        if (hudScale <= 0f) return screenX;
        return screenX * mc.getWindow().getGuiScale() / hudScale;
    }

    private float toFixedY(float screenY) {
        Minecraft mc = minecraft;
        if (mc == null) return screenY;
        float hudScale = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        if (hudScale <= 0f) return screenY;
        return screenY * mc.getWindow().getGuiScale() / hudScale;
    }

    private void renderShaderBackground(GuiGraphicsExtractor context) {
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        MainConfig cfg = MainConfig.get();
        String mode = cfg != null ? cfg.getMenuBackgroundMode() : "off";
        if (mode == null || mode.equalsIgnoreCase("off")) {
            context.fill(0, 0, width, height, 0xFF000000);
            return;
        }
        MenuBackgroundRenderer.render(mc, mode.equalsIgnoreCase("aurora"));
    }
}
