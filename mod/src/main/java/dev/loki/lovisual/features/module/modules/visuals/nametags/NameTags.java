package dev.loki.lovisual.features.module.modules.visuals.nametags;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.nametags.render.NameTagRenderer;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.item.food.IllegalItemUtil;
import dev.loki.lovisual.util.item.enchant.TopEnchantUtil;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

@ModuleInfo(id = "nametags", displayName = "NameTags", aliases = {"nametag", "tags", "names"}, category = ModuleCategory.VISUALS)
public class NameTags extends Module {

    public static final int OPPONENT_DISTANCE = 80;

    private final ItemIdSetValue topIgnore = TopEnchantUtil.ignoreValue();

    final List<NameTagModel.RenderEntry> renderQueue = new ArrayList<>();
    final List<Player> playerBuffer = new ArrayList<>();
    final Map<UUID, ItemStack[]> slotCache = new HashMap<>();
    final Map<UUID, NameTagModel.EffectCache> effectCache = new HashMap<>();
    final Map<UUID, NameTagModel.EnchantCache> enchantCache = new HashMap<>();
    final Map<UUID, Float> totemPopAnimations = new HashMap<>();
    final Map<UUID, Integer> totemPopLastCounts = new HashMap<>();
    final HashSet<UUID> seenIds = new HashSet<>();

