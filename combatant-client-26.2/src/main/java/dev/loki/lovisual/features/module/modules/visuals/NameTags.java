/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.SettingDef;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.*;
import dev.loki.lovisual.features.gui.clickgui.settings.TextListSetting;
import dev.loki.lovisual.features.module.*;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.mixininterface.IEntity;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.ScreenProjection;
import dev.loki.lovisual.util.item.IllegalItemUtil;
import dev.loki.lovisual.util.item.TopEnchantUtil;
import dev.loki.lovisual.util.player.effect.StatusEffectView;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

//todo Description
@ModuleInfo(id = "nametags", displayName = "NameTags", aliases = {"nametag", "tags", "names"}, category = ModuleCategory.VISUALS)
public class NameTags extends Module {

    static final int OPPONENT_DISTANCE = 80;
    private static final int INFO_COLOR_DEFAULT = 0xFFFF8A8A;
    private static final float NAMEPLATE_EXTRA_LIFT = 8f;
    private static final ItemStack[] EMPTY_SLOTS = new ItemStack[0];

    private static final String SETTING_TOGGLES = "toggles";
    private static final String SETTING_DURABILITY_START = "durability_start_percent";
    private static final String SETTING_DISTANCE_TOGGLES = "distance_toggles";
    private static final String SETTING_NAMEPLATE_CUTOFF = "nameplate_cutoff";
    private static final String SETTING_SLOTS_CUTOFF = "slots_cutoff";
    private static final String SETTING_EFFECTS_CUTOFF = "effects_cutoff";
    private static final String SETTING_ARMOR_ENCHANTS = "armor_enchants";
    private static final String SETTING_MELEE_ENCHANTS = "melee_enchants";
    private static final String SETTING_TOOLS_ENCHANTS = "tools_enchants";
    private static final String SETTING_BOW_ENCHANTS = "bow_enchants";
    private static final String SETTING_FISHING_ENCHANTS = "fishing_enchants";
    private static final String SETTING_TRIDENT_ENCHANTS = "trident_enchants";
    private static final String SETTING_CROSSBOW_ENCHANTS = "crossbow_enchants";
    private static final String SETTING_MACE_ENCHANTS = "mace_enchants";
    private static final String SETTING_OTHER_ENCHANTS = "other_enchants";
    private static final String SETTING_ILLEGAL_ENCHANT_COLOR = "illegal_enchant_color";
    private static final String SETTING_ILLEGAL_ENCHANTS_ALWAYS = "illegal_enchants_always";
    private static final String SETTING_TOP_ENCHANT_COLOR = "top_enchant_color";
    private static final String SETTING_TOP_ENCHANT_IGNORE_LIST = "top_enchant_ignore_list";
    private static final String SETTING_HP_DECIMALS = "hp_decimals";
    private static final String SETTING_HP_COLOR_MODE = "hp_color_mode";
    private static final String SETTING_NAMEPLATE_ALPHA = "nameplate_alpha";
    private static final String SETTING_NAMEPLATE_STYLE = "nameplate_style";
    private static final String SETTING_PLAYER_HEAD = "player_head";
    private static final String SETTING_NAMEPLATE_EXTRAS = "nameplate_extras";
    private static final String SETTING_OPPONENT_APPLE_COOLDOWNS = "opponent_apple_cooldowns";
    private static final String SETTING_PVP_PREFIX = "pvp_prefix";
    private static final String SETTING_PVP_PREFIX_COLOR = "pvp_prefix_color";

    private final ItemIdSetValue topIgnore = TopEnchantUtil.ignoreValue();
    private final List<NameTagModel.RenderEntry> renderQueue = new ArrayList<>();
    private final List<Player> playerBuffer = new ArrayList<>();
    final Map<UUID, ItemStack[]> slotCache = new HashMap<>();
    final Map<UUID, NameTagModel.EffectCache> effectCache = new HashMap<>();
    final Map<UUID, NameTagModel.EnchantCache> enchantCache = new HashMap<>();
    final Map<UUID, Float> totemPopAnimations = new HashMap<>();
    final Map<UUID, Integer> totemPopLastCounts = new HashMap<>();
    final HashSet<UUID> seenIds = new HashSet<>();

