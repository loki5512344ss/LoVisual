package dev.loki.lovisual.addon.core;

public record AddonIssue(
        String addonId,
        Severity severity,
        String message,
        String detail
) {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
