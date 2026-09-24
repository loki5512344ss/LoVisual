package dev.loki.lovisual.features.gui.clickgui.editor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.module.modules.misc.ClickGui;

public final class ClickGuiEditorScreen extends Screen {

    public ClickGuiEditorScreen() {
        super(Component.literal("ClickGui Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Render is intentionally deferred to GameRendererMixin after GuiRenderer.render(...).
        // Vanilla HUD/screen elements are queued into GuiRenderState and otherwise flush above immediate ClickGui rendering.
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // No vanilla blur/darkening behind editor.
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        ClickGuiRenderer.onEditorMouseMoveScaled(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        ClickGuiRenderer.onEditorMouseButtonScaled(click.x(), click.y(), click.button(), true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        ClickGuiRenderer.onEditorMouseButtonScaled(click.x(), click.y(), click.button(), false);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ClickGuiRenderer.onEditorMouseScrollScaled(mouseX, mouseY, verticalAmount);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClickGuiRenderer.closeGuiEditor();
    }
}
