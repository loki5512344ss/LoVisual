/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.addon;

import dev.loki.lovisual.api.v0.client.LoVisualClientApi;

public interface LoVisualAddonRuntimeContext {
    String addonId();

    LoVisualClientApi client();
}
