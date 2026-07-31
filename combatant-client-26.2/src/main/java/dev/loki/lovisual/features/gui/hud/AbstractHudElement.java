/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud;

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
