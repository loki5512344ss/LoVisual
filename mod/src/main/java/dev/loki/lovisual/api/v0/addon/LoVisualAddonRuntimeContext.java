package dev.loki.lovisual.api.v0.addon;

import dev.loki.lovisual.api.v0.client.LoVisualClientApi;

public interface LoVisualAddonRuntimeContext {
    String addonId();

    LoVisualClientApi client();
}
