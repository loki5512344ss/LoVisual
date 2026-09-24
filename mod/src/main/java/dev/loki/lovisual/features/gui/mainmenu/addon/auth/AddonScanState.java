package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.core.AddonManager;
import net.minecraft.util.Util;

import java.util.List;

final class AddonScanState {
    private List<AddonIssue> scanIssues = List.of();
    private String status = "";
    private long statusUntilMillis;

    List<AddonIssue> scanIssues() {
        return scanIssues;
    }

    String status() {
        return status;
    }

    boolean statusVisible() {
        return !status.isBlank() && (statusUntilMillis <= 0L || Util.getMillis() <= statusUntilMillis);
    }

    void setStatus(String text) {
        if (text == null || text.isBlank()) {
            status = "";
            statusUntilMillis = 0L;
            return;
        }
        status = text;
        statusUntilMillis = Util.getMillis() + 1600L;
    }

    void tickStatusExpiry() {
        if (!status.isBlank() && statusUntilMillis > 0L && Util.getMillis() > statusUntilMillis) {
            status = "";
            statusUntilMillis = 0L;
        }
    }

    void scan() {
        scanIssues = AddonManager.scan();
        if (scanIssues.isEmpty()) setStatus("");
        else setStatus(AddonUiMetrics.tr("screen.lovisual.addon_manager.status.scanned", scanIssues.size()));
    }

    void clearIssues() {
        scanIssues = List.of();
    }
}
