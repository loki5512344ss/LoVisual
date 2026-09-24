package dev.loki.lovisual.addon.context;

import dev.loki.lovisual.api.v0.addon.LoVisualModuleLoadContext;
import dev.loki.lovisual.features.module.lifecycle.ModuleAutoLoader;

import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.core.AddonRegistration;

public final class LoVisualModuleLoadContextImpl implements LoVisualModuleLoadContext {
    private final AddonRegistration registration;
    private final List<String> pendingExclusions = new ArrayList<>();

    public LoVisualModuleLoadContextImpl(AddonRegistration registration) {

        this.registration = registration;
    }

    @Override
    public String addonId() {
        return registration.descriptor.id();
    }

    @Override
    public boolean disableBuiltInModule(String moduleReference) {
        String reference = moduleReference == null ? "" : moduleReference.trim();
        if (reference.isEmpty()) return false;

        if (!ModuleAutoLoader.acceptsDisableRequests()) {
            noteRestartRequired(reference);
            return false;
        }

        if (!pendingExclusions.contains(reference)) {
            pendingExclusions.add(reference);
        }
        return true;
    }

    public void commit() {

        for (String reference : pendingExclusions) {
            if (!registration.disabledBuiltInModules.contains(reference)) {
                registration.disabledBuiltInModules.add(reference);
            }
            if (!ModuleAutoLoader.requestDisable(reference, addonId())) {
                noteRestartRequired(reference);
            }
        }
        pendingExclusions.clear();
    }

    private void noteRestartRequired(String reference) {
        if (!registration.disabledBuiltInModules.contains(reference)) {
            registration.disabledBuiltInModules.add(reference);
        }
        registration.restartRequired = true;
        registration.issue(
                AddonIssue.Severity.WARNING,
                "Built-in module exclusion requires restart",
                reference
        );
    }
}
