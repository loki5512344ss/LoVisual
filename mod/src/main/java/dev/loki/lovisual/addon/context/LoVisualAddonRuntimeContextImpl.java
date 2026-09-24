package dev.loki.lovisual.addon.context;

import dev.loki.lovisual.api.v0.addon.LoVisualAddonRuntimeContext;
import dev.loki.lovisual.api.v0.client.LoVisualClientApi;

public final class LoVisualAddonRuntimeContextImpl implements LoVisualAddonRuntimeContext {
    private final String addonId;

    public LoVisualAddonRuntimeContextImpl(String addonId) {

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
