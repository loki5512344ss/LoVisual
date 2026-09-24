package dev.loki.lovisual.render.invisible;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.InvisEsp;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Логика See Invisibles: невидимый игрок раскрывается — флаг невидимости
 * снимается перед рендером, а render type форсится в полупрозрачный.
 */
public enum SeeInvisiblesReveal {
    ;

    public static <T extends LivingEntity> void reveal(T entity, LivingEntityRenderState state) {
        if (!(entity instanceof Player player) || player == Minecraft.getInstance().player) {
            return;
        }
        InvisEsp module = Modules.get(InvisEsp.class);
        if (module == null || !module.isEnabled() || !entity.isInvisible()) {
            return;
        }
        if (state instanceof SeeInvisiblesState seeInvis) {
            seeInvis.lovisual$setSeeInvisRevealed(true);
        }
        state.isInvisible = false;
    }

    /** @return true if the entity was revealed this frame and should render below full opacity. */
    public static boolean isRevealedTranslucent(LivingEntityRenderState state) {
        InvisEsp module = Modules.get(InvisEsp.class);
        return module != null && module.isEnabled() && module.isTranslucentReveal()
                && state instanceof SeeInvisiblesState seeInvis && seeInvis.lovisual$isSeeInvisRevealed();
    }

    /** @return the configured InvisESP reveal opacity, or fully opaque if the module is missing. */
    public static float revealAlpha() {
        InvisEsp module = Modules.get(InvisEsp.class);
        return module == null ? 1.0f : module.getOpacity();
    }

    public static RenderType renderTypeFor(RenderType original, Identifier texture) {
        InvisEsp module = Modules.get(InvisEsp.class);
        if (module == null || !module.isEnabled() || IrisRuntime.isRenderingShadowPass()) {
            return original;
        }
        if (!module.isTranslucentReveal()) {
            return original;
        }
        if (original != null && original.hasBlending()) {
            return original;
        }
        return RenderTypes.entityTranslucent(texture, true);
    }
}
