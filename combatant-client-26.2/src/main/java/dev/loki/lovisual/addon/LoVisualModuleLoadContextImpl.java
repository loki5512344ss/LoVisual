/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.addon;

import dev.loki.lovisual.api.v0.addon.LoVisualModuleLoadContext;
import dev.loki.lovisual.features.module.ModuleAutoLoader;

import java.util.ArrayList;
import java.util.List;

final class LoVisualModuleLoadContextImpl implements LoVisualModuleLoadContext {
    private final AddonRegistration registration;
    private final List<String> pendingExclusions = new ArrayList<>();

    LoVisualModuleLoadContextImpl(AddonRegistration registration) {
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

    void commit() {
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
