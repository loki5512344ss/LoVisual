package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.util.Util;

/**
 * Owns the "current theme" pointer plus the cross-fade animation state used when
 * the user switches themes. Every accessor on {@link Themes} that returns a
 * {@code Theme} or {@code ThemeEntry} goes through {@link #animatedTheme()} /
 * {@link #animatedEntry()} so the visible UI blends smoothly between selections.
 * <p>
 * Lives in a sibling folder to keep the main theme directory within the
 * four-file-per-directory limit; the package is shared with {@link Themes} so
 * all mutable state stays package-private there.
 */
final class ThemesTransition {

    /** Total cross-fade duration in milliseconds; matches {@code Themes.THEME_TRANSITION_MS}. */
    ThemesTransition() { }

    /** Length of the shader wipe that replaces the old backdrop with the new one. */
    static final long WIPE_MS = 700L;
    private static long wipeStartMs = -WIPE_MS;
    private static Backdrop wipeFrom = Backdrop.NONE;

    /** Which backdrops to draw now: the target, the one it replaces, and the wipe progress (1 when finished). */
    record BackdropFrame(Backdrop target, Backdrop outgoing, float progress) {
        boolean wiping() {
            return progress < 1.0f;
        }
    }

    static BackdropFrame backdropFrame() {
        Backdrop target = Themes.currentEntry.backdrop();
        long elapsed = Util.getMillis() - wipeStartMs;
        if (elapsed >= WIPE_MS || elapsed < 0L) return new BackdropFrame(target, Backdrop.NONE, 1.0f);
        return new BackdropFrame(target, wipeFrom, AnimationUtility.easeOutCubic((float) elapsed / (float) WIPE_MS));
    }

    /** Current theme with any in-flight transition applied; safe to call every frame. */
    static Theme theme() {
        return animatedTheme();
    }

    /** Current entry (theme + five gradient slots) with the transition blend applied. */
    static ThemeEntry currentEntry() {
        return animatedEntry();
    }

    /** Switch to the theme stored under the given preset / custom id, or fall back to classic. */
    static void setTheme(String id) {
        if (id == null || id.isBlank()) {
            boolean animate = !Themes.THEME_DEFAULT.equals(Themes.currentId) || Themes.transitionActive;
            applyThemeFromEntry(ThemePresets1.AMBER_ENTRY, animate);
            Themes.STORE.setCurrentId(Themes.THEME_DEFAULT);
            return;
        }
        String key = id.toLowerCase();
        ThemeEntry entry = Themes.STORE.getById(key);
        if (entry == null) entry = Themes.PRESET_ENTRIES.get(key);
        if (entry == null) entry = ThemePresets1.AMBER_ENTRY;
        boolean animate = !entry.getId().equals(Themes.currentId) || Themes.transitionActive;
        applyThemeFromEntry(entry, animate);
        Themes.STORE.setCurrentId(entry.getId());
    }

    /** Push a raw {@link Theme} instance (not registered as a preset) and start the fade. */
    static void setTheme(Theme t) {
        if (t == null) {
            setTheme(Themes.THEME_CLASSIC);
            return;
        }
        for (ThemeEntry entry : Themes.PRESET_ENTRIES.values()) {
            if (entry.theme() == t) {
                setTheme(entry.getId());
                return;
            }
        }
        Theme from = displayedThemeRaw();
        ThemeEntry fromEntry = displayedEntryRaw();
        Themes.current = t;
        Themes.currentId = Themes.THEME_CLASSIC;
        Themes.currentEntry = new ThemeEntry(
                Themes.THEME_CLASSIC, "Custom", false, t,
                ThemesBlending.flatGradient(t.windowBg()),
                ThemesBlending.subtleHeaderGradient(t.windowHeader()),
                ThemesBlending.flatGradient(t.surface()),
                ThemesBlending.flatGradient(t.cardEnabled()),
                ThemesBlending.flatGradient(t.windowStroke())
        );
        beginThemeTransition(from, fromEntry);
    }

