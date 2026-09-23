/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.items.held;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

/**
 * Держит руки видимыми, когда ваниль их прячет: при еде/питье/натягивании лука
 * ваниль выбирает RENDER_OFF_HAND_ONLY и предмет в главной руке исчезает.
 * По мотивам Hold My Items (HandRenderTypeMixin) — пере-реализация под наши правила.
 */
@ModuleInfo(
        id = "alwaysvisiblehand",
        displayName = "AlwaysVisibleHand",
        aliases = {"alwaysvisible", "hand"},
        category = ModuleCategory.VISUALS,
        description = "Не даёт ванилле прятать руку с предметом при использовании другого предмета."
)
public final class AlwaysVisibleHand extends Module {

    public static final String SELECTION_BOTH = "Both";
    public static final String SELECTION_MAIN = "Main hand";
    public static final String SELECTION_OFF = "Off hand";

    private static final String SETTING_KEEP = "keep_visible";

    private final ModeValue keepVisible =
            modeSetting("handKeepVisible", SETTING_KEEP, SELECTION_BOTH, SELECTION_BOTH, SELECTION_MAIN, SELECTION_OFF);

    private final BooleanValue keepDuringUse =
            bool("hand_keep_during_use", SETTING_KEEP + "_during_use", true);

    public boolean renderMainHand(boolean vanillaDecision) {
        return resolveHand(SELECTION_MAIN, vanillaDecision);
    }

    public boolean renderOffHand(boolean vanillaDecision) {
        return resolveHand(SELECTION_OFF, vanillaDecision);
    }

    private boolean resolveHand(String handLabel, boolean vanillaDecision) {
        if (!isEnabled() || !keepDuringUse.get()) return vanillaDecision;
        String mode = keepVisible.get();
        return SELECTION_BOTH.equals(mode) || mode.equals(handLabel);
    }

    /**
     * Чистое правило для теста: какая рука должна остаться видимой при выбранном режиме.
     */
    public static boolean[] resolveSelection(String mode, boolean vanillaMain, boolean vanillaOff) {
        return switch (mode) {
            case SELECTION_BOTH -> new boolean[]{true, true};
            case SELECTION_MAIN -> new boolean[]{true, vanillaOff};
            case SELECTION_OFF -> new boolean[]{vanillaMain, true};
            default -> new boolean[]{vanillaMain, vanillaOff};
        };
    }
}
