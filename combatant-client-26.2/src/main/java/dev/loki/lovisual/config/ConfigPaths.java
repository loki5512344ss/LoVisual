/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum ConfigPaths {
    ;

    // TODO: make the user config root configurable, including base directory and visible folder name.
    // Keep config/lovisual as the default for compatibility with existing installs.
    public static Path root() {
        return Paths.get("config", legacyNamespaceName());
    }

    public static Path profilesRoot() {
        return root().resolve("profiles");
    }

    public static String legacyNamespaceName() {
        return new String(new char[]{'l', 'o', 'v', 'i', 's', 'u', 'a', 'l'});
    }
}
