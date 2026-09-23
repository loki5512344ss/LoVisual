package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.util.TickDelta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.util.Util;
import java.util.List;

@HudElementRegister(order = 95)
public final class CustomTabList extends AbstractHudElement {
    public static final CustomTabList INSTANCE = new CustomTabList();
    private static final float TOP_Y = 56f;
    private static final long STRUCTURE_PROBE_INTERVAL_MS = 250L;
    private static final long DYNAMIC_REFRESH_INTERVAL_MS = 500L;
    private final TabListAnimator animator = new TabListAnimator();
    private final TabListPalette palette = new TabListPalette();
    private final NumberValue<Integer> maxWidth = num("tab_list_max_width", "max_width", 1680, 760, 2600);
    private final BooleanValue syncTheme = bool("tab_list_sync_theme", "sync_theme", true);
    private final NumberValue<Integer> themeAlpha = visibleWhen(num("tab_list_theme_alpha", "theme_alpha", 225, 0, 255), syncTheme::get);
    private final RGBAColorValue bgColor = visibleWhen(color("tab_list_bg", "background", "#E50B1018"), () -> !syncTheme.get());
    private final RGBColorValue accentColor = visibleWhen(colorNoAlpha("tab_list_accent", "accent", "#6E8DFF"), () -> !syncTheme.get());
    private final RGBColorValue textColor = visibleWhen(colorNoAlpha("tab_list_text", "text", "#F4F7FB"), () -> !syncTheme.get());
    private final RGBColorValue mutedColor = visibleWhen(colorNoAlpha("tab_list_muted", "muted", "#9AA5B5"), () -> !syncTheme.get());
    private TabListModel.Snapshot snapshot = TabListModel.Snapshot.empty();
    private long lastStructureProbeMs;
    private long lastDynamicRefreshMs;
    private long lastStructureSignature = Long.MIN_VALUE;
    private int lastSnapshotWidth = -1;
    private boolean wasShown;

    private CustomTabList() { super("tab_list", "Tab List", true); }

