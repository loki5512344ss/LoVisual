/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module;

import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;

public enum Notifier {
    ;

    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    public static void info(String text) {
        message(text, Type.INFO);
    }

    public static void success(String text) {
        message(text, Type.SUCCESS);
    }

    public static void warning(String text) {
        message(text, Type.WARNING);
    }

    public static void error(String text) {
        message(text, Type.ERROR);
    }

    public static void message(String text, Type type) {
        HudNotifier.pushMessage(text, toHudType(type));
    }

    public static void state(String text, boolean enabled) {
        HudNotifier.pushState(text, enabled);
    }

    public static void update(String key, String text) {
        HudNotifier.pushOrUpdateMessage(key, text);
    }

    public static void update(String key, String text, Type type) {
        HudNotifier.pushOrUpdateMessage(key, text, toHudType(type));
    }

    public static void clear(String key) {
        HudNotifier.clearMessage(key);
    }

    private static HudNotifier.NotifyType toHudType(Type type) {
        return switch (type == null ? Type.INFO : type) {
            case SUCCESS -> HudNotifier.NotifyType.YES;
            case ERROR -> HudNotifier.NotifyType.NO;
            case WARNING -> HudNotifier.NotifyType.WARN;
            case INFO -> HudNotifier.NotifyType.INFO;
        };
    }
}
