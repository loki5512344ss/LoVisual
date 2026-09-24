package dev.loki.lovisual.addon.core;

import dev.loki.lovisual.api.v0.addon.LoVisualAddon;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.addon.core.AddonDescriptor;
import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.addon.pipeline.AddonStatus;

public final class AddonRegistration {
    public final AddonDescriptor descriptor;

    public final EntrypointContainer<LoVisualAddon> entrypoint;

    public final List<String> modules = new ArrayList<>();

    public final List<String> disabledBuiltInModules = new ArrayList<>();

    public final List<String> draggableHudElements = new ArrayList<>();

    public final List<String> staticHudElements = new ArrayList<>();

    public final List<String> commands = new ArrayList<>();

    public final List<String> clickGuiSections = new ArrayList<>();

    public final List<String> moduleExtensions = new ArrayList<>();

    public final List<String> irisPatchManifests = new ArrayList<>();

    public final List<String> renderCallbacks = new ArrayList<>();

    public final List<String> postProcessPasses = new ArrayList<>();

    final List<AddonIssue> issues = new ArrayList<>();
    final Map<String, Boolean> suspendedModules = new LinkedHashMap<>();
    final Map<String, Boolean> suspendedDraggableHudElements = new LinkedHashMap<>();
    final Map<String, Boolean> suspendedStaticHudElements = new LinkedHashMap<>();
    LoVisualAddon addon;
    boolean moduleConfigurationApplied;
    boolean initialized;
    boolean clientReadyNotified;
    AddonStatus status;
    boolean enabled;
    public boolean restartRequired;


    AddonRegistration(AddonDescriptor descriptor,
                      EntrypointContainer<LoVisualAddon> entrypoint,
                      boolean enabled,
                      AddonStatus status) {
        this.descriptor = descriptor;
        this.entrypoint = entrypoint;
        this.enabled = enabled;
        this.status = status;
    }

    AddonSnapshot snapshot() {
        return new AddonSnapshot(
                descriptor.id(),
                descriptor.name(),
                descriptor.version(),
                descriptor.description(),
                descriptor.authors(),
                descriptor.iconPath(),
                descriptor.apiVersion(),
                status,
                enabled,
                restartRequired,
                modules.size(),
                draggableHudElements.size(),
                staticHudElements.size(),
                commands.size(),
                clickGuiSections.size(),
                moduleExtensions.size(),
                irisPatchManifests.size(),
                List.copyOf(issues)
        );
    }

    public void issue(AddonIssue.Severity severity, String message, String detail) {

        issues.add(new AddonIssue(descriptor.id(), severity, message, detail));
    }
}
