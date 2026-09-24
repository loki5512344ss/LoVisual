package dev.loki.lovisual.features.module.modules.visuals.esp.overlay;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.mixininterface.entity.IEntity;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.text.glyph.VanillaTextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import dev.loki.lovisual.render.helpers.clip.ScreenSpaceOverlay2D;
import dev.loki.lovisual.util.item.food.IllegalItemUtil;
import dev.loki.lovisual.util.item.rarity.RarityColorUtil;
import dev.loki.lovisual.util.item.enchant.TopEnchantUtil;

//todo Description
@ModuleInfo(id = "dropesp", displayName = "DropESP", category = ModuleCategory.VISUALS)
public class DropESP extends Module {

    private static final String SETTING_LIMIT_COMMON_DISTANCE = "limit_common_distance";
    private static final String SETTING_COMMON_MAX_DISTANCE = "common_max_distance";
    private static final String SETTING_SPECIAL_ITEMS_COLOR = "special_items_color";
    private static final String SETTING_ILLEGAL_ENCHANT_COLOR = "illegal_enchant_color";
    private static final String SETTING_TOP_ENCHANT_IGNORE_LIST = "top_enchant_ignore_list";
    private static final String SETTING_SPECIAL_ITEM_IDS = "special_item_ids";
    private static final String SETTING_MODE = "mode";
    private static final String SETTING_TEXT_SHADOW = "text_shadow";
    private static final String SETTING_FRAME = "frame";
    private static final String SETTING_ITEM_ICON = "item_icon";
    private static final double ITEM_BOX_EXPAND_XZ = 0.05;
    private static final double ITEM_BOX_EXPAND_TOP = 0.15;
    private final ModeValue modeValue = modeSetting("dropEspMode", SETTING_MODE, "Matte", "Vanilla", "New", "Matte");
    private final BooleanValue limitCommonDistanceValue = bool("dropEspLimitCommonDistance", SETTING_LIMIT_COMMON_DISTANCE, true);
    private final NumberValue<Integer> commonMaxDistanceValue =
            visibleWhen(num("dropEspCommonMaxDistance", SETTING_COMMON_MAX_DISTANCE, 32, 4, 256), limitCommonDistanceValue::get);
    private final BooleanValue textShadowValue =
            visibleWhen(bool("dropEspTextShadow", SETTING_TEXT_SHADOW, true), this::isNewMode);
    private final BooleanValue frameValue =
            visibleWhen(bool("dropEspFrame", SETTING_FRAME, true), this::isOverlayMode);
    private final BooleanValue itemIconValue =
            visibleWhen(bool("dropEspItemIcon", SETTING_ITEM_ICON, true), this::isMatteMode);
    private final RGBColorValue specialColorValue = colorNoAlpha("dropEspSpecialColor", SETTING_SPECIAL_ITEMS_COLOR, "#FFAA00");
    private final ItemIdSetValue topIgnore = TopEnchantUtil.ignoreValue();
    private final ItemIdSetValue specialItemsValue =
            itemList("dropEspSpecialItems", SETTING_SPECIAL_ITEM_IDS, TextListSetting.PickerMode.ITEMS);

