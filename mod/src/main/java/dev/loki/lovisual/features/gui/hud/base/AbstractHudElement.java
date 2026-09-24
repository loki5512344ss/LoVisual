package dev.loki.lovisual.features.gui.hud.base;
import dev.loki.lovisual.features.gui.hud.base.BaseHudElement;

/**
 * Base class for non-draggable (vanilla/static) HUD elements with self-owned config.
 */
public abstract class AbstractHudElement extends BaseHudElement {

    protected AbstractHudElement() {
        super();
    }

    protected AbstractHudElement(String id, String title, boolean defaultEnabled) {
        super(id, title, defaultEnabled);
    }

    @Override
    public String getTranslationKeyPrefix() {
        return "setting.hud_element." + getId();
    }

    @Override
    public String getConfigName() {
        return "hud_element_" + getId();
    }
}
