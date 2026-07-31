/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config;

import java.util.List;

/**
 * Provides GUI-neutral setting definitions.
 */
public interface SettingDefProvider {
    List<SettingDef> getSettingDefs();
}
