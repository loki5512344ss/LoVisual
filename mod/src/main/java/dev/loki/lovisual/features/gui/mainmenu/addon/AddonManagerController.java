package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.core.AddonManager;
import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AddonManagerController {
    private final AddonPanelAnims anims = new AddonPanelAnims();
    private final AddonScanState scanState = new AddonScanState();
    private final Map<String, Float> cardHover = new HashMap<>();
    private final Map<String, Float> cardActive = new HashMap<>();
    private final Map<String, Float> cardPress = new HashMap<>();
    private String selectedId = "";
    private float fixedWidth;
    private float fixedHeight;
    private float scrollOffset;
    private float targetScrollOffset;
    AddonPanelAnims anims() { return anims; }
    AddonScanState scanState() { return scanState; }
    Map<String, Float> cardHover() { return cardHover; }
    Map<String, Float> cardActive() { return cardActive; }
    Map<String, Float> cardPress() { return cardPress; }
    String selectedId() { return selectedId; }
    void setSelectedId(String id) { selectedId = id == null ? "" : id; }
    float fixedWidth() { return fixedWidth; }
    float fixedHeight() { return fixedHeight; }
    float scrollOffset() { return scrollOffset; }
    void setStatus(String text) { scanState.setStatus(text); }
    boolean statusVisible() { return scanState.statusVisible(); }
    String status() { return scanState.status(); }
    void updateUiMetrics(Minecraft mc, int width, int height) {
        if (mc == null) { fixedWidth = width; fixedHeight = height; return; }
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        fixedWidth = Math.max(1f, HudScale.virtualWidth(fbw, fbh));
        fixedHeight = Math.max(1f, HudScale.virtualHeight(fbw, fbh));
    }
    AddonUiMetrics.Layout layout() {
        float totalW = AddonUiMetrics.LEFT_W + AddonUiMetrics.GAP + AddonUiMetrics.RIGHT_W;
        float totalH = AddonUiMetrics.LEFT_TOP_H + AddonUiMetrics.GAP + AddonUiMetrics.LEFT_BOTTOM_H;
        float topY = fixedHeight * 0.5f - totalH * 0.5f;
        float leftX = fixedWidth * 0.5f - totalW * 0.5f;
        return new AddonUiMetrics.Layout(leftX, topY, leftX + AddonUiMetrics.LEFT_W + AddonUiMetrics.GAP, topY + AddonUiMetrics.LEFT_TOP_H + AddonUiMetrics.GAP);
    }
    float bodyY(AddonUiMetrics.Layout l) { return l.topY() + 28f * AddonUiMetrics.SCALE; }
    float listHeight(AddonUiMetrics.Layout l) { return Math.max(1f, AddonUiMetrics.RIGHT_H - 31f * AddonUiMetrics.SCALE); }
    void ensureSelection() {
        List<AddonSnapshot> addons = AddonManager.snapshots();
        if (addons.isEmpty()) { selectedId = ""; return; }
        if (selected(addons) == null) selectedId = addons.get(0).id();
    }
    AddonSnapshot selected(List<AddonSnapshot> addons) {
        for (AddonSnapshot addon : addons) if (addon.id().equals(selectedId)) return addon;
        return null;
    }
    void clampScroll() {
        List<AddonSnapshot> addons = AddonManager.snapshots();
        float listW = AddonUiMetrics.RIGHT_W - 10f * AddonUiMetrics.SCALE;
        int columns = AddonManagerOps.listColumns(addons.size(), listW);
        int rows = AddonManagerOps.rowsFor(addons.size(), columns);
        float listH = listHeight(layout());
        float contentH = Math.max(0f, rows * AddonUiMetrics.CARD_H + Math.max(0, rows - 1) * AddonUiMetrics.CARD_GAP);
        float max = Math.max(0f, contentH - listH);
        targetScrollOffset = Mth.clamp(targetScrollOffset, 0f, max);
        scrollOffset = Mth.clamp(scrollOffset, 0f, max);
    }
    boolean scrollBy(float mouseX, float mouseY, float amount) {
        AddonUiMetrics.Layout l = layout();
        float listX = l.rightX() + 5f * AddonUiMetrics.SCALE;
        float listY = bodyY(l);
        float listW = AddonUiMetrics.RIGHT_W - 10f * AddonUiMetrics.SCALE;
        float listH = listHeight(l);
        if (!AddonUiMetrics.inside(mouseX, mouseY, listX, listY, listW, listH)) return false;
        targetScrollOffset -= amount * (25f * AddonUiMetrics.SCALE);
        clampScroll();
        return true;
    }
    boolean handleClick(float mouseX, float mouseY, Runnable closeToParent) {
        AddonUiMetrics.Layout l = layout();
        List<AddonSnapshot> addons = AddonManager.snapshots();
        AddonSnapshot selected = selected(addons);
        float x = l.leftX() + 5f * AddonUiMetrics.SCALE;
        float y = l.bottomY() + 6f * AddonUiMetrics.SCALE;
        float w = AddonUiMetrics.LEFT_W - 10f * AddonUiMetrics.SCALE;
        float half = (w - AddonUiMetrics.GAP) * 0.5f;
        if (selected != null && AddonUiMetrics.inside(mouseX, mouseY, x, y, half, AddonUiMetrics.BUTTON_H)) {
            anims.pressToggle();
            boolean next = !selected.enabled();
            if (AddonManager.setEnabled(selected.id(), next)) setStatus("");
            return true;
        }
        if (AddonUiMetrics.inside(mouseX, mouseY, x + half + AddonUiMetrics.GAP, y, half, AddonUiMetrics.BUTTON_H)) {
            anims.pressScan(); scanState.scan(); return true;
        }
        if (AddonUiMetrics.inside(mouseX, mouseY, x, y + 17f * AddonUiMetrics.SCALE, half, AddonUiMetrics.BUTTON_H)) {
            anims.pressMods();
            if (AddonManagerOps.openPath(FabricLoader.getInstance().getGameDir().resolve("mods"), this)) setStatus(AddonUiMetrics.tr("screen.lovisual.addon_manager.status.opened_mods"));
            return true;
        }
        if (AddonUiMetrics.inside(mouseX, mouseY, x + half + AddonUiMetrics.GAP, y + 17f * AddonUiMetrics.SCALE, half, AddonUiMetrics.BUTTON_H)) {
            anims.pressConfig();
            if (AddonManagerOps.openPath(FabricLoader.getInstance().getConfigDir().resolve("lovisual").resolve("addons"), this)) setStatus(AddonUiMetrics.tr("screen.lovisual.addon_manager.status.opened_config"));
            return true;
        }
        if (AddonUiMetrics.inside(mouseX, mouseY, x, y + 34f * AddonUiMetrics.SCALE, w, AddonUiMetrics.BUTTON_H)) {
            anims.pressBack(); closeToParent.run(); return true;
        }
        float listX = l.rightX() + 5f * AddonUiMetrics.SCALE;
        float listY = bodyY(l);
        float listW = AddonUiMetrics.RIGHT_W - 10f * AddonUiMetrics.SCALE;
        float listH = listHeight(l);
        int columns = AddonManagerOps.listColumns(addons.size(), listW);
        float cardW = AddonManagerOps.cardWidth(listW, columns);
        if (!AddonUiMetrics.inside(mouseX, mouseY, listX, listY, listW, listH)) return true;
        for (int i = 0; i < addons.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float cardX = listX + col * (cardW + AddonUiMetrics.CARD_GAP);
            float cardY = listY + row * (AddonUiMetrics.CARD_H + AddonUiMetrics.CARD_GAP) - scrollOffset;
            if (AddonUiMetrics.inside(mouseX, mouseY, cardX, cardY, cardW, AddonUiMetrics.CARD_H)) {
                selectedId = addons.get(i).id(); cardPress.put(selectedId, 1f); setStatus(""); return true;
            }
        }
        return true;
    }
    void updateAnimations(float mouseX, float mouseY) {
        float dt = AnimationUtility.deltaTime();
        AddonUiMetrics.Layout l = layout();
        anims.updateHover(mouseX, mouseY, l);
        scanState.tickStatusExpiry();
        List<AddonSnapshot> addons = AddonManager.snapshots();
        Set<String> keys = new HashSet<>();
        float listX = l.rightX() + 5f * AddonUiMetrics.SCALE;
        float listY = bodyY(l);
        float listW = AddonUiMetrics.RIGHT_W - 10f * AddonUiMetrics.SCALE;
        int columns = AddonManagerOps.listColumns(addons.size(), listW);
        float cardW = AddonManagerOps.cardWidth(listW, columns);
        for (int i = 0; i < addons.size(); i++) {
            AddonSnapshot addon = addons.get(i);
            keys.add(addon.id());
            int col = i % columns;
            int row = i / columns;
            float cardX = listX + col * (cardW + AddonUiMetrics.CARD_GAP);
            float cardY = listY + row * (AddonUiMetrics.CARD_H + AddonUiMetrics.CARD_GAP) - scrollOffset;
            boolean hover = AddonUiMetrics.inside(mouseX, mouseY, cardX, cardY, cardW, AddonUiMetrics.CARD_H);
            cardHover.put(addon.id(), AddonUiMetrics.animate(cardHover.getOrDefault(addon.id(), 0f), hover ? 1f : 0f, dt, 10f));
            cardActive.put(addon.id(), AddonUiMetrics.animate(cardActive.getOrDefault(addon.id(), 0f), addon.id().equals(selectedId) ? 1f : 0f, dt, 8f));
            cardPress.put(addon.id(), AddonUiMetrics.animate(cardPress.getOrDefault(addon.id(), 0f), 0f, dt, 7f));
        }
        AddonUiMetrics.prune(cardHover, keys);
        AddonUiMetrics.prune(cardActive, keys);
        AddonUiMetrics.prune(cardPress, keys);
        scrollOffset = AddonUiMetrics.animate(scrollOffset, targetScrollOffset, dt, 12f);
    }
}