    {
        setting(SettingDef.colorNoAlpha(SETTING_ILLEGAL_ENCHANT_COLOR, IllegalItemUtil.illegalColorValue()));
        setting(SettingDef.textList(SETTING_TOP_ENCHANT_IGNORE_LIST, topIgnore, TextListSetting.PickerMode.ENCHANTMENTS));
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.FIRST;
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        boolean matteMode = isMatteMode();
        boolean overlayMode = isOverlayMode();
        TextRenderer labelRenderer = overlayMode ? ScreenSpaceOverlay2D.labelRenderer(textRenderer) : VanillaTextRenderer.INSTANCE;
        boolean measureStarted = false;
        java.util.List<DropEspOverlayRenderer.DropOverlayEntry> overlayEntries = new java.util.ArrayList<>();
        java.util.List<DropEspOverlayRenderer.DropVanillaEntry> vanillaEntries = new java.util.ArrayList<>();
        double textScale = overlayMode ? ScreenSpaceOverlay2D.TEXT_SCALE : ScreenSpaceOverlay2D.VANILLA_TEXT_SCALE;

        if (!labelRenderer.isBuilding()) {
            labelRenderer.begin(textScale, true, false);
            measureStarted = true;
        }

        Vec3 cameraPos = mc.player.getEyePosition(tickDelta);
        for (ItemEntity item : mc.level.getEntitiesOfClass(
                ItemEntity.class,
                mc.player.getBoundingBox().inflate(64),
                e -> true
        )) {

            Vec3 pos = obtainEntityLerpedPos(item, tickDelta);

            double distSq = pos.distanceToSqr(cameraPos);
            double dist = Math.sqrt(distSq);
            if (!passesDistanceFilter(item, dist)) continue;

            ItemStack stack = item.getItem();
            String text = stack.getHoverName().getString() + " x" + stack.getCount();
            int color = resolveDisplayColor(stack);
            int sortPriority = resolveSortPriority(stack);

            if (overlayMode) {
                ScreenSpaceOverlay2D.ScreenRect rect = ScreenSpaceOverlay2D.projectEntityBox(
                        item,
                        pos,
                        tickDelta,
                        ITEM_BOX_EXPAND_XZ,
                        ITEM_BOX_EXPAND_TOP
                );
                if (rect == null) continue;

                DropEspOverlayRenderer.DropLabelEntry matteLabel = matteMode
                        ? DropEspOverlayRenderer.createMatteDropLabel(labelRenderer, stack, text, color, rect, itemIconValue.get())
                        : null;
                ScreenSpaceOverlay2D.LabelEntry label = matteMode
                        ? null
                        : ScreenSpaceOverlay2D.createCenteredLabel(labelRenderer, text, color, rect);
                overlayEntries.add(new DropEspOverlayRenderer.DropOverlayEntry(sortPriority, distSq, rect, color, matteLabel, label));
            } else {
                AABB box = item.getBoundingBox().move(
                        pos.x - item.getX(),
                        pos.y - item.getY(),
                        pos.z - item.getZ()
                );

                Vec3 center = new Vec3(
                        (box.minX + box.maxX) * 0.5,
                        box.minY + (box.maxY - box.minY) * 0.6,
                        (box.minZ + box.maxZ) * 0.5
                );

                Vec3 screen = ScreenProjection.worldToScreen(center, tickDelta);
                if (screen == null) continue;

                double x = screen.x - (labelRenderer.getWidth(text, true) / 2.0);
                double y = screen.y;
                vanillaEntries.add(new DropEspOverlayRenderer.DropVanillaEntry(sortPriority, distSq, ScreenSpaceOverlay2D.labelAt(text, x, y, color)));
            }
        }

        if (measureStarted) {
            labelRenderer.end();
        }
        if (overlayEntries.isEmpty() && vanillaEntries.isEmpty()) return;

        overlayEntries.sort(DropEspOverlayRenderer::compareDropRenderOrder);
        vanillaEntries.sort(DropEspOverlayRenderer::compareDropRenderOrder);

        if (overlayMode) {
            for (DropEspOverlayRenderer.DropOverlayEntry entry : overlayEntries) {
                DropEspOverlayRenderer.renderOverlayEntryPass(renderer, labelRenderer, entry, matteMode, textScale, frameValue.get(), textShadowValue.get());
            }
        } else {
            for (DropEspOverlayRenderer.DropVanillaEntry entry : vanillaEntries) {
                DropEspOverlayRenderer.renderSingleLabel(labelRenderer, entry.label(), true, textScale);
                Renderer2D.flushBatch(Renderer2D.FlushReason.EXPLICIT);
            }
        }
    }

    private boolean passesDistanceFilter(ItemEntity item, double dist) {

        String id = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString().toLowerCase();

        if (specialItemsValue.get().contains(id)) {
            return true;
        }

        if (limitCommonDistanceValue.get()) {

            if (item.getItem().isEnchanted()) {
                return true;
            }

            var rarity = item.getItem().getOrDefault(
                    DataComponents.RARITY,
                    Rarity.COMMON
            );

            if (rarity == Rarity.COMMON) {
                return dist <= commonMaxDistanceValue.get();
            }
        }

        return true;
    }

    @Override
    public java.util.List<ConfigValue<?>> getConfigValues() {
        java.util.List<ConfigValue<?>> list = new java.util.ArrayList<>(super.getConfigValues());
        list.add(TopEnchantUtil.topColorValue());
        return list;
    }

    private boolean isNewMode() {
        return "New".equalsIgnoreCase(modeValue.get());
    }

    private boolean isMatteMode() {
        return "Matte".equalsIgnoreCase(modeValue.get());
    }

    private boolean isOverlayMode() {
        return isNewMode() || isMatteMode();
    }

    private int resolveSortPriority(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        if (IllegalItemUtil.isIllegal(stack)) {
            return 1000;
        }
        if (TopEnchantUtil.hasTopEnchant(stack)) {
            return 900;
        }
        if (specialItemsValue.get().contains(id)) {
            return 800;
        }

        Rarity rarity = stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        return switch (rarity) {
            case EPIC -> 400;
            case RARE -> 300;
            case UNCOMMON -> 200;
            case COMMON -> 100;
            default -> 100;
        };
    }

    private int resolveDisplayColor(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        if (IllegalItemUtil.isIllegal(stack)) {
            return IllegalItemUtil.illegalColor();
        }
        if (TopEnchantUtil.hasTopEnchant(stack)) {
            return TopEnchantUtil.topColor();
        }
        if (specialItemsValue.get().contains(id)) {
            return specialColorValue.getArgb();
        }
        float[] c = RarityColorUtil.INSTANCE.getRarityColor(stack);
        return rgbaToARGB(c);
    }

    private int rgbaToARGB(float[] c) {
        int r = (int) (c[0] * 255);
        int g = (int) (c[1] * 255);
        int b = (int) (c[2] * 255);
        int a = (int) (c[3] * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private Vec3 obtainEntityLerpedPos(Entity e, float f) {
        try {
            return e.getPosition(f);
        } catch (NoSuchMethodError ex) {
            if (e instanceof IEntity access) {
                Vec3 last = access.get$InstantRenderPos();
                return last.lerp(e.position(), f);
            }
            return e.position();
        }
    }
}
