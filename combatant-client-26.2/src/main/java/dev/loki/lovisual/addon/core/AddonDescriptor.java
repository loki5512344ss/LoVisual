/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.addon.core;

import java.util.List;

public record AddonDescriptor(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        String iconPath,
        int apiVersion,
        String entrypointDefinition
) {
}
