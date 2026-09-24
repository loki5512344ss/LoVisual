package dev.loki.lovisual.render.fade;

public interface ViewObstructionFadeState {
    public boolean lovisual$isViewObstructionFadeActive();

    public float lovisual$getViewObstructionFadeAlpha();

    public void lovisual$setViewObstructionFadeActive(boolean active);

    public void lovisual$setViewObstructionFadeAlpha(float alpha);
}
