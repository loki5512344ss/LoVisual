package dev.loki.lovisual.addon.core;

import java.util.List;

public record AddonDescriptor(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        String iconPath,
        int apiVersion,
        String entrypointDefinition
) {
}
