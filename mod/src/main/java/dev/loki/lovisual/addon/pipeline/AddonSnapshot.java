package dev.loki.lovisual.addon.pipeline;

import java.util.List;
import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.pipeline.AddonStatus;

public record AddonSnapshot(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        String iconPath,
        int apiVersion,
        AddonStatus status,
        boolean enabled,
        boolean restartRequired,
        int modules,
        int draggableHudElements,
        int staticHudElements,
        int commands,
        int clickGuiSections,
        int moduleExtensions,
        int irisPatchManifests,
        List<AddonIssue> issues
) {
}
