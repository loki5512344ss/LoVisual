package dev.loki.lovisual.features.gui.mainmenu.alt;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.gui.mainmenu.alt.auth.AltMicrosoftAuth;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltManagerRenderer;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import dev.loki.lovisual.render.engine.math.HudScale;

/**
 * Account manager screen facade: wires the controller, Microsoft auth flow and renderer together.
 */
public final class LoVisualAltManagerScreen extends Screen {
    private final Screen parent;
    private final AltMicrosoftAuth auth = new AltMicrosoftAuth(AccountConfig.get());
    private final AltManagerController controller = new AltManagerController(AccountConfig.get(), auth);
    private AltManagerRenderer renderer;

    public LoVisualAltManagerScreen(Screen parent) {
        this(parent, false);
    }

    public LoVisualAltManagerScreen(Screen parent, boolean microsoftMode) {
        super(Component.translatable("screen.lovisual.alt_manager.title"));
        this.parent = parent;
        controller.setMicrosoftMode(microsoftMode);
    }

    @Override
    protected void init() {
        renderer = AltManagerRenderer.create();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        controller.updateUiMetrics(minecraft, width, height);
        renderShaderBackground(context);
        renderer.drawFrame(context, controller, auth, toFixedX(mouseX), toFixedY(mouseY));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return true;
        float mx = toFixedX((float) click.x());
        float my = toFixedY((float) click.y());
        if (controller.handleClick(mx, my)) return true;
        renderer.handleCardClick(mx, my, controller);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return controller.scrollBy(toFixedX((float) mouseX), toFixedY((float) mouseY), (float) verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (controller.isNicknameFocused() && controller.handleKey(input.key())) return true;
        if (input.key() == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (controller.isNicknameFocused() && controller.handleChar(input.codepoint())) return true;
        return super.charTyped(input);
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
