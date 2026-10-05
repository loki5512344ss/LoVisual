package dev.loki.lovisual.features.module.modules.visuals.wings.model.anim;

import dev.loki.lovisual.util.bedrock.geo.GeoParser;

/** Poses a wing model for a moment in time. */
public interface WingAnimator {
    /**
     * @param seconds animation clock in seconds
     * @param flight  0 while standing, 1 while gliding; models without a flight pose ignore it
     */
    void apply(GeoParser.Model model, float seconds, float flight);
}
