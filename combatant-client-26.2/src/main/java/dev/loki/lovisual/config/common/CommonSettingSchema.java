/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.common;

import java.util.List;

public interface CommonSettingSchema {
    String commonI18nKey();

    default List<String> commonI18nKeys() {
        String key = commonI18nKey();
        return key == null || key.isBlank() ? List.of() : List.of(key);
    }
}
