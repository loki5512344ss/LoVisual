package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.runtime;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier.NotifyType;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.PreviewToast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.Toast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.ToastVisual;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastStyle;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.util.wav.engine.CustomSoundEngine;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owns the mutable toast queue, the enqueue / clear API and the notification sound helper.
 * <p>
 * Called by {@link HudNotifier} (public static facade) and by the painter at the start of
 * every frame (to prune expired entries and to read the live list). Kept in its own
 * subpackage so the main folder stays within the four-file-per-directory limit.
 */
public final class HudNotifierRuntime {

    /** Hard cap for the visible stack; the oldest toast is dropped when exceeded. */
    public static final int MAX_TOASTS = 6;

    private final HudNotifier owner;
    private final List<Toast> toasts = new ArrayList<>();

    public HudNotifierRuntime(HudNotifier owner) {
        this.owner = owner;
    }

    /** Live, ordered view of the toast stack. Callers must not mutate; use the enqueue / clear APIs. */
    public List<Toast> toasts() {
        return toasts;
    }

    /** Drop every toast whose lifetime has fully elapsed (including the fade-out tail). */
    public void pruneExpiredToasts(long now, int duration) {
        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            if (t.isExpired(now, duration)) it.remove();
        }
    }

    /** Push a module-toggle notification with a fixed YES / NO visual. */
    public void enqueueModuleState(String text, boolean enabled) {
        enqueueInternal(null, text, enabled, enabled ? NotifyType.YES : NotifyType.NO, ToastVisual.MODULE_TOGGLE);
    }

    /** Public push helper used by the HudNotifier facade: picks type / visual from the raw arguments. */
    public void enqueue(String key, String text, Boolean enabled, NotifyType forcedType) {
        NotifyType type = forcedType != null ? forcedType : ToastStyle.resolveType(text, enabled);
        enqueueInternal(key, text, enabled, type, ToastVisual.STANDARD);
    }

    /** Remove every toast that was pushed under the given key. */
    public void clearKey(String key) {
        if (key == null) return;
        toasts.removeIf(t -> key.equals(t.key));
    }

    private void enqueueInternal(String key, String text, Boolean enabled, NotifyType type, ToastVisual visual) {
        if (text == null || text.isBlank()) return;
        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;
        if (!owner.enabledValue().get()) return;
        if (hud.getHeaderMode() == HudGlobalConfig.HeaderMode.NEVER) return;

        String line = ToastStyle.resolveLine(text, enabled);
        long now = System.currentTimeMillis();

        if (key != null) {
            for (Toast toast : toasts) {
                if (key.equals(toast.key)) {
                    toast.line = line;
                    toast.type = type;
                    toast.visual = visual == null ? ToastVisual.STANDARD : visual;
                    toast.stateEnabled = enabled;
                    toast.startMs = now;
                    return;
                }
            }
        }

        toasts.add(new Toast(key, line, type, now, visual, enabled));
        if (toasts.size() > MAX_TOASTS) toasts.remove(0);
        if (enabled != null) playSound(enabled);
    }

    private void playSound(boolean enabled) {
        if (ModuleManager.isToggleSoundSuppressed()) return;
        if (!owner.isSoundEnabled()) return;
        ModeValue mode = owner.getSoundMode();
        Identifier snd = resolveSound(enabled, mode == null ? "2" : mode.get());
        if (snd == null) return;
        double vol = Math.min(1.0, owner.getSoundVolume());
        CustomSoundEngine.get().play(snd, vol, 1.0, false, true);
    }

    private static Identifier resolveSound(boolean enabled, String mode) {
        String base = switch (mode) {
            case "2" -> "enable/enable2";
            case "3" -> "enable/enable3";
            default -> "enable/enable1";
        };
        if (!enabled) base = base.replace("enable/", "disable/").replace("enable", "disable");
        return Identifier.fromNamespaceAndPath("lovisual", "sounds/" + base + ".wav");
    }

    /** Sample toast used by the drag-preview overlay when the real stack is empty. */
    public static Toast previewToast(long now) {
        PreviewToast preview = ToastModel.previewExample(now);
        long start = ToastModel.previewStart(now);
        return new Toast(ToastModel.PREVIEW_KEY, preview.line(), preview.type(), start, preview.visual(), preview.enabled());
    }
}