    /** Bootstrap hook: read the persisted id from the store and snap to it without animating. */
    static void applyFromStore() {
        String id = Themes.STORE.getCurrentId();
        ThemeEntry entry = Themes.STORE.getById(id);
        if (entry == null) entry = Themes.PRESET_ENTRIES.get(Themes.THEME_DEFAULT);
        if (entry == null) entry = ThemePresets1.AMBER_ENTRY;
        applyThemeFromEntry(entry, false);
    }

    private static void applyThemeFromEntry(ThemeEntry entry, boolean animate) {
        if (entry == null) entry = ThemePresets1.CLASSIC_ENTRY;
        if (animate) {
            beginThemeTransition(displayedThemeRaw(), displayedEntryRaw());
        } else {
            Themes.transitionActive = false;
            Themes.animatedCacheKey = -1;
        }
        Themes.current = entry.theme();
        Themes.currentId = entry.getId();
        Themes.currentEntry = entry;
        if (!animate) {
            Themes.animatedThemeCache = Themes.current;
            Themes.animatedEntryCache = Themes.currentEntry;
        }
    }

    private static void beginThemeTransition(Theme fromTheme, ThemeEntry fromEntry) {
        Themes.transitionFromTheme = fromTheme != null ? fromTheme : Themes.current;
        Themes.transitionFromEntry = fromEntry != null ? fromEntry : Themes.currentEntry;
        Themes.transitionStartMs = Util.getMillis();
        wipeFrom = Themes.currentEntry.backdrop();
        wipeStartMs = Themes.transitionStartMs;
        Themes.transitionActive = true;
        Themes.animatedCacheKey = -1;
    }

    private static Theme displayedThemeRaw() {
        if (!Themes.transitionActive) return Themes.current;
        return animatedTheme();
    }

    private static ThemeEntry displayedEntryRaw() {
        if (!Themes.transitionActive) return Themes.currentEntry;
        return animatedEntry();
    }

    /** Blend of the previous and new {@link Theme} at the current transition progress. */
    private static Theme animatedTheme() {
        if (!Themes.transitionActive) return Themes.current;
        float progress = transitionProgress();
        if (!Themes.transitionActive) return Themes.current;
        int key = Math.round(progress * 1024.0f);
        if (key == Themes.animatedCacheKey && Themes.animatedThemeCache != null) return Themes.animatedThemeCache;
        Themes.animatedThemeCache = ThemesBlending.blendTheme(Themes.transitionFromTheme, Themes.current, progress);
        Themes.animatedEntryCache = null;
        Themes.animatedCacheKey = key;
        return Themes.animatedThemeCache;
    }

    /** Same blend, extended to the five gradient slots so headers / cards animate together. */
    private static ThemeEntry animatedEntry() {
        if (!Themes.transitionActive) return Themes.currentEntry;
        float progress = transitionProgress();
        if (!Themes.transitionActive) return Themes.currentEntry;
        int key = Math.round(progress * 1024.0f);
        if (key != Themes.animatedCacheKey || Themes.animatedThemeCache == null) {
            Themes.animatedThemeCache = ThemesBlending.blendTheme(Themes.transitionFromTheme, Themes.current, progress);
            Themes.animatedEntryCache = null;
            Themes.animatedCacheKey = key;
        }
        if (Themes.animatedEntryCache != null) return Themes.animatedEntryCache;
        Themes.animatedEntryCache = ThemesBlending.blendEntry(Themes.transitionFromEntry, Themes.currentEntry, progress);
        return Themes.animatedEntryCache;
    }

    private static float transitionProgress() {
        long elapsed = Math.max(0L, Util.getMillis() - Themes.transitionStartMs);
        if (elapsed >= Themes.THEME_TRANSITION_MS) {
            Themes.transitionActive = false;
            Themes.animatedThemeCache = Themes.current;
            Themes.animatedEntryCache = Themes.currentEntry;
            Themes.animatedCacheKey = -1;
            return 1.0f;
        }
        float linear = Themes.THEME_TRANSITION_MS <= 0L ? 1.0f : (float) elapsed / (float) Themes.THEME_TRANSITION_MS;
        return AnimationUtility.easeOutCubic(AnimationUtility.clamp01(linear));
    }
}
