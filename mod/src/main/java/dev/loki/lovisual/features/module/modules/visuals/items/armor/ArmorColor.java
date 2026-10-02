package dev.loki.lovisual.features.module.modules.visuals.items.armor;

import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.relations.category.CategoryService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Colours worn armor by its remaining durability (full, warning, broken). Which entities qualify is
 * decided once per frame at render-state extraction; the tint itself is applied in EquipmentLayerRendererMixin.
 */
@ModuleInfo(
        id = "armorcolor",
        displayName = "Armor Color",
        aliases = {"armorcolor"},
        category = ModuleCategory.VISUALS
)
public class ArmorColor extends Module {
    private static final String PLAYERS = "players";
    private static final String FRIENDS = "friends";
    private static final String SELF = "self";
    private static final String MOBS = "mobs";

    private final BooleanMapValue targets = group("armorColorTargets", "targets", defaultTargets());
    private final ModeValue colorMode =
            modeSetting("armorColorMode", "color_mode", ArmorTint.GRADIENT, ArmorTint.GRADIENT, ArmorTint.STEPS);
    private final RGBColorValue fullColor = colorNoAlpha("armorColorFull", "full_color", "#4CD964");
    private final RGBColorValue warnColor = colorNoAlpha("armorColorWarn", "warn_color", "#FFC107");
    private final RGBColorValue brokenColor = colorNoAlpha("armorColorBroken", "broken_color", "#FF3B30");
    private final NumberValue<Float> strength = num("armorColorStrength", "strength", 1.0f, 0.1f, 1.0f);
    private final NumberValue<Float> onlyBelow = num("armorColorOnlyBelow", "only_below", 100.0f, 5.0f, 100.0f);
    private final BooleanValue pulse = bool("armorColorPulse", "pulse", true);

    private static Map<String, Boolean> defaultTargets() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(PLAYERS, true);
        map.put(FRIENDS, true);
        map.put(SELF, true);
        map.put(MOBS, false);
        return map;
    }

    /** Whether this entity's armor should be coloured; evaluated at render-state extraction. */
    public boolean covers(LivingEntity entity) {
        if (!isEnabled()) return false;
        if (entity instanceof LocalPlayer) return targets.get(SELF);
        if (entity instanceof Player player) {
            return targets.get(CategoryService.isFriend(player) ? FRIENDS : PLAYERS);
        }
        return targets.get(MOBS);
    }

    /** @return the tint for one armor piece, {@link ArmorTint#NO_TINT} to keep vanilla colour. */
    public int tintFor(ItemStack stack, Object renderState) {
        if (!isEnabled() || stack == null || stack.isEmpty() || !stack.isDamageableItem()) return ArmorTint.NO_TINT;
        if (!(renderState instanceof ArmorColorState state) || !state.lovisual$isArmorColored()) return ArmorTint.NO_TINT;

        float fraction = ArmorTint.fraction(stack.getDamageValue(), stack.getMaxDamage());
        if (fraction < 0.0f || fraction * 100.0f > onlyBelow.get()) return ArmorTint.NO_TINT;

        int rgb = ArmorTint.ramp(fraction, colorMode.get(),
                fullColor.getArgb() & 0xFFFFFF, warnColor.getArgb() & 0xFFFFFF, brokenColor.getArgb() & 0xFFFFFF);
        if (pulse.get() && fraction <= ArmorTint.CRITICAL_FRACTION) rgb = ArmorTint.pulse(rgb, Util.getMillis());
        return ArmorTint.tint(rgb, strength.get());
    }
}