    final BooleanMapValue toggles = group("nameTagsToggles", SETTING_TOGGLES, Map.of(
            "Show nameplate", true,
            "Show distance", true,
            "Show armor row", true,
            "Item durability bar", true,
            "Show effects", true,
            "Show self in 3rd person", false
    ));
    final NumberValue<Integer> durabilityStartPercent =
            visibleWhen(num("nameTagsDurabilityStartPercent", SETTING_DURABILITY_START, 99, 0, 100),
                    () -> toggles.get("Item durability bar"));
    private final BooleanMapValue distanceToggles =
            visibleWhen(group("nameTagsDistanceToggles", SETTING_DISTANCE_TOGGLES, Map.of(
                    "nameplate", true,
                    "slots", true,
                    "effects", true
            )), () -> toggles.get("Show nameplate") || toggles.get("Show armor row") || toggles.get("Show effects"));
    private final NumberValue<Integer> plateDistance =
            visibleWhen(num("nameTagsPlateDistance", SETTING_NAMEPLATE_CUTOFF, 80, 5, 256),
                    () -> toggles.get("Show nameplate"));
    private final NumberValue<Integer> slotsDistance =
            visibleWhen(num("nameTagsSlotsDistance", SETTING_SLOTS_CUTOFF, 70, 5, 256),
                    () -> toggles.get("Show armor row"));
    private final NumberValue<Integer> effectsDistance =
            visibleWhen(num("nameTagsEffectsDistance", SETTING_EFFECTS_CUTOFF, 90, 5, 256),
                    () -> toggles.get("Show effects"));
    final NumberValue<Integer> nameplateAlpha =
            visibleWhen(num("nameTagsNameplateAlpha", SETTING_NAMEPLATE_ALPHA, 180, 0, 255),
                    () -> toggles.get("Show nameplate"));
    final EnumValue<NameTagModel.NameplateStyle> nameplateStyle =
            visibleWhen(enumSetting("nameTagsNameplateStyle", SETTING_NAMEPLATE_STYLE, NameTagModel.NameplateStyle.LIQUID_GLASS, NameTagModel.NameplateStyle.values()),
                    () -> toggles.get("Show nameplate"));
    final BooleanValue playerHead =
            visibleWhen(bool("nameTagsPlayerHead", SETTING_PLAYER_HEAD, false),
                    () -> toggles.get("Show nameplate"));
    final BooleanMapValue nameplateExtras = visibleWhen(group("nameTagsNameplateExtras", SETTING_NAMEPLATE_EXTRAS, new LinkedHashMap<>() {{
        put("Tab names", false);
        put("Ping", false);
    }}), () -> toggles.get("Show nameplate"));
    final BooleanMapValue enchantArmor = group("nameTagsEnchantArmor", SETTING_ARMOR_ENCHANTS, Map.ofEntries(
            Map.entry("protection", true),
            Map.entry("fire_protection", false),
            Map.entry("feather_falling", false),
            Map.entry("blast_protection", false),
            Map.entry("projectile_protection", false),
            Map.entry("respiration", false),
            Map.entry("aqua_affinity", false),
            Map.entry("thorns", true),
            Map.entry("depth_strider", false),
            Map.entry("frost_walker", false),
            Map.entry("soul_speed", false),
            Map.entry("swift_sneak", false)
    ));
    final BooleanMapValue enchantMelee = group("nameTagsEnchantMelee", SETTING_MELEE_ENCHANTS, Map.ofEntries(
            Map.entry("sharpness", true),
            Map.entry("smite", false),
            Map.entry("bane_of_arthropods", false),
            Map.entry("knockback", true),
            Map.entry("fire_aspect", true),
            Map.entry("looting", true),
            Map.entry("sweeping_edge", false)
    ));
    final BooleanMapValue enchantTools = group("nameTagsEnchantTools", SETTING_TOOLS_ENCHANTS, Map.ofEntries(
            Map.entry("efficiency", true),
            Map.entry("silk_touch", true),
            Map.entry("fortune", true)
    ));
    final BooleanMapValue enchantBow = group("nameTagsEnchantBow", SETTING_BOW_ENCHANTS, Map.ofEntries(
            Map.entry("power", true),
            Map.entry("punch", true),
            Map.entry("flame", false),
            Map.entry("infinity", false)
    ));
    final BooleanMapValue enchantFishing = group("nameTagsEnchantFishing", SETTING_FISHING_ENCHANTS, Map.ofEntries(
            Map.entry("luck_of_the_sea", false),
            Map.entry("lure", false)
    ));
    final BooleanMapValue enchantTrident = group("nameTagsEnchantTrident", SETTING_TRIDENT_ENCHANTS, Map.ofEntries(
            Map.entry("loyalty", false),
            Map.entry("impaling", false),
            Map.entry("riptide", false),
            Map.entry("channeling", false)
    ));
    final BooleanMapValue enchantCrossbow = group("nameTagsEnchantCrossbow", SETTING_CROSSBOW_ENCHANTS, Map.ofEntries(
            Map.entry("multishot", false),
            Map.entry("quick_charge", false),
            Map.entry("piercing", true)
    ));
    final BooleanMapValue enchantMace = group("nameTagsEnchantMace", SETTING_MACE_ENCHANTS, Map.ofEntries(
            Map.entry("density", true),
            Map.entry("breach", false),
            Map.entry("wind_burst", true)
    ));
    final BooleanMapValue enchantOther = group("nameTagsEnchantOther", SETTING_OTHER_ENCHANTS, Map.ofEntries(
            Map.entry("mending", false),
            Map.entry("unbreaking", false),
            Map.entry("vanishing_curse", false),
            Map.entry("lunge", true)
    ));
    final BooleanValue illegalEnchantsAlways = bool("nameTagsIllegalEnchantsAlways", SETTING_ILLEGAL_ENCHANTS_ALWAYS, true);
    final BooleanValue opponentAppleCooldowns = bool("nameTagsOpponentAppleCooldowns", SETTING_OPPONENT_APPLE_COOLDOWNS, true);
    final BooleanValue totemPopCounter = TotemPopCounter.enabledValue();
    private final NumberValue<Integer> totemPopResetSeconds = TotemPopCounter.resetAfterSecondsValue();
    final BooleanValue pvpPrefix = bool("nameTagsPvpPrefix", SETTING_PVP_PREFIX, false);
    final RGBColorValue pvpPrefixColor =
            visibleWhen(colorNoAlpha("nameTagsPvpPrefixColor", SETTING_PVP_PREFIX_COLOR, "#FF0000"), pvpPrefix::get);
    final BooleanValue hpDecimals =
            visibleWhen(bool("nameTagsHpDecimals", SETTING_HP_DECIMALS, false),
                    () -> toggles.get("Show nameplate"));
    final ModeValue hpColorMode =
            visibleWhen(modeSetting("nameTagsHpColorMode", SETTING_HP_COLOR_MODE, "Static", "Static", "Gradient"),
                    () -> toggles.get("Show nameplate"));

