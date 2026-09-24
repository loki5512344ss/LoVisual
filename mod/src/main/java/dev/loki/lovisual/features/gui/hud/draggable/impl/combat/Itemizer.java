package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorX;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorY;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

@HudElementRegister(order = 200)
public final class Itemizer extends DraggableHudElement {
    static final String LAYOUT_ID = "lovisual:api/hud/draggable/itemizer";
    static final float BASE_ICON_CARD = 24.0f;
    static final float BASE_COMPACT_W = 72.0f;
    static final float BASE_COMPACT_H = 24.0f;
    static final float BASE_GAP = 4.0f;
    static final float DEFAULT_DURATION = 1.15f;
    static final int MAX_INTERNAL_ENTRIES = 8;

    public static Itemizer INSTANCE;

    final CachedUiScriptRuntime runtime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());
    final LinkedHashMap<ItemizerEvent, Entry> entries = new LinkedHashMap<>();
    final List<ItemRenderTask> itemTasks = new java.util.ArrayList<>(5);
    final dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);

    final NumberValue<Double> scale = num("itemizer_scale", "scale", 1.0, 0.5, 3.0);
    final EnumValue<ItemizerDisplayMode> displayMode = enumSetting("itemizer_display_mode", "display_mode", ItemizerDisplayMode.ICONS, ItemizerDisplayMode.values());
    final EnumValue<ItemizerDirection> direction = enumSetting("itemizer_direction", "direction", ItemizerDirection.HORIZONTAL, ItemizerDirection.values());
    final NumberValue<Double> duration = num("itemizer_duration", "duration", (double) DEFAULT_DURATION, 0.25, 3.0);
    final NumberValue<Integer> maxEntries = num("itemizer_max_entries", "max_entries", 3, 1, 5);
    final BooleanValue showAutoEat = bool("itemizer_auto_eat", "auto_eat", true);
    final BooleanValue showAutoTotem = bool("itemizer_auto_totem", "auto_totem", true);
    final BooleanValue showElytraSwap = bool("itemizer_elytra_swap", "elytra_swap", true);
    final BooleanValue blur = bool("itemizer_blur", "blur", true);
    final NumberValue<Integer> bgAlpha = num("itemizer_bg_alpha", "bg_alpha", 168, 0, 255);

    float visibilityAnim;
    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    boolean foregroundReady;

    public Itemizer() {
        super("itemizer", "Itemizer", true);
        INSTANCE = this;
        defaultLayout(-16.0f, 48.0f, "CENTER", "CENTER");
    }

    public static void showAutoEat(ItemStack stack) { show(ItemizerEvent.AUTO_EAT, stack, "Eat"); }
    public static void showAutoTotem(ItemStack stack) { show(ItemizerEvent.AUTO_TOTEM, stack, "Totem"); }
    public static void showElytraSwap(ItemStack stack) { show(ItemizerEvent.ELYTRA_SWAP, stack, "Swap"); }
    public static void hideElytraSwap() { hide(ItemizerEvent.ELYTRA_SWAP); }

    private static void show(ItemizerEvent event, ItemStack stack, String label) {
        Itemizer itemizer = INSTANCE;
        if (itemizer == null || event == null || stack == null || stack.isEmpty()) return;
        itemizer.push(event, stack, label);
    }

    private static void hide(ItemizerEvent event) {
        Itemizer itemizer = INSTANCE;
        if (itemizer == null || event == null) return;
        itemizer.entries.remove(event);
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        float w = ItemizerModel.baseEntryWidth(this) * scale.get().floatValue();
        float h = ItemizerModel.baseEntryHeight(this) * scale.get().floatValue();
        this.x = (screenW - w) * 0.5f;
        this.y = screenH * 0.5f + 48.0f;
        setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, this.x, this.y);
    }

    @Override public boolean usesEngineRenderer() { return true; }
    @Override public HudPhase getHudPhase() { return HudPhase.LAST; }
    @Override public int getRenderOrder() { return 82; }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        foregroundReady = false;
        itemTasks.clear();
        boolean forceVisible = DraggableHudElementRegistry.isForceVisible();
        if (!isEnabled() && !forceVisible) { width = 0.0f; height = 0.0f; return; }
        float dt = AnimationUtility.deltaTime();
        List<Entry> visible = forceVisible && entries.isEmpty() ? ItemizerModel.previewEntries(this) : ItemizerModel.updateEntries(this, dt);
        boolean showWidget = !visible.isEmpty();
        visibilityAnim = HudRenderUtil.animateVisibility(visibilityAnim, showWidget);
        if (!showWidget && visibilityAnim <= 0.0f) { width = 0.0f; height = 0.0f; return; }
        float baseScale = scale.get().floatValue();
        int count = Math.max(1, visible.size());
        float entryW = ItemizerModel.baseEntryWidth(this) * baseScale;
        float entryH = ItemizerModel.baseEntryHeight(this) * baseScale;
        float gap = BASE_GAP * baseScale;
        float targetWidth = direction.get() == ItemizerDirection.HORIZONTAL ? entryW * count + gap * Math.max(0, count - 1) : entryW;
        float targetHeight = direction.get() == ItemizerDirection.HORIZONTAL ? entryH : entryH * count + gap * Math.max(0, count - 1);
        displayWidth = HudRenderUtil.animateDimension(displayWidth, targetWidth);
        displayHeight = HudRenderUtil.animateDimension(displayHeight, targetHeight);
        width = displayWidth; height = displayHeight;
        float drawScale = HudRenderUtil.visibilityScale(visibilityAnim);
        float drawWidth = displayWidth * drawScale;
        float drawHeight = displayHeight * drawScale;
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) return;
        float drawX = x + (displayWidth - drawWidth) * 0.5f;
        float drawY = y + (displayHeight - drawHeight) * 0.5f;
        float drawBaseScale = baseScale * drawScale;
        ItemizerRenderer.drawCards(this, renderer, visible, drawX, drawY, drawBaseScale, AnimationUtility.clamp01(visibilityAnim));
        UiScriptModule module = ItemizerProps.ensureModule(this);
        if (module == null) return;
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        LinkedHashMap<String, Object> props = ItemizerProps.props(this, visible, drawWidth, drawHeight, drawBaseScale, drawScale);
        long treeSignature = ItemizerProps.signature(this, props);
        long layoutSignature = 0xcbf29ce484222325L;
        layoutSignature = CachedUiScriptRuntime.mix(layoutSignature, drawX);
        layoutSignature = CachedUiScriptRuntime.mix(layoutSignature, drawY);
        layoutSignature = CachedUiScriptRuntime.mix(layoutSignature, drawWidth);
        layoutSignature = CachedUiScriptRuntime.mix(layoutSignature, drawHeight);
        UiRuntime baked = runtime.bake(moduleHandle, module, "itemizer", treeSignature, layoutSignature, drawWidth, drawHeight, fallback, drawX, drawY, drawWidth, drawHeight, () -> props);
        if (baked == null) return;
        baked.render(new UiRenderContext(renderer, fallback, ctx, tickDelta, UiProjectionMode.CURRENT));
        foregroundReady = true;
    }

    @Override
    public void renderEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        if (!foregroundReady || itemTasks.isEmpty()) { itemTasks.clear(); return; }
        int seed = 0;
        for (ItemRenderTask task : itemTasks) {
            ItemizerRenderer.drawItem(renderer, task.stack(), task.x(), task.y(), task.scale(), seed++, task.overlayFlags(), task.alpha());
        }
        itemTasks.clear();
        foregroundReady = false;
    }

    void push(ItemizerEvent event, ItemStack stack, String label) { ItemizerModel.push(this, event, stack, label); }

    void setSize(float w, float h) { setBounds(getX(), getY(), w, h); }

    public enum ItemizerEvent implements EnumValue.IdProvider {
        AUTO_EAT("auto_eat", "Eat"), AUTO_TOTEM("auto_totem", "Totem"), ELYTRA_SWAP("elytra_swap", "Swap");
        private final String id; final String fallbackLabel;
        ItemizerEvent(String id, String fallbackLabel) { this.id = id; this.fallbackLabel = fallbackLabel; }
        @Override public String id() { return id; }
    }

    enum ItemizerDisplayMode implements EnumValue.IdProvider {
        ICONS("icons"), COMPACT("compact");
        private final String id; ItemizerDisplayMode(String id) { this.id = id; }
        @Override public String id() { return id; }
    }

    enum ItemizerDirection implements EnumValue.IdProvider {
        HORIZONTAL("horizontal"), VERTICAL("vertical");
        private final String id; ItemizerDirection(String id) { this.id = id; }
        @Override public String id() { return id; }
    }

    static final class Entry {
        final ItemizerEvent event;
        ItemStack stack = ItemStack.EMPTY;
        String label;
        float life; float totalLife; float anim; float punch;
        Entry(ItemizerEvent event) { this.event = event; this.label = event.fallbackLabel; }
    }

    record ItemRenderTask(ItemStack stack, float x, float y, float scale, int overlayFlags, float alpha) {}
}
