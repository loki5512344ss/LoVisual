package dev.loki.lovisual.features.gui.mainmenu.proxy;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Proxy manager screen facade: wires controller and renderer together.
 */
public final class LoVisualProxyManagerScreen extends Screen {
    private final Screen parent;
    private final ProxyManagerController controller = new ProxyManagerController();
    private final ProxyManagerRenderer renderer = new ProxyManagerRenderer();

    public LoVisualProxyManagerScreen(Screen parent) {
        super(Component.translatable("screen.lovisual.proxy_manager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        renderer.init();
        controller.loadProxy();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        controller.updateUiMetrics(minecraft, width, height);
        renderShaderBackground(context);
        float fx = toFixedX(mouseX);
        float fy = toFixedY(mouseY);
        renderer.updateAnimations(controller, fx, fy);

        ViewportContext.beginUnscaledLogical(context);
        Renderer2D.COLOR.begin();
        try {
            renderDimmer();
            renderer.renderPanel(controller, fx, fy);
        } finally {
            Renderer2D.COLOR.render();
            ViewportContext.end(context);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return true;
        boolean back = controller.handleClick(toFixedX((float) click.x()), toFixedY((float) click.y()));
        if (back) closeToParent();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (controller.handleKey(input)) return true;
        if (input.key() == 256) { closeToParent(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (controller.handleChar(input)) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private void renderDimmer() {
        Themes.Theme t = Theme.theme();
        int soft = (HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.46f) & 0x00FFFFFF) | (140 << 24);
        int deep = (HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.68f) & 0x00FFFFFF) | (180 << 24);
        Renderer2D.COLOR.quad(0, 0, controller.fixedWidth(), controller.fixedHeight(), soft, soft, deep, deep);
    }

    private void closeToParent() {
        if (minecraft != null) ClientScreen.show(minecraft, parent);
    }

    private float toFixedX(float screenX) {
        Minecraft mc = minecraft;
        if (mc == null) return screenX;
        float hs = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        return hs <= 0f ? screenX : screenX * mc.getWindow().getGuiScale() / hs;
    }

    private float toFixedY(float screenY) {
        Minecraft mc = minecraft;
        if (mc == null) return screenY;
        float hs = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        return hs <= 0f ? screenY : screenY * mc.getWindow().getGuiScale() / hs;
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
