package dev.loki.lovisual.addon.core;

import dev.loki.lovisual.api.v0.addon.LoVisualAddon;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.List;

/**
 * Builds an {@link AddonDescriptor} from Fabric mod metadata and validates a registration's
 * sanity (API-version match, icon and entrypoint presence), recording findings as
 * {@link AddonIssue}s on the registration. Pure helpers over data the caller already holds,
 * extracted from {@link AddonManager} to keep it under the file-size cap. Shares
 * {@code addon.core}'s package so {@link AddonManager#normalizeId} stays reachable.
 */
final class AddonDescriptors {

    private AddonDescriptors() { }

    /** Parse a descriptor from the mod's metadata plus its resolved entrypoint definition. */
    static AddonDescriptor descriptor(ModMetadata metadata, String entrypointDefinition) {
        List<String> authors = metadata.getAuthors().stream()
                .map(person -> person.getName() == null ? "" : person.getName())
                .filter(name -> !name.isBlank())
                .toList();
        return new AddonDescriptor(
                AddonManager.normalizeId(metadata.getId()),
                blankFallback(metadata.getName(), metadata.getId()),
                String.valueOf(metadata.getVersion()),
                metadata.getDescription() == null ? "" : metadata.getDescription(),
                authors,
                metadata.getIconPath(64).or(() -> metadata.getIconPath(32)).orElse(""),
                apiVersion(metadata),
                entrypointDefinition
        );
    }

    /** Read the declared LoVisual API version from the {@code lovisual:addon} custom block. */
    static int apiVersion(ModMetadata metadata) {
        CustomValue value = metadata.getCustomValue(AddonManager.ADDON_METADATA_KEY);
        if (value == null || value.getType() != CustomValue.CvType.OBJECT) return LoVisualAddon.API_VERSION;
        CustomValue api = value.getAsObject().get("api");
        if (api == null || api.getType() != CustomValue.CvType.NUMBER) return LoVisualAddon.API_VERSION;
        return api.getAsNumber().intValue();
    }

    /** Flag API-version drift, a missing icon and an unavailable entrypoint definition. */
    static void validate(AddonRegistration registration) {
        if (registration.descriptor.apiVersion() != LoVisualAddon.API_VERSION) {
            registration.issue(
                    AddonIssue.Severity.WARNING,
                    "Addon API version differs from client API",
                    "addon=" + registration.descriptor.apiVersion() + ", client=" + LoVisualAddon.API_VERSION
            );
        }
        if (registration.descriptor.iconPath() == null || registration.descriptor.iconPath().isBlank()) {
            registration.issue(AddonIssue.Severity.WARNING, "Addon icon is missing", "Add icon in fabric.mod.json");
        }
        if (registration.descriptor.entrypointDefinition() == null
                || registration.descriptor.entrypointDefinition().isBlank()) {
            registration.issue(AddonIssue.Severity.WARNING, "Entrypoint definition is unavailable", "");
        }
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
