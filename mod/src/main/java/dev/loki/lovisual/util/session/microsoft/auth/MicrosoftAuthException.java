package dev.loki.lovisual.util.session.microsoft.auth;

public class MicrosoftAuthException extends RuntimeException {
    private final String key;

    public MicrosoftAuthException(String message) {
        this(message, null, "lovisual.auth.microsoft.error");
    }

    public MicrosoftAuthException(String message, Throwable cause) {
        this(message, cause, "lovisual.auth.microsoft.error");
    }

    public MicrosoftAuthException(String message, String key) {
        this(message, null, key);
    }

    public MicrosoftAuthException(String message, Throwable cause, String key) {
        super(message + (key == null || key.isBlank() ? "" : " (key: " + key + ")"), cause);
        this.key = key == null ? "lovisual.auth.microsoft.error" : key;
    }

    public String key() {
        return key;
    }
}
