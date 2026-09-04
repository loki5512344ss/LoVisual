/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.addon;

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