    public final BooleanMapValue toggles = group("nameTagsToggles", NameTagKeys.TOGGLES, Map.of(
            "Show nameplate", true,
            "Show distance", true,
            "Show armor row", true,
            "Item durability bar", true,
            "Show effects", true,
            "Show self in 3rd person", false
    ));
    public final NumberValue<Integer> durabilityStartPercent =
            visibleWhen(num("nameTagsDurabilityStartPercent", NameTagKeys.DURABILITY_START, 99, 0, 100),
                    () -> toggles.get("Item durability bar"));
    final BooleanMapValue distanceToggles =
            visibleWhen(group("nameTagsDistanceToggles", NameTagKeys.DISTANCE_TOGGLES, Map.of(
                    "nameplate", true,
                    "slots", true,
                    "effects", true
            )), () -> toggles.get("Show nameplate") || toggles.get("Show armor row") || toggles.get("Show effects"));
    final NumberValue<Integer> plateDistance =
            visibleWhen(num("nameTagsPlateDistance", NameTagKeys.NAMEPLATE_CUTOFF, 80, 5, 256),
                    () -> toggles.get("Show nameplate"));
    final NumberValue<Integer> slotsDistance =
            visibleWhen(num("nameTagsSlotsDistance", NameTagKeys.SLOTS_CUTOFF, 70, 5, 256),
                    () -> toggles.get("Show armor row"));
    final NumberValue<Integer> effectsDistance =
            visibleWhen(num("nameTagsEffectsDistance", NameTagKeys.EFFECTS_CUTOFF, 90, 5, 256),
                    () -> toggles.get("Show effects"));
    public final NumberValue<Integer> nameplateAlpha =
            visibleWhen(num("nameTagsNameplateAlpha", NameTagKeys.NAMEPLATE_ALPHA, 180, 0, 255),
                    () -> toggles.get("Show nameplate"));
    public final EnumValue<NameTagModel.NameplateStyle> nameplateStyle =
            visibleWhen(enumSetting("nameTagsNameplateStyle", NameTagKeys.NAMEPLATE_STYLE, NameTagModel.NameplateStyle.LIQUID_GLASS, NameTagModel.NameplateStyle.values()),
                    () -> toggles.get("Show nameplate"));
    public final BooleanValue playerHead =
            visibleWhen(bool("nameTagsPlayerHead", NameTagKeys.PLAYER_HEAD, false),
                    () -> toggles.get("Show nameplate"));
    public final BooleanMapValue nameplateExtras = visibleWhen(group("nameTagsNameplateExtras", NameTagKeys.NAMEPLATE_EXTRAS, new LinkedHashMap<>() {{
        put("Tab names", false);
        put("Ping", false);
    }}), () -> toggles.get("Show nameplate"));
    public final BooleanMapValue enchantArmor = group("nameTagsEnchantArmor", NameTagKeys.ARMOR_ENCHANTS, Map.ofEntries(
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
    public final BooleanMapValue enchantMelee = group("nameTagsEnchantMelee", NameTagKeys.MELEE_ENCHANTS, Map.ofEntries(
            Map.entry("sharpness", true),
            Map.entry("smite", false),
            Map.entry("bane_of_arthropods", false),
            Map.entry("knockback", true),
            Map.entry("fire_aspect", true),
            Map.entry("looting", true),
            Map.entry("sweeping_edge", false)
    ));
    public final BooleanMapValue enchantTools = group("nameTagsEnchantTools", NameTagKeys.TOOLS_ENCHANTS, Map.ofEntries(
            Map.entry("efficiency", true),
            Map.entry("silk_touch", true),
            Map.entry("fortune", true)
    ));
    public final BooleanMapValue enchantBow = group("nameTagsEnchantBow", NameTagKeys.BOW_ENCHANTS, Map.ofEntries(
            Map.entry("power", true),
            Map.entry("punch", true),
            Map.entry("flame", false),
            Map.entry("infinity", false)
    ));
    public final BooleanMapValue enchantFishing = group("nameTagsEnchantFishing", NameTagKeys.FISHING_ENCHANTS, Map.ofEntries(
            Map.entry("luck_of_the_sea", false),
            Map.entry("lure", false)
    ));
    public final BooleanMapValue enchantTrident = group("nameTagsEnchantTrident", NameTagKeys.TRIDENT_ENCHANTS, Map.ofEntries(
            Map.entry("loyalty", false),
            Map.entry("impaling", false),
            Map.entry("riptide", false),
            Map.entry("channeling", false)
    ));
    public final BooleanMapValue enchantCrossbow = group("nameTagsEnchantCrossbow", NameTagKeys.CROSSBOW_ENCHANTS, Map.ofEntries(
            Map.entry("multishot", false),
            Map.entry("quick_charge", false),
            Map.entry("piercing", true)
    ));
    public final BooleanMapValue enchantMace = group("nameTagsEnchantMace", NameTagKeys.MACE_ENCHANTS, Map.ofEntries(
            Map.entry("density", true),
            Map.entry("breach", false),
            Map.entry("wind_burst", true)
    ));
    public final BooleanMapValue enchantOther = group("nameTagsEnchantOther", NameTagKeys.OTHER_ENCHANTS, Map.ofEntries(
            Map.entry("mending", false),
            Map.entry("unbreaking", false),
            Map.entry("vanishing_curse", false),
            Map.entry("lunge", true)
    ));
    public final BooleanValue illegalEnchantsAlways = bool("nameTagsIllegalEnchantsAlways", NameTagKeys.ILLEGAL_ENCHANTS_ALWAYS, true);
    public final BooleanValue opponentAppleCooldowns = bool("nameTagsOpponentAppleCooldowns", NameTagKeys.OPPONENT_APPLE_COOLDOWNS, true);
    final BooleanValue totemPopCounter = TotemPopCounter.enabledValue();
    private final NumberValue<Integer> totemPopResetSeconds = TotemPopCounter.resetAfterSecondsValue();
    public final BooleanValue pvpPrefix = bool("nameTagsPvpPrefix", NameTagKeys.PVP_PREFIX, false);
    public final RGBColorValue pvpPrefixColor =
            visibleWhen(colorNoAlpha("nameTagsPvpPrefixColor", NameTagKeys.PVP_PREFIX_COLOR, "#FF0000"), pvpPrefix::get);
    public final BooleanValue hpDecimals =
            visibleWhen(bool("nameTagsHpDecimals", NameTagKeys.HP_DECIMALS, false),
                    () -> toggles.get("Show nameplate"));
    public final ModeValue hpColorMode =
            visibleWhen(modeSetting("nameTagsHpColorMode", NameTagKeys.HP_COLOR_MODE, "Static", "Static", "Gradient"),
                    () -> toggles.get("Show nameplate"));

    public transient TextRenderer cachedNameTr;
    public transient TextRenderer cachedHpTr;
    public transient TextRenderer cachedLevelTr;
    public transient TextRenderer cachedTimeTr;

    {
        setting(SettingDef.bool(totemPopCounter)
                .common(CommonSettingSchemas.TOTEM_POP_COUNTER.commonI18nKey()));
        setting(SettingDef.number(totemPopResetSeconds)
                .common(CommonSettingSchemas.TOTEM_POP_RESET_SECONDS.commonI18nKey())
                .visibleWhen(totemPopCounter::get));
        setting(SettingDef.colorNoAlpha(NameTagKeys.ILLEGAL_ENCHANT_COLOR, IllegalItemUtil.illegalColorValue()));
        setting(SettingDef.colorNoAlpha(NameTagKeys.TOP_ENCHANT_COLOR, TopEnchantUtil.topColorValue()));
        setting(SettingDef.textList(NameTagKeys.TOP_ENCHANT_IGNORE_LIST, topIgnore, TextListSetting.PickerMode.ENCHANTMENTS));
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
        NameTagsCollector.collect(this, textRenderer, tickDelta);
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

    boolean shouldRender(Player player) {
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
}
