/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.session.microsoft.auth;

public final class DeviceAuthPendingException extends RuntimeException {
    public DeviceAuthPendingException(String message) {
        super(message);
    }

    public DeviceAuthPendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
