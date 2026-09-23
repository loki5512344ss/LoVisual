/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.compat;

import dev.tr7zw.entityculling.versionless.access.Cullable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;

public enum EntityCullingCompat {
    ;

    private static final String MOD_ID = "entityculling";

    public static void forceVisibleForShaderEsp(Entity entity) {
        if (entity == null || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        if (!(entity instanceof Cullable cullable)) {
            return;
        }

        cullable.setCulled(false);
        cullable.setOutOfCamera(false);
        cullable.setTimeout();
    }
}
