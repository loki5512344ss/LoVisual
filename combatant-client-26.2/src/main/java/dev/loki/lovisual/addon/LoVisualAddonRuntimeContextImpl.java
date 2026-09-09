/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.addon;

import dev.loki.lovisual.api.v0.addon.LoVisualAddonRuntimeContext;
import dev.loki.lovisual.api.v0.client.LoVisualClientApi;

public final class LoVisualAddonRuntimeContextImpl implements LoVisualAddonRuntimeContext {
    private final String addonId;

    LoVisualAddonRuntimeContextImpl(String addonId) {
        this.addonId = addonId;
    }

    @Override
    public String addonId() {
        return addonId;
    }

    @Override
    public LoVisualClientApi client() {
        return LoVisualClientApi.get();
    }
}
