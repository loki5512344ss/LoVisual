package dev.loki.lovisual.events.impl.render;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
public class LightmapEvent extends Event {
    private final float originalAmbient;
    @Setter
    private float ambientLight;

    public LightmapEvent(float ambientLight) {
        this.originalAmbient = ambientLight;
        this.ambientLight = ambientLight;
    }

    public void raiseAmbientLight(float ambientLight) {
        if (ambientLight > this.ambientLight) {
            this.ambientLight = ambientLight;
        }
    }

    public boolean isModified() {
        return Math.abs(ambientLight - originalAmbient) > 1.0e-4f;
    }
}
