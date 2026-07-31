/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config;

import dev.loki.lovisual.config.values.ConfigValue;

import java.util.List;

public interface ConfigObject {
    List<ConfigValue<?>> getConfigValues();
}
