/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.relations;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.util.block.bed.BedwarsTeamColorUtil;

public enum CategoryRules {
    ;

    public static CategoryType determine(String name) {
        if (name == null || name.isBlank()) return CategoryType.DEFAULT;

        PlayerRelations rel = PlayerRelations.get();

        if (rel.isStaff(name) || StaffHeuristicsConfig.get().matches(name) || matchesTrackedStaff(name))
            return CategoryType.STAFF;

        if (rel.isFriend(name))
            return CategoryType.FRIEND;

        if (rel.isEnemy(name))
            return CategoryType.ENEMY;

        DefineTarget defineTarget = Modules.get(DefineTarget.class);
        if (defineTarget != null && defineTarget.isBedwarsRelationsActive()) {
            return switch (BedwarsTeamColorUtil.determineRelation(name)) {
                case SELF -> CategoryType.BEDWARS_SELF;
                case ENEMY -> CategoryType.BEDWARS_ENEMY;
                case NONE -> CategoryType.DEFAULT;
            };
        }

        return CategoryType.DEFAULT;
    }

    private static boolean matchesTrackedStaff(String name) {
        if (name == null || name.isBlank()) return false;
        for (StaffTracker.StaffInfo info : StaffTracker.all().values()) {
            if (info == null || info.name == null || !info.name.equalsIgnoreCase(name)) continue;
            if (StaffHeuristicsConfig.get().matches(info.name, info.displayName)) return true;
        }
        return false;
    }
}
