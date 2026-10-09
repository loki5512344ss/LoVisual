package dev.loki.lovisual.features.module.modules.combat.pvp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

@ModuleInfo(
        id = "spearassist",
        displayName = "SpearAssist",
        category = ModuleCategory.COMBAT
)
public class SpearAssist extends Module {

    private static final String SETTING_TOGGLES = "toggles";

    private final BooleanMapValue toggles = group(
            "spearassist_toggles",
            SETTING_TOGGLES,
            new java.util.LinkedHashMap<>() {{
                put("respect_cooldown", true);
            }}
    );

    public static boolean isSpear(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.get(DataComponents.KINETIC_WEAPON) != null;
    }

    public boolean respectCooldownEnabled() {
        return toggles.get("respect_cooldown");
    }
}
