package dev.loki.lovisual.util.session.microsoft.auth;

public final class DeviceAuthPendingException extends RuntimeException {
    public DeviceAuthPendingException(String message) {
        super(message);
    }

    public DeviceAuthPendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
