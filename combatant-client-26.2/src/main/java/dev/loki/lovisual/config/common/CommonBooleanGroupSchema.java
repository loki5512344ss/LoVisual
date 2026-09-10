/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.common;

import java.util.Map;
import dev.loki.lovisual.config.common.CommonSettingSchema;

public interface CommonBooleanGroupSchema extends CommonSettingSchema {
    Map<String, Boolean> defaults();
}
