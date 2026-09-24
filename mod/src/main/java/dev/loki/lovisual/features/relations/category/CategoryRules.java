package dev.loki.lovisual.features.relations.category;
import dev.loki.lovisual.features.relations.PlayerRelations;


import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.util.block.bed.BedwarsTeamColorUtil;

public enum CategoryRules {
    ;

    public static CategoryType determine(String name) {
        if (name == null || name.isBlank()) return CategoryType.DEFAULT;

        PlayerRelations rel = PlayerRelations.get();

        if (rel.isStaff(name))
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
}
