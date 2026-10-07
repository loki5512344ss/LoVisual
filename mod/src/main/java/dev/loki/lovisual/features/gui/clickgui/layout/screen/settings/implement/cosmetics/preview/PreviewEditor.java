package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview;

import com.mojang.blaze3d.platform.NativeImage;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Modal editor over the cosmetics page: drag the window over the picture, wheel to zoom, then save. */
public final class PreviewEditor {
    private static final Identifier EDIT_TEXTURE = Identifier.fromNamespaceAndPath("lovisual", "cosmetic_preview/editing");

    private final PreviewStore store;
    private String moduleId;
    private Path source;
    private int imageW;
    private int imageH;
    private PreviewCrop crop;
    private boolean dragging;
    private float lastMouseX;
    private float lastMouseY;
    private float boxX, boxY, boxW, boxH;
    private float[] saveBtn = new float[4];
    private float[] cancelBtn = new float[4];
    private float[] removeBtn = new float[4];

    public PreviewEditor(PreviewStore store) {
        this.store = store;
    }

    public boolean isOpen() {
        return moduleId != null;
    }

    public String moduleId() {
        return moduleId;
    }

    /** Opens the editor for a newly chosen picture, or for the saved one when {@code newSource} is null. */
    public boolean open(String id, Path newSource) {
        Path file = newSource != null ? newSource : store.pictureOf(id);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !Files.isRegularFile(file)) return false;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            imageW = image.getWidth();
            imageH = image.getHeight();
            mc.getTextureManager().release(EDIT_TEXTURE);
            mc.getTextureManager().register(EDIT_TEXTURE, new DynamicTexture(() -> "lovisual_cosmetic_editing", image));
        } catch (IOException | RuntimeException e) {
            DebugLog.errorOnce("cosmetic-preview-open", "[Cosmetics] Cannot open picture " + file, e);
            return false;
        }
        PreviewStore.Preview saved = newSource == null ? store.get(id) : null;
        crop = saved != null ? saved.crop() : PreviewCrop.centered(imageW, imageH);
        source = newSource;
        moduleId = id;
        dragging = false;
        return true;
    }

    public void close() {
        moduleId = null;
        source = null;
        dragging = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getTextureManager().release(EDIT_TEXTURE);
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float s) {
        if (!isOpen()) return;
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        LayoutRender2D.rect(menuX, menuY, menuW, menuH, 0xDD000000);
        layoutBox(menuX, menuY, menuW, menuH);
        if (dragging) {
            crop = crop.moved((mx - lastMouseX) / boxW, (my - lastMouseY) / boxH);
        }
        lastMouseX = mx;
        lastMouseY = my;

        ClickGuiRenderer.flushRenderer();
        Renderer2D.TEXTURE.roundedTexRect(boxX, boxY, boxW, boxH, 0f, 0f, 0, 0, 1, 1, 0xFFFFFFFF, EDIT_TEXTURE);
        ClickGuiRenderer.flushRenderer();
        drawCropFrame(palette, s);
        float btnY = boxY + boxH + 10f * s;
        float x = boxX;
        saveBtn = button(ClickGuiI18n.tr("clickgui.settings.cosmetics.save", "Save"), x, btnY, true, mx, my, s, palette);
        x += saveBtn[2] + 6f * s;
        cancelBtn = button(ClickGuiI18n.tr("clickgui.settings.cosmetics.cancel", "Cancel"), x, btnY, false, mx, my, s, palette);
        x += cancelBtn[2] + 6f * s;
        removeBtn = source == null && store.get(moduleId) != null
                ? button(ClickGuiI18n.tr("clickgui.settings.cosmetics.remove", "Remove picture"), x, btnY, false, mx, my, s, palette)
                : new float[4];
        String hint = ClickGuiI18n.tr("clickgui.settings.cosmetics.crop_hint", "Drag to move, wheel to zoom");
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), hint, boxX, boxY - 11f * s, 8f * s, palette.panelMuted(), false);
    }

    public boolean mousePressed(float mx, float my, int button) {
        if (!isOpen()) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (hit(saveBtn, mx, my)) {
            boolean ok = source != null ? store.save(moduleId, source, crop) : persistCrop();
            if (ok) close();
        } else if (hit(cancelBtn, mx, my)) {
            close();
        } else if (hit(removeBtn, mx, my)) {
            store.clear(moduleId);
            close();
        } else if (ClickGuiMath.insideRect(mx, my, boxX, boxY, boxW, boxH)) {
            dragging = true;
            lastMouseX = mx;
            lastMouseY = my;
        }
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public boolean mouseScrolled(double amount) {
        if (!isOpen()) return false;
        crop = crop.zoomed(amount > 0 ? 0.9f : 1.1f, imageW, imageH);
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!isOpen()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) close();
        return true;
    }

    private boolean persistCrop() {
        store.saveCrop(moduleId, crop);
        return true;
    }

    private void layoutBox(float menuX, float menuY, float menuW, float menuH) {
        float maxW = menuW * 0.62f;
        float maxH = menuH * 0.58f;
        float fit = Math.min(maxW / imageW, maxH / imageH);
        boxW = imageW * fit;
        boxH = imageH * fit;
        boxX = menuX + (menuW - boxW) / 2f;
        boxY = menuY + (menuH - boxH) / 2f - menuH * 0.04f;
    }

    private void drawCropFrame(SettingsGuiPalette palette, float s) {
        float cx0 = boxX + crop.u0() * boxW;
        float cy0 = boxY + crop.v0() * boxH;
        float cx1 = boxX + crop.u1() * boxW;
        float cy1 = boxY + crop.v1() * boxH;
        int shade = 0xAA000000;
        LayoutRender2D.rect(boxX, boxY, boxW, cy0 - boxY, shade);
        LayoutRender2D.rect(boxX, cy1, boxW, boxY + boxH - cy1, shade);
        LayoutRender2D.rect(boxX, cy0, cx0 - boxX, cy1 - cy0, shade);
        LayoutRender2D.rect(cx1, cy0, boxX + boxW - cx1, cy1 - cy0, shade);
        float t = Math.max(1f, 0.8f * s);
        int line = palette.panelPillActive() | 0xFF000000;
        LayoutRender2D.rect(cx0, cy0, cx1 - cx0, t, line);
        LayoutRender2D.rect(cx0, cy1 - t, cx1 - cx0, t, line);
        LayoutRender2D.rect(cx0, cy0, t, cy1 - cy0, line);
        LayoutRender2D.rect(cx1 - t, cy0, t, cy1 - cy0, line);
    }

    private static float[] button(String label, float x, float y, boolean primary, float mx, float my, float s, SettingsGuiPalette palette) {
        float size = 8f * s;
        float w = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size) + 16f * s;
        float h = 15f * s;
        boolean hover = ClickGuiMath.insideRect(mx, my, x, y, w, h);
        int base = primary ? palette.panelPillActive() : palette.panelPillBase();
        int fill = hover ? SettingsGuiPalette.mix(base, palette.panelText(), 0.15f) : base;
        LayoutRender2D.roundedQuad(x, y, w, h, h * 0.5f, fill, fill, fill, fill);
        float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, x + 8f * s, y + (h - th) / 2f, size, palette.panelText(), false);
        return new float[]{x, y, w, h};
    }

    private static boolean hit(float[] r, float mx, float my) {
        return r[2] > 0f && ClickGuiMath.insideRect(mx, my, r[0], r[1], r[2], r[3]);
    }
}
