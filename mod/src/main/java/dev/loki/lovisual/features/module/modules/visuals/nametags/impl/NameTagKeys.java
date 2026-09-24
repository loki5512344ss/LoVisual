package dev.loki.lovisual.features.module.modules.visuals.nametags;

/**
 * Setting-schema keys for {@link NameTags}. These are the second argument to the
 * value factories (the persisted schema id stays the first argument), kept in one
 * place so the module class is not peppered with string-literal constants.
 * Lives in the {@code impl/} subfolder but the {@code nametags} package.
 */
final class NameTagKeys {
    static final String TOGGLES = "toggles";
    static final String DURABILITY_START = "durability_start_percent";
    static final String DISTANCE_TOGGLES = "distance_toggles";
    static final String NAMEPLATE_CUTOFF = "nameplate_cutoff";
    static final String SLOTS_CUTOFF = "slots_cutoff";
    static final String EFFECTS_CUTOFF = "effects_cutoff";
    static final String ARMOR_ENCHANTS = "armor_enchants";
    static final String MELEE_ENCHANTS = "melee_enchants";
    static final String TOOLS_ENCHANTS = "tools_enchants";
    static final String BOW_ENCHANTS = "bow_enchants";
    static final String FISHING_ENCHANTS = "fishing_enchants";
    static final String TRIDENT_ENCHANTS = "trident_enchants";
    static final String CROSSBOW_ENCHANTS = "crossbow_enchants";
    static final String MACE_ENCHANTS = "mace_enchants";
    static final String OTHER_ENCHANTS = "other_enchants";
    static final String ILLEGAL_ENCHANT_COLOR = "illegal_enchant_color";
    static final String ILLEGAL_ENCHANTS_ALWAYS = "illegal_enchants_always";
    static final String TOP_ENCHANT_COLOR = "top_enchant_color";
    static final String TOP_ENCHANT_IGNORE_LIST = "top_enchant_ignore_list";
    static final String HP_DECIMALS = "hp_decimals";
    static final String HP_COLOR_MODE = "hp_color_mode";
    static final String NAMEPLATE_ALPHA = "nameplate_alpha";
    static final String NAMEPLATE_STYLE = "nameplate_style";
    static final String PLAYER_HEAD = "player_head";
    static final String NAMEPLATE_EXTRAS = "nameplate_extras";
    static final String OPPONENT_APPLE_COOLDOWNS = "opponent_apple_cooldowns";
    static final String PVP_PREFIX = "pvp_prefix";
    static final String PVP_PREFIX_COLOR = "pvp_prefix_color";

    private NameTagKeys() {
    }
}
