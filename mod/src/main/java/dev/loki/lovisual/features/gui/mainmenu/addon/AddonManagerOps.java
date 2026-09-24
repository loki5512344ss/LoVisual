package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class AddonManagerOps {
    private AddonManagerOps() {
    }

    static boolean openPath(Path path, AddonManagerController controller) {
        try {
            Files.createDirectories(path);
            Util.getPlatform().openUri(path.toUri());
            return true;
        } catch (Throwable ignored) {
            controller.setStatus(AddonUiMetrics.tr("screen.lovisual.addon_manager.status.open_failed"));
            return false;
        }
    }

    static String addonIcon(AddonSnapshot addon) {
        return switch (addon.status()) {
            case ACTIVE -> addon.enabled() ? "folder-check" : "folder";
            case ERROR -> "folder-x";
            case DISABLED -> "folder";
            case SUSPENDED -> "folder-clock";
            case SHUTDOWN -> "folder-output";
            default -> "package";
        };
    }

    static String statusText(AddonSnapshot addon) {
        if (!addon.enabled()) return AddonUiMetrics.tr("screen.lovisual.addon_manager.status.disabled_short");
        return switch (addon.status()) {
            case ACTIVE -> AddonUiMetrics.tr("screen.lovisual.addon_manager.state.active");
            case ERROR -> AddonUiMetrics.tr("screen.lovisual.addon_manager.state.error");
            case DISABLED -> AddonUiMetrics.tr("screen.lovisual.addon_manager.state.disabled");
            case SUSPENDED -> AddonUiMetrics.tr("screen.lovisual.addon_manager.state.suspended");
            case SHUTDOWN -> AddonUiMetrics.tr("screen.lovisual.addon_manager.state.shutdown");
            default -> addon.status().name().toLowerCase(Locale.ROOT);
        };
    }

    static String countsText(AddonSnapshot addon) {
        int hud = addon.draggableHudElements() + addon.staticHudElements();
        return AddonUiMetrics.tr("screen.lovisual.addon_manager.metric.modules") + " " + addon.modules()
                + " \u00b7 " + AddonUiMetrics.tr("screen.lovisual.addon_manager.metric.hud") + " " + hud
                + " \u00b7 " + AddonUiMetrics.tr("screen.lovisual.addon_manager.metric.commands") + " " + addon.commands()
                + " \u00b7 " + AddonUiMetrics.tr("screen.lovisual.addon_manager.metric.gui") + " " + addon.clickGuiSections();
    }

    static int statusColor(AddonSnapshot addon, AddonUiMetrics.PanelColors c) {
        return switch (addon.status()) {
            case ACTIVE -> c.accent();
            case ERROR -> AddonUiMetrics.withAlpha(0xFF8888, 255);
            case DISABLED -> c.muted();
            default -> c.label();
        };
    }

    static String addonInfo(AddonSnapshot addon) {
        if (addon == null) return "";
        AddonIssue issue = primaryIssue(addon);
        if (issue != null) return issueText(issue);
        if (addon.restartRequired()) return AddonUiMetrics.tr("screen.lovisual.addon_manager.restart_required");
        return addonDescription(addon);
    }

    static AddonIssue primaryIssue(AddonSnapshot addon) {
        AddonIssue fallback = null;
        for (AddonIssue issue : addon.issues()) {
            if (issue.severity() == AddonIssue.Severity.ERROR) return issue;
            if (fallback == null && issue.severity() == AddonIssue.Severity.WARNING) fallback = issue;
        }
        return fallback;
    }

    static String issueText(AddonIssue issue) {
        if (issue == null) return "";
        String message = issue.message() == null ? "" : issue.message().trim();
        String detail = issue.detail() == null ? "" : issue.detail().trim();
        String body = !message.isEmpty() ? message : detail;
        if (!message.isEmpty() && !detail.isEmpty()) body = message + " \u00b7 " + detail;
        if (body.isEmpty()) body = issue.severity().name().toLowerCase(Locale.ROOT);
        return body;
    }

    static String addonDescription(AddonSnapshot addon) {
        if (addon == null || addon.description() == null) return "";
        return addon.description().trim();
    }

    static int listColumns(int count, float listW) {
        if (count <= 1) return 1;
        return listW >= 250f * AddonUiMetrics.SCALE ? 2 : 1;
    }

    static int rowsFor(int count, int columns) {
        if (count <= 0) return 0;
        return (count + Math.max(1, columns) - 1) / Math.max(1, columns);
    }

    static float cardWidth(float listW, int columns) {
        int safeColumns = Math.max(1, columns);
        return safeColumns == 1 ? listW : (listW - AddonUiMetrics.CARD_GAP * (safeColumns - 1)) / safeColumns;
    }
}
