package dev.loki.lovisual.features.gui.clickgui.settings.core;

import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.values.collection.ConfigValue;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for every ClickGUI setting widget. Owns the underlying {@link ConfigValue}, the
 * show/hide visibility animation and the input/render contract subclasses implement. All i18n
 * resolution and translation-key derivation is delegated to {@link SettingLocalization} (per
 * instance) and {@link SettingI18n} (process-wide); this class keeps only the stable API used
 * by the settings panels and by the concrete setting types.
 */
public abstract class Setting {

    protected static final float LABEL_PAD = 6f;
    protected final ConfigValue<?> value;
    private final String name;
    private final SettingLocalization localization;
    private Supplier<Boolean> visibility = () -> true;
    private Supplier<String> unavailableReason = () -> null;
    private boolean lastVisible = true;
    private float visibilityAnim = 1f;
    private long lastAnimTime = System.nanoTime();

    public Setting(String name, ConfigValue<?> value) {
        this.name = name;
        this.value = value;
        this.localization = new SettingLocalization(this);
    }

    public Setting(String name) {
        this.name = name;
        this.value = null;
        this.localization = new SettingLocalization(this);
    }

    protected static float labelX(float x) {
        return x + LABEL_PAD;
    }

    /** Reset the language gate and probe readiness; call before a preflight pass. */
    public static boolean prepareI18nPreflightPass() { return SettingI18n.preparePreflight(); }

    /** Capture missing translation keys reported during the returned handle's lifetime. */
    public static AutoCloseable captureMissingI18n(MissingI18nReporter reporter) { return SettingI18n.captureMissing(reporter); }

    // --- UI ---
    public String getName() { return name; }

    public String getDisplayName() { return localization.getDisplayName(); }

    public String getOptionDisplayName(String optionId) { return localization.getOptionDisplayName(optionId); }

    /** Hover description from {@code <translationKey>.desc}; null when no key exists. */
    public String getDescription() { return localization.getDescription(); }

    public SettingOwner getParent() { return localization.getParent(); }

    public void setParent(SettingOwner owner) { localization.setParent(owner); }

    public String getTranslationKey() { return localization.getTranslationKey(); }

    public List<String> getTranslationKeys() { return localization.getTranslationKeys(); }

    public void setCommonI18nKey(String key) { localization.setCommonI18nKey(key); }

    public void setCommonI18nKeys(Iterable<String> keys) { localization.setCommonI18nKeys(keys); }

    public void setI18nEnabled(boolean name, boolean options) { localization.setI18nEnabled(name, options); }

    /** Trigger i18n lookup so missing keys are logged early. */
    public void preflightI18n() { localization.preflight(); }

    protected void preflightOptionI18n(String optionId) { localization.preflightOption(optionId); }

    public Setting unavailableReason(Supplier<String> supplier) {
        this.unavailableReason = supplier == null ? () -> null : supplier;
        return this;
    }

    public boolean isAvailable() { return getUnavailableReason().isEmpty(); }

    public String getUnavailableReason() {
        try {
            String reason = unavailableReason.get();
            return reason == null ? "" : reason.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public abstract void render(float x, float y, float width, float mouseX, float mouseY);

    public abstract void mouseClicked(double mx, double my, int button);

    public void mouseReleased(double mx, double my, int button) { }

    /** Called by parent panels when a click happens outside this setting hitbox. */
    public void mouseClickedOutside(double mx, double my, int button) { }

    public boolean mouseScrolled(double mx, double my, double amount) { return false; }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    public boolean charTyped(char chr, int modifiers) { return false; }

    public abstract float getHeight();

    /** Unique id used for serialization. */
    public String getId() { return name.toLowerCase().replace(" ", "_"); }

    protected String getTranslationId() { return getId(); }

    public Object save() { return value != null ? value.toJson() : null; }

    public void load(Object o) {
        if (value != null) value.fromJson(o);
    }

    /** Underlying config value (used for persistence). */
    public ConfigValue<?> getConfigValue() { return value; }

    // --- Visibility ---
    public Setting visibleWhen(Supplier<Boolean> supplier) {
        this.visibility = supplier == null ? () -> true : supplier;
        return this;
    }

    public boolean isVisible() {
        try {
            return visibility.get();
        } catch (Exception e) {
            return true;
        }
    }

    /** Smooth visibility progress 0..1 for animating show/hide. */
    public float updateVisibilityAnim() {
        boolean vis = isVisible();
        long now = System.nanoTime();
        float dt = (now - lastAnimTime) / 1_000_000_000f;
        lastAnimTime = now;
        if (dt <= 0f) dt = 0.0001f;
        dt = Math.min(dt, 0.05f); // avoid huge jumps on long pauses
        if (vis && !lastVisible) visibilityAnim = 0f; // start animating from 0 when becoming visible
        float target = vis ? 1f : 0f;
        float speed = 10f; // higher = faster
        float step = Math.min(0.35f, dt * speed); // cap so it never jumps in a single frame
        visibilityAnim += (target - visibilityAnim) * step;
        if (visibilityAnim < 0.001f) visibilityAnim = 0f;
        if (visibilityAnim > 0.999f) visibilityAnim = 1f;
        lastVisible = vis;
        return visibilityAnim;
    }

    public float getVisibilityAnim() { return visibilityAnim; }

    public boolean isVisibilityTargetVisible() { return lastVisible; }

    /** Height scaled by visibility animation, used for layout. */
    public float getAnimatedHeight() { return getHeight() * visibilityAnim; }

    @FunctionalInterface
    public interface MissingI18nReporter {
        void missing(List<String> keyChain);
    }
}
