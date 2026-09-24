package dev.loki.lovisual.addon.core;

import dev.loki.lovisual.addon.pipeline.AddonStatus;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementRegistry;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;

/**
 * Runtime enable/disable transitions for a single addon, backing {@link AddonManager#setEnabled}.
 * Applies the contributed modules / draggable-HUD / static-HUD elements transiently (recording
 * their prior enabled state so it can be restored) and drives the {@code onRuntimeResumed} /
 * {@code onRuntimeSuspended} callbacks via {@link AddonLifecycle}. Shares {@code addon.core}'s
 * package for package-private registration access; lives in a subfolder to keep the manager small.
 */
final class AddonRuntime {

    private AddonRuntime() { }

    /** Bring a just-enabled addon up: configure, initialize-or-resume, and notify if client-ready. */
    static void enable(AddonRegistration registration, boolean clientReady) {
        if (!registration.moduleConfigurationApplied) {
            AddonLifecycle.configure(registration);
        }
        if (!registration.initialized) {
            AddonLifecycle.initialize(registration, clientReady);
        } else {
            resumeContributions(registration);
            AddonLifecycle.resume(registration, "enabled by user");
            if (registration.status != AddonStatus.ERROR) {
                registration.status = AddonStatus.ACTIVE;
            }
        }
        if (clientReady) {
            AddonLifecycle.notifyClientReady(registration);
        }
    }

    /** Take a just-disabled addon down: flag a restart if it excluded built-ins, then suspend. */
    static void disable(AddonRegistration registration) {
        registration.restartRequired = !registration.disabledBuiltInModules.isEmpty();
        if (!registration.initialized) {
            registration.status = AddonStatus.DISABLED;
            return;
        }
        suspendContributions(registration);
        AddonLifecycle.suspend(registration, "disabled by user");
        registration.status = AddonStatus.DISABLED;
    }

    /** Record and transiently disable every module / HUD element the addon contributes. */
    static void suspendContributions(AddonRegistration registration) {
        registration.suspendedModules.clear();
        registration.suspendedDraggableHudElements.clear();
        registration.suspendedStaticHudElements.clear();
        for (String id : registration.modules) {
            Module module = ModuleManager.get(id);
            if (module != null) {
                registration.suspendedModules.put(id, module.isEnabled());
                ModuleManager.setRuntimeEnabledTransient(id, false);
            }
        }
        for (String id : registration.draggableHudElements) {
            DraggableHudElement element = DraggableHudElementRegistry.getById(id);
            if (element != null) {
                registration.suspendedDraggableHudElements.put(id, element.isEnabled());
                element.setRuntimeEnabledTransient(false);
            }
        }
        for (String id : registration.staticHudElements) {
            AbstractHudElement element = StaticHudElementRegistry.getById(id);
            if (element != null) {
                registration.suspendedStaticHudElements.put(id, element.isEnabled());
                element.setRuntimeEnabledTransient(false);
            }
        }
    }

    /** Restore the pre-suspend enabled state of every contributed module / HUD element. */
    static void resumeContributions(AddonRegistration registration) {
        for (String id : registration.modules) {
            Boolean enabled = registration.suspendedModules.get(id);
            if (enabled != null) {
                ModuleManager.setRuntimeEnabledTransient(id, enabled);
            }
        }
        for (String id : registration.draggableHudElements) {
            Boolean enabled = registration.suspendedDraggableHudElements.get(id);
            DraggableHudElement element = DraggableHudElementRegistry.getById(id);
            if (enabled != null && element != null) {
                element.setRuntimeEnabledTransient(enabled);
            }
        }
        for (String id : registration.staticHudElements) {
            Boolean enabled = registration.suspendedStaticHudElements.get(id);
            AbstractHudElement element = StaticHudElementRegistry.getById(id);
            if (enabled != null && element != null) {
                element.setRuntimeEnabledTransient(enabled);
            }
        }
        registration.suspendedModules.clear();
        registration.suspendedDraggableHudElements.clear();
        registration.suspendedStaticHudElements.clear();
    }
}
