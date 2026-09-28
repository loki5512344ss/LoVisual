package dev.loki.lovisual.features.platform;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.primitive.StringValue;

import java.util.List;

/**
 * Runtime-configurable client settings for talking to LoVisual's own backend
 * (accounts-service / gateway), as opposed to third-party endpoints such as
 * Mojang or Microsoft auth. Kept separate from {@link dev.loki.lovisual.config.i18n.MainConfig}
 * (already near the file's line cap) and from {@link dev.loki.lovisual.features.account.AccountConfig}
 * (which stores account *data*, not connection settings), so future
 * mod-backend integration code has one obvious place to read the configured
 * base URL. No HTTP client or auth flow lives here; that is out of scope
 * until the actual integration work starts.
 */
public final class PlatformConfig implements JsonConfigObject, ConfigNameProvider, SettingOwner {

    public static final PlatformConfig INSTANCE = new PlatformConfig();

    /**
     * No production domain has been decided anywhere in the repo yet (checked
     * frontend/.env.example, backend/PLAN.md, backend/gateway/PLAN.md,
     * backend/STRUCTURE.md and TODO.md). This is a clearly-marked placeholder,
     * not a real address; set it in the ClickGUI or config file once a real
     * host exists.
     */
    public static final String DEFAULT_BACKEND_BASE_URL = "https://api.lovisual.example";

    private final StringValue backendBaseUrl = new StringValue("backendBaseUrl", DEFAULT_BACKEND_BASE_URL);

    private PlatformConfig() {
        ConfigSerializer.load(this);
    }

    public static PlatformConfig get() {
        return INSTANCE;
    }

    /** Normalized, always-usable base URL: trimmed, no trailing slash, falls back to the default when invalid. */
    public String getBackendBaseUrl() {
        return BackendEndpoint.normalize(backendBaseUrl.get(), DEFAULT_BACKEND_BASE_URL);
    }

    public void setBackendBaseUrl(String url) {
        backendBaseUrl.set(url == null ? "" : url);
        ConfigSerializer.requestSave(this);
    }

    public List<SettingDef> getSettingDefs() {
        return List.of(SettingDef.text(backendBaseUrl));
    }

    @Override
    public String getConfigName() {
        return "platform";
    }

    @Override
    public String name() {
        return "platform_config";
    }

    @Override
    public void saveConfig() {
        ConfigSerializer.requestSave(this);
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        return List.of(backendBaseUrl);
    }
}
