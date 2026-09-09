/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.wav;

import net.minecraft.resources.Identifier;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;

/**
 * Helper for modules: WAV sound entry with per-sound volume setting.
 */
public class WavSound {

    private final Identifier id;
    private final NumberValue<Double> volume;

    public WavSound(String name, Identifier id, double defVolume) {
        this.id = id;
        this.volume = new NumberValue<>(name + "_vol", defVolume, 0.0, 2.0);
    }

    public Identifier getId() {
        return id;
    }

    public ConfigValue<?> getVolumeValue() {
        return volume;
    }

    public Setting createVolumeSetting(String label) {
        return new SliderSetting(label, volume);
    }

    public void play(double pitch, boolean loop, boolean relative) {
        CustomSoundEngine.get().play(id, volume.get(), pitch, loop, relative);
    }

    public void play() {
        play(1.0, false, true);
    }
}
