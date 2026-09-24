package dev.loki.lovisual.render.engine.renderer.ui.runtime.core;

public final class UiRuntimeException extends RuntimeException {
    public UiRuntimeException(String message) {
        super(message);
    }

    public UiRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
