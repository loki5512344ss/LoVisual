package dev.loki.lovisual.features.platform;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.primitive.StringValue;

import java.util.List;

/**
 * Persists the mod's link to a LoVisual platform account: only the opaque
 * {@code lvd_...} device token handed out by {@code POST /device/token},
 * never a password or a site access/refresh token. Structured the same way
 * as {@link PlatformConfig} (a {@link JsonConfigObject} singleton through
 * {@link ConfigSerializer}), stored separately from it because a session can
 * be cleared (unlink) independently of the configured backend URL.
 */
public final class PlatformSession implements JsonConfigObject, ConfigNameProvider, SettingOwner {

    public static final PlatformSession INSTANCE = new PlatformSession();

    private final StringValue deviceToken = new StringValue("deviceToken", "");

    private PlatformSession() {
        ConfigSerializer.load(this);
    }

    public static PlatformSession get() {
        return INSTANCE;
    }

    public boolean isLinked() {
        return !deviceToken.get().isBlank();
    }

    public String getDeviceToken() {
        return deviceToken.get();
    }

    public void setDeviceToken(String token) {
        deviceToken.set(token == null ? "" : token.trim());
        ConfigSerializer.requestSave(this);
    }

    public void clear() {
        setDeviceToken("");
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        return List.of(deviceToken);
    }

    @Override
    public String getConfigName() {
        return "platform_session";
    }

    @Override
    public String name() {
        return "platform_session";
    }

    @Override
    public void saveConfig() {
        ConfigSerializer.requestSave(this);
    }
}
