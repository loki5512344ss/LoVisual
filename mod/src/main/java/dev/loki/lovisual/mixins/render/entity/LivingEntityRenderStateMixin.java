package dev.loki.lovisual.mixins.render.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.loki.lovisual.render.invisible.SeeInvisiblesState;
import dev.loki.lovisual.render.fade.ViewObstructionFadeState;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements ViewObstructionFadeState, SeeInvisiblesState {
    @Unique
    private boolean lovisual$viewObstructionFadeActive;

    @Unique
    private float lovisual$viewObstructionFadeAlpha = 1.0f;

    @Unique
    private boolean lovisual$seeInvisRevealed;

    @Override
    public boolean lovisual$isSeeInvisRevealed() {
        return lovisual$seeInvisRevealed;
    }

    @Override
    public void lovisual$setSeeInvisRevealed(boolean revealed) {
        this.lovisual$seeInvisRevealed = revealed;
    }

    @Override
    public boolean lovisual$isViewObstructionFadeActive() {
        return lovisual$viewObstructionFadeActive;
    }

    @Override
    public float lovisual$getViewObstructionFadeAlpha() {
        return lovisual$viewObstructionFadeAlpha;
    }

    @Override
    public void lovisual$setViewObstructionFadeActive(boolean active) {
        this.lovisual$viewObstructionFadeActive = active;
    }

    @Override
    public void lovisual$setViewObstructionFadeAlpha(float alpha) {
        this.lovisual$viewObstructionFadeAlpha = alpha;
    }
}
