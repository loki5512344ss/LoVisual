/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc;

import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.SetValue;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;

import java.util.Locale;
import java.util.Set;

//todo Description
@ModuleInfo(
        id = "nosound",
        displayName = "NoSound",
        category = ModuleCategory.MISC
)
public class NoSound extends Module {
    private static final String SETTING_SOUND_IDS = "sound_ids";
    private static final String SETTING_MODE = "mode";
    private static final String SETTING_VOLUME_SCALE = "volume_scale";

    private final SetValue soundIds = textList("noSoundIds", SETTING_SOUND_IDS, TextListSetting.PickerMode.SOUNDS);
    private final ModeValue mode = mode("noSoundMode", SETTING_MODE,
            Mode.MUTE.id, Mode.REDUCE.id);
    private final NumberValue<Integer> volumeScale =
            visibleWhen(num("noSoundVolumeScale", SETTING_VOLUME_SCALE, 30, 1, 100),
                    () -> Mode.REDUCE.id.equals(mode.get()));

    {
        soundIds.set(Set.of("minecraft:entity.ender_dragon.growl"));
    }

    public enum Mode {
        MUTE("Mute"),
        REDUCE("Reduce");

        public final String id;

        Mode(String id) {
            this.id = id;
        }
    }

    public boolean shouldMute(String id) {
        return isEnabled() && Mode.MUTE.id.equals(mode.get()) && matches(id);
    }

    /**
     * Volume multiplier for the sound engine: 1.0 unless the sound is listed
     * and the module is in reduce mode.
     */
    public float volumeScale(String id) {
        if (!isEnabled() || !Mode.REDUCE.id.equals(mode.get())) return 1.0f;
        return resolveScale(matches(id), volumeScale.get());
    }

    private boolean matches(String id) {
        if (id == null || id.isEmpty()) return false;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (String mutedId : soundIds.get()) {
            if (mutedId != null && mutedId.toLowerCase(Locale.ROOT).equals(normalized)) return true;
        }
        return false;
    }

    /** Pure scale math shared with the sound-engine mixin. */
    public static float resolveScale(boolean matches, int scalePct) {
        if (!matches) return 1.0f;
        return Math.max(0, Math.min(100, scalePct)) / 100.0f;
    }
}