    transient TextRenderer cachedNameTr;
    transient TextRenderer cachedHpTr;
    transient TextRenderer cachedLevelTr;
    transient TextRenderer cachedTimeTr;

    {
        setting(SettingDef.bool(totemPopCounter)
                .common(CommonSettingSchemas.TOTEM_POP_COUNTER.commonI18nKey()));
        setting(SettingDef.number(totemPopResetSeconds)
                .common(CommonSettingSchemas.TOTEM_POP_RESET_SECONDS.commonI18nKey())
                .visibleWhen(totemPopCounter::get));
        setting(SettingDef.colorNoAlpha(SETTING_ILLEGAL_ENCHANT_COLOR, IllegalItemUtil.illegalColorValue()));
        setting(SettingDef.colorNoAlpha(SETTING_TOP_ENCHANT_COLOR, TopEnchantUtil.topColorValue()));
        setting(SettingDef.textList(SETTING_TOP_ENCHANT_IGNORE_LIST, topIgnore, TextListSetting.PickerMode.ENCHANTMENTS));
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.AFTER_MISC_OVERLAYS;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float tickDelta) {
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        NameTagRenderer.ensureFontCache(this, textRenderer);

        StatusEffectView.sync(mc);
        TotemPopCounter.tick();
        float dt = AnimationUtility.deltaTime();

        renderQueue.clear();
        playerBuffer.clear();
        seenIds.clear();

        boolean wantNameplate = toggles.get("Show nameplate");
        boolean wantSlots = toggles.get("Show armor row");
        boolean wantEffects = toggles.get("Show effects");
        boolean wantOpponent = opponentAppleCooldowns.get() && NameTagOpponentRenderer.isOpponentAppleTrackingEnabled();
        if (!wantNameplate && !wantSlots && !wantEffects && !wantOpponent) return;

        boolean hasUnlimited = false;
        double maxCutoff = 0.0;
        if (wantNameplate) {
            if (distanceToggles.get("nameplate")) {
                maxCutoff = Math.max(maxCutoff, plateDistance.get());
            } else {
                hasUnlimited = true;
            }
        }
        if (wantSlots) {
            if (distanceToggles.get("slots")) {
                maxCutoff = Math.max(maxCutoff, slotsDistance.get());
            } else {
                hasUnlimited = true;
            }
        }
        if (wantEffects) {
            if (distanceToggles.get("effects")) {
                maxCutoff = Math.max(maxCutoff, effectsDistance.get());
            } else {
                hasUnlimited = true;
            }
        }
        if (wantOpponent) {
            double oppCutoff = OPPONENT_DISTANCE;
            if (oppCutoff > 0) {
                maxCutoff = Math.max(maxCutoff, oppCutoff);
            } else {
                hasUnlimited = true;
            }
        }

        Vec3 cameraPos = null;
        if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            cameraPos = mc.gameRenderer.mainCamera().position();
        }
        if (cameraPos == null && mc.player != null) {
            cameraPos = mc.player.position();
        }

