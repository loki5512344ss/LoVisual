package dev.loki.lovisual.addon.core;

import dev.loki.lovisual.addon.context.LoVisualAddonContextImpl;
import dev.loki.lovisual.addon.context.LoVisualAddonRuntimeContextImpl;
import dev.loki.lovisual.addon.context.LoVisualModuleLoadContextImpl;
import dev.loki.lovisual.addon.pipeline.AddonStatus;
import dev.loki.lovisual.api.v0.addon.LoVisualAddon;
import dev.loki.lovisual.features.module.lifecycle.ModuleAutoLoader;
import dev.loki.lovisual.util.logging.DebugLog;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

/**
 * Per-registration lifecycle callbacks backing {@link AddonManager}: lazy entrypoint
 * materialization plus the {@code onConfigureModules} / {@code onInitialize} /
 * {@code onClientReady} / {@code onRuntimeSuspended} / {@code onRuntimeResumed} /
 * {@code onShutdown} invocations, each guarded by the registration's flags and turning any
 * thrown error into an {@link AddonIssue} + {@link AddonStatus#ERROR}. Shares {@code addon.core}'s
 * package so the registration's package-private state stays reachable; lives in a subfolder to
 * keep {@link AddonManager} under the file-size cap.
 */
final class AddonLifecycle {

    private AddonLifecycle() { }