    public static boolean shouldReplaceVanilla() {
        return Lifecycle.canRunHud() && INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.canRenderReplacement();
    }
    public static void renderVanillaTabStratum(GuiGraphicsExtractor ctx, int guiWidth) {
        CustomTabList i = INSTANCE; if (i == null) return; i.renderVanillaTabStratumInternal(ctx, guiWidth);
    }
    private void renderVanillaTabStratumInternal(GuiGraphicsExtractor ctx, int guiWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (!Lifecycle.canRunHud()) return;
        if (mc == null || mc.getWindow() == null || ctx == null) return;
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(mc.getWindow().getWidth(), mc.getWindow().getHeight())));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(mc.getWindow().getWidth(), mc.getWindow().getHeight())));
        LoVisualRenderSystem.ensureFrameContext();
        LoVisualRenderSystem.updateFrameTiming(TickDelta.tickProgress(false), TickDelta.frameDeltaTicks(), TickDelta.fixedDeltaTicks());
        ViewportContext.beginCurrentStratumUnscaledLogical(ctx);
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.SCREEN_TOP, "2d:vanilla_stratum:tab_list")) {
            Renderer2D.COLOR.begin(); renderTab(Renderer2D.COLOR, ctx, TickDelta.tickProgress(false), screenW, screenH, true); Renderer2D.COLOR.render();
        } finally { ViewportContext.endCurrentStratum(ctx); }
    }
    @Override public boolean usesEngineRenderer() { return true; }
    @Override public HudRenderSpace getRenderSpace() { return HudRenderSpace.UNSCALED_LOGICAL; }
    @Override public HudPhase getHudPhase() { return HudPhase.NONE; }
    @Override public int getRenderOrder() { return 1000; }
    @Override public void renderEngineForeground(Renderer2D renderer, dev.loki.lovisual.render.engine.text.render.TextRenderer tr, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        renderTab(renderer, ctx, tickDelta, screenW, screenH, isEnabled() && shouldShowNow());
    }
    private void renderTab(Renderer2D renderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH, boolean show) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || ctx == null || renderer == null) return;
        float panelPresence = animator.updatePanel(show);
        if (!show && panelPresence <= 0.01f && !animator.hasVisibleRows()) { setBounds(0f, 0f, 0f, 0f); return; }
        if (show) refreshSnapshot(screenW, !wasShown);
        wasShown = show; animator.updateRows(show ? snapshot.visibleIds() : null);
        float panelAlpha = AnimationUtility.easeOutCubic(panelPresence);
        float width = tabPanelWidth(screenW);
        float height = snapshot.height();
        float drawX = (screenW - width) * 0.5f;
        float drawY = TOP_Y - (1f - panelAlpha) * 18f;
        setBounds(drawX, drawY, width, height);
        palette.update(syncTheme.get(), themeAlpha.get(), bgColor.getArgb(), accentColor.getArgb(), textColor.getArgb(), mutedColor.getArgb());
        if (panelAlpha <= 0.01f) return;
        TabListShellRenderer.renderChrome(renderer, drawX, drawY, width, height, panelAlpha, snapshot, palette);
        List<TabListRowRenderer.RowRender> rows = TabListRowRenderer.collect(drawX, drawY, width, panelAlpha, snapshot, animator, palette);
        TabListRowRenderer.renderRows(renderer, rows, palette);
        TabListTextRenderer.renderHeadsAndText(ctx, renderer, drawX, drawY, width, panelAlpha, snapshot, animator, palette);
    }
    private void refreshSnapshot(int screenW, boolean force) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return;
        long now = Util.getMillis();
        int listedPlayers = Math.min(mc.getConnection().getListedOnlinePlayers().size(), TabListModel.maxPlayers());
        boolean multiWide = listedPlayers > TabListModel.maxRowsPerColumn() * 2;
        int effectiveWidth = Math.max(1, Math.min(screenW, multiWide ? screenW : maxWidth.get()));
        boolean needCollect = force || snapshot.entries().isEmpty() || snapshot.entries().size() != listedPlayers || effectiveWidth != lastSnapshotWidth;
        long signature = lastStructureSignature;
        if (needCollect || now - lastStructureProbeMs >= STRUCTURE_PROBE_INTERVAL_MS) {
            signature = TabListModel.structureSignature(mc, effectiveWidth);
            lastStructureProbeMs = now;
            if (signature != lastStructureSignature) needCollect = true;
        }
        if (needCollect) { snapshot = TabListModel.collect(mc, effectiveWidth); lastSnapshotWidth = effectiveWidth; lastStructureSignature = signature; lastDynamicRefreshMs = now; return; }
        if (now - lastDynamicRefreshMs >= DYNAMIC_REFRESH_INTERVAL_MS) { snapshot = TabListModel.refreshDynamic(mc, snapshot); lastDynamicRefreshMs = now; }
    }
    private float tabPanelWidth(int screenW) {
        float screenCap = Math.max(360f, screenW - 24f);
        float configured = Math.max(1f, maxWidth.get());
        float wanted = snapshot.width();
        if (snapshot.columns() > 2) configured = Math.max(configured, wanted);
        return Math.min(screenCap, Math.min(configured, wanted));
    }
    private boolean canRenderReplacement() {
        Minecraft mc = Minecraft.getInstance(); return mc != null && mc.player != null && mc.getConnection() != null;
    }
    private boolean shouldShowNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getConnection() == null || mc.options == null) return false;
        if (!mc.options.keyPlayerList.isDown()) return false;
        int listedPlayers = mc.getConnection().getListedOnlinePlayers().size();
        if (listedPlayers <= 0) return false;
        if (!mc.isLocalServer() || listedPlayers > 1) return true;
        Objective listObjective = null;
        if (mc.level != null) { Scoreboard sb = mc.level.getScoreboard(); if (sb != null) listObjective = sb.getDisplayObjective(DisplaySlot.LIST); }
        return listObjective != null;
    }
}
