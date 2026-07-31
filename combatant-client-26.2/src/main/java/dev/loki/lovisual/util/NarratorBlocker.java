/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.Options;
import dev.loki.lovisual.config.MainConfig;

//Да ну нахуй эту залупу блять
public enum NarratorBlocker {
    ;

    private static boolean applying;

    public static boolean isBlocked() {
        return MainConfig.get().isNarratorDisabled();
    }

    public static void enforce(Minecraft client) {
        if (!isBlocked() || client == null) return;
        enforce(client.options);
        try {
            client.getNarrator().clear();
        } catch (Throwable ignored) {
        }
    }

    public static void enforce(Options options) {
        if (!isBlocked() || options == null || applying) return;
        applying = true;
        try {
            if (options.narrator().get() != NarratorStatus.OFF) {
                options.narrator().set(NarratorStatus.OFF);
            }
            if (options.narratorHotkey().get()) {
                options.narratorHotkey().set(false);
            }
        } finally {
            applying = false;
        }
    }
}
