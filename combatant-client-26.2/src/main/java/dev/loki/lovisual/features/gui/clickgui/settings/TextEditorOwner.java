/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

public interface TextEditorOwner {
    void applyEditorText(String rawText);

    String getEditorText();

    boolean isSingleLine();
}
