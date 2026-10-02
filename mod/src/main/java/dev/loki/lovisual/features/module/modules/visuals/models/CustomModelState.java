package dev.loki.lovisual.features.module.modules.visuals.models;

/**
 * Implemented by LivingEntityRenderState. The model a player is drawn with is chosen once per frame, while the
 * entity is still at hand, and carried here to the moment the model is submitted.
 */
public interface CustomModelState {
    /** The chosen model name, or null when this player is drawn as a player. */
    String lovisual$customModel();

    void lovisual$setCustomModel(String model);
}
