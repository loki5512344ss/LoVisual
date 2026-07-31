/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.core;

public final class UiRuntimeException extends RuntimeException {
    public UiRuntimeException(String message) {
        super(message);
    }

    public UiRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