        for (Player player : mc.level.players()) {
            if (!shouldRender(player)) continue;
            playerBuffer.add(player);
            seenIds.add(player.getUUID());
        }

        if (cameraPos != null && playerBuffer.size() > 1) {
            double camX = cameraPos.x;
            double camY = cameraPos.y;
            double camZ = cameraPos.z;
            playerBuffer.sort(Comparator.comparingDouble((Player p) -> p.distanceToSqr(camX, camY, camZ)).reversed());
        }

        for (Player player : playerBuffer) {
            Vec3 pos = obtainEntityLerpedPos(player, tickDelta);
            double dist = (cameraPos == null) ? 0.0 : cameraPos.distanceTo(pos);
            if (!hasUnlimited && maxCutoff > 0.0 && dist > maxCutoff) continue;

            AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
            Vec3 anchor = new Vec3(box.getCenter().x, box.maxY + 0.5, box.getCenter().z);
            NameTagModel.ScreenRect rect = projectBoxScreen(box, tickDelta);
            int centerX;
            int anchorY;
            if (rect != null) {
                centerX = (int) Math.round((rect.minX() + rect.maxX()) * 0.5);
                anchorY = (int) Math.round(rect.minY());
            } else {
                Vec3 screen = ScreenProjection.worldToScreen(anchor, tickDelta);
                if (screen == null) continue;
                centerX = (int) screen.x;
                anchorY = (int) screen.y;
            }
            float distOffset = NameTagLayoutEngine.computeDistanceOffset(dist);
            int nameY = NameTagLayoutEngine.computeNameY(rect, anchorY, distOffset);
            int sectionLift = Math.round(NAMEPLATE_EXTRA_LIFT) + NameTagLayoutEngine.computeEspNameSectionLift(player);
            nameY -= sectionLift;

            boolean renderSlots = wantSlots;
            if (renderSlots && distanceToggles.get("slots") && dist > slotsDistance.get()) renderSlots = false;

            boolean renderNameplate = wantNameplate;
            if (renderNameplate && distanceToggles.get("nameplate") && dist > plateDistance.get())
                renderNameplate = false;

            boolean renderEffects = wantEffects;
            if (renderEffects && distanceToggles.get("effects") && dist > effectsDistance.get()) renderEffects = false;

            TotemPopSnapshot totemPopSnapshot = TotemPopCounter.snapshot(player.getUUID());
            float totemPopAnimation = NameTagCache.updateTotemPopAnimation(this, player, totemPopSnapshot, dt);
            int totemPopCount = NameTagCache.resolveTotemPopCount(this, player, totemPopSnapshot, totemPopAnimation);

            NameTagModel.LabelInfo labelInfo = renderNameplate ? NameTagLabelBuilder.buildLabelInfo(this, player) : null;
            int infoColor = labelInfo != null ? labelInfo.infoColor() : INFO_COLOR_DEFAULT;
            NameTagModel.NameplateLayout nameplateLayout = null;
            if (renderNameplate && labelInfo != null) {
                nameplateLayout = NameTagLayoutEngine.buildLayout(
                        this,
                        player,
                        cachedNameTr != null ? cachedNameTr : textRenderer,
                        cachedHpTr != null ? cachedHpTr : textRenderer,
                        labelInfo,
                        dist,
                        centerX,
                        nameY,
                        totemPopCount,
                        totemPopAnimation
                );
            }

            ItemStack[] slots = renderSlots ? NameTagCache.getCachedEquipmentSlots(this, player) : EMPTY_SLOTS;
            List<List<NameTagModel.EnchantLine>> enchantLines = List.of();
            int maxEnchantLines = 0;
            if (renderSlots) {
                NameTagModel.EnchantCache cache = NameTagCache.resolveEnchantCache(this, player, slots);
                enchantLines = cache.lines;
                maxEnchantLines = cache.maxLines;
            }
            List<MobEffectInstance> effects = renderEffects ? NameTagCache.resolveEffects(this, player) : List.of();

            int itemsY = NameTagLayoutEngine.computeItemsY(rect, nameY, distOffset);
            itemsY -= sectionLift;
            renderQueue.add(new NameTagModel.RenderEntry(player, dist, distOffset, centerX, nameY, itemsY, rect,
                    renderSlots, renderNameplate, renderEffects, labelInfo, infoColor, nameplateLayout,
                    slots, enchantLines, maxEnchantLines, effects, totemPopCount, totemPopAnimation));
        }

