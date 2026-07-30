/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.runtime;

public enum ClientRuntimeState {
    BOOTING,
    ACTIVE,
    SOFT_PANIC,
    JAR_REPLACEMENT_PANIC,
    SHUTDOWN_PENDING,
    DEAD
}