    /** Discover addon entrypoints from the Fabric loader and register each as a new addon. */
    static void discover() {
        for (var entrypoint : FabricLoader.getInstance()
                .getEntrypointContainers(LoVisualAddon.ENTRYPOINT_KEY, LoVisualAddon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            String id = AddonManager.normalizeId(metadata.getId());
            AddonDescriptor descriptor = AddonDescriptors.descriptor(metadata, entrypoint.getDefinition());
            boolean enabled = AddonManager.ENABLED_OVERRIDES.getOrDefault(id, true);
            AddonStatus status = enabled ? AddonStatus.DISCOVERED : AddonStatus.DISABLED;
            AddonRegistration duplicate = AddonManager.ADDONS.get(id);
            if (duplicate != null) {
                duplicate.issue(AddonIssue.Severity.ERROR, "Duplicate addon id", metadata.getId());
                continue;
            }
            AddonRegistration registration = new AddonRegistration(descriptor, entrypoint, enabled, status);
            AddonDescriptors.validate(registration);
            AddonManager.ADDONS.put(id, registration);
        }
    }

    /** Configure modules for every currently-enabled addon (pre-instantiation pass). */
    static void configureEnabled() {
        for (AddonRegistration registration : AddonManager.ADDONS.values()) {
            if (registration.enabled) {
                configure(registration);
            }
        }
    }

    /** Drop built-in module exclusions whose target was never found, warning about each. */
    static void reconcileExclusions() {
        for (AddonRegistration registration : AddonManager.ADDONS.values()) {
            if (registration.disabledBuiltInModules.isEmpty()) continue;
            registration.disabledBuiltInModules.removeIf(reference -> {
                if (ModuleAutoLoader.wasDisableRequestMatched(reference, registration.descriptor.id())) {
                    return false;
                }
                registration.issue(
                        AddonIssue.Severity.WARNING,
                        "Built-in module exclusion target was not found",
                        reference
                );
                return true;
            });
        }
    }

    /** Initialize every enabled, non-errored addon. */
    static void initializeEnabled() {
        for (AddonRegistration registration : AddonManager.ADDONS.values()) {
            if (registration.enabled && registration.status != AddonStatus.ERROR) {
                initialize(registration, AddonManager.clientReady);
            }
        }
    }

    /** Let the addon configure built-in module exclusions once, before modules are instantiated. */
    static void configure(AddonRegistration registration) {
        if (registration.moduleConfigurationApplied || !registration.enabled || registration.status == AddonStatus.ERROR) {
            return;
        }
        try {
            LoVisualAddon addon = addonInstance(registration);
            LoVisualModuleLoadContextImpl context = new LoVisualModuleLoadContextImpl(registration);
            addon.onConfigureModules(context);
            context.commit();
            registration.moduleConfigurationApplied = true;
        } catch (Throwable t) {
            registration.status = AddonStatus.ERROR;
            registration.issue(AddonIssue.Severity.ERROR, "Module configuration failed", t.toString());
            DebugLog.error("Addon module configuration failed: %s", t, registration.descriptor.id());
        }
    }

    /** Initialize the addon (configuring modules first if needed); notifies client-ready if up. */
    static void initialize(AddonRegistration registration, boolean clientReady) {
        if (registration.initialized || !registration.enabled || registration.status == AddonStatus.ERROR) return;
        if (!registration.moduleConfigurationApplied) {
            configure(registration);
            if (registration.status == AddonStatus.ERROR) return;
        }
        try {
            LoVisualAddon addon = addonInstance(registration);
            addon.onInitialize(new LoVisualAddonContextImpl(registration));
            registration.initialized = true;
            registration.status = AddonStatus.ACTIVE;
            DebugLog.info("Addon initialized: %s", registration.descriptor.id());
            if (clientReady) {
                notifyClientReady(registration);
            }
        } catch (Throwable t) {
            registration.status = AddonStatus.ERROR;
            registration.issue(AddonIssue.Severity.ERROR, "Initialization failed", t.toString());
            DebugLog.error("Addon initialization failed: %s", t, registration.descriptor.id());
        }
    }

    /** Fire {@code onClientReady} once, after module configs have been applied. */
    static void notifyClientReady(AddonRegistration registration) {
        if (registration == null
                || registration.clientReadyNotified
                || !registration.initialized
                || !registration.enabled
                || registration.status != AddonStatus.ACTIVE) {
            return;
        }
        try {
            registration.addon.onClientReady(
                    new LoVisualAddonRuntimeContextImpl(registration.descriptor.id())
            );
            registration.clientReadyNotified = true;
        } catch (Throwable t) {
            registration.status = AddonStatus.ERROR;
            registration.issue(AddonIssue.Severity.ERROR, "Client ready callback failed", t.toString());
            DebugLog.error("Addon client ready callback failed: %s", t, registration.descriptor.id());
        }
    }

    /** Fire {@code onRuntimeSuspended} for an addon being toggled off at runtime. */
    static void suspend(AddonRegistration registration, String reason) {
        if (registration.addon == null) return;
        try {
            registration.addon.onRuntimeSuspended(new LoVisualAddonRuntimeContextImpl(registration.descriptor.id()));
        } catch (Throwable t) {
            registration.status = AddonStatus.ERROR;
            registration.issue(AddonIssue.Severity.ERROR, "Runtime suspend failed", t.toString());
            DebugLog.error("Addon runtime suspend failed: %s", t, registration.descriptor.id());
        }
    }

    /** Fire {@code onRuntimeResumed} for an addon being toggled back on at runtime. */
    static void resume(AddonRegistration registration, String reason) {
        if (registration.addon == null) return;
        try {
            registration.addon.onRuntimeResumed(new LoVisualAddonRuntimeContextImpl(registration.descriptor.id()));
        } catch (Throwable t) {
            registration.status = AddonStatus.ERROR;
            registration.issue(AddonIssue.Severity.ERROR, "Runtime resume failed", t.toString());
            DebugLog.error("Addon runtime resume failed: %s", t, registration.descriptor.id());
        }
    }

    /** Fire {@code onShutdown} on full addon teardown. */
    static void shutdown(AddonRegistration registration, String reason) {
        if (registration.addon == null) return;
        try {
            registration.addon.onShutdown(new LoVisualAddonRuntimeContextImpl(registration.descriptor.id()));
        } catch (Throwable t) {
            registration.issue(AddonIssue.Severity.ERROR, "Shutdown failed", t.toString());
            DebugLog.error("Addon shutdown failed: %s", t, registration.descriptor.id());
        }
    }

    /** Materialize (and memoize) the addon instance from its entrypoint container. */
    static LoVisualAddon addonInstance(AddonRegistration registration) {
        LoVisualAddon addon = registration.addon;
        if (addon == null) {
            addon = registration.entrypoint.getEntrypoint();
            registration.addon = addon;
        }
        return addon;
    }
}
