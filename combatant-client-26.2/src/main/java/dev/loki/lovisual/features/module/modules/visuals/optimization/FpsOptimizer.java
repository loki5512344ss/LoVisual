/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.optimization;

import net.minecraft.core.particles.ParticleTypes;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.impl.render.ParticleSpawnEvent;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;

@ModuleInfo(
        id = "fpsoptimizer",
        displayName = "FPS Optimizer",
        category = ModuleCategory.VISUALS
)
public final class FpsOptimizer extends Module {

    private final BooleanValue disableParticles =
            bool("disable_particles", false);
    private final BooleanValue disableMenuBackground =
            bool("disable_menu_background", false);
    private final BooleanValue disableGlass =
            bool("disable_glass", false);
    private final BooleanValue disableBloom =
            bool("disable_bloom", false);
    private final BooleanValue entityCulling =
            bool("entity_culling", false);
    private final NumberValue<Integer> entityCullDistance =
            visibleWhen(num("entity_cull_distance", 64, 8, 256), entityCulling::get);

    public boolean isMenuBackgroundDisabled() {
        return isEnabled() && disableMenuBackground.get();
    }

    public boolean isGlassDisabled() {
        return isEnabled() && disableGlass.get();
    }

    public boolean isBloomDisabled() {
        return isEnabled() && disableBloom.get();
    }

    public boolean isEntityCullingEnabled() {
        return isEnabled() && entityCulling.get();
    }

    public int entityCullDistance() {
        return entityCullDistance.get();
    }

    /** Cheap call-site guard for renderers that build bloom/glow geometry every frame. */
    public static boolean bloomDisabled() {
        FpsOptimizer module = Modules.get(FpsOptimizer.class);
        return module != null && module.isBloomDisabled();
    }

    @EventHandler
    private void onParticleSpawn(ParticleSpawnEvent event) {
        if (!isEnabled() || !disableParticles.get()) return;
        // Firework particles are created by FireworkParticles$Starter and assume ParticleEngine.createParticle never returns null.
        // Cancelling them returns null and causes NPE at Starter.createParticle -> setTrail().
        // Guard is also present in FireworkParticlesStarterMixin, but exempt here to preserve fireworks even with optimizer enabled.
        var type = event.options().getType();
        if (type == ParticleTypes.FIREWORK || type == ParticleTypes.FLASH) return;
        event.cancel();
    }
}