        NameTagCache.pruneCaches(this);
    }

    @Override
    public void onRenderHudEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        NameTagRenderer.ensureFontCache(this, textRenderer);

        for (NameTagModel.RenderEntry entry : renderQueue) {
            NameTagRenderer.renderNameTagForeground(this, renderer, ctx, textRenderer, entry);
        }
    }

    private boolean shouldRender(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.player == null) return false;
        if (player == mc.player) {
            CameraType p = mc.options.getCameraType();
            if (p == null || p.isFirstPerson()) return false;
            if (!toggles.get("Show self in 3rd person")) return false;
        }
        String name = player.getGameProfile().name();
        return !"FreeCamera".equals(name);
    }

    public void onResourceReload() {
        cachedNameTr = null;
        cachedHpTr = null;
        cachedLevelTr = null;
        cachedTimeTr = null;
        NameTagCache.clearCaches(this);
    }

    private static Vec3 obtainEntityLerpedPos(Entity e, float tickDelta) {
        try {
            return e.getPosition(tickDelta);
        } catch (NoSuchMethodError ex) {
            if (e instanceof IEntity access) {
                return access.get$InstantRenderPos().lerp(e.position(), tickDelta);
            }
            return e.position();
        }
    }

    private static NameTagModel.ScreenRect projectBoxScreen(AABB box, float tickDelta) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean any = false;
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vec3 screen = ScreenProjection.worldToScreen(new Vec3(x, y, z), tickDelta);
                    if (screen == null) continue;
                    any = true;
                    minX = Math.min(minX, screen.x);
                    minY = Math.min(minY, screen.y);
                    maxX = Math.max(maxX, screen.x);
                    maxY = Math.max(maxY, screen.y);
                }
            }
        }
        if (!any) return null;
        return new NameTagModel.ScreenRect(minX, minY, maxX, maxY);
    }
}
