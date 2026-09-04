/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.media.impl.linux;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import dev.loki.lovisual.util.media.IMediaSession;
import dev.loki.lovisual.util.media.MediaPlayerInfo;
import dev.loki.lovisual.util.media.impl.linux.dbus.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public final class LinuxMediaPlayerInfo implements MediaPlayerInfo {
    public static final LinuxMediaPlayerInfo INSTANCE = new LinuxMediaPlayerInfo();

    private final DBusConnection conn;
    private final DBus dbus;

    private LinuxMediaPlayerInfo() {
        DBusConnection connection = null;
        DBus dbusObject = null;
        try {
            connection = DBusConnectionBuilder.forSessionBus().build();
            dbusObject = connection.getRemoteObject("org.freedesktop.DBus", "/", DBus.class);
        } catch (Exception ignored) {
        }
        this.conn = connection;
        this.dbus = dbusObject;
    }

    @Override
    public List<IMediaSession> getMediaSessions() {
        if (conn == null || dbus == null) return List.of();

        String[] names;
        try {
            names = dbus.ListNames();
        } catch (Exception ignored) {
            return List.of();
        }

        List<IMediaSession> sessions = new ArrayList<>();
        for (String name : names) {
            if (!name.startsWith("org.mpris.MediaPlayer2.")) continue;
            String owner = name.substring("org.mpris.MediaPlayer2.".length());
            String status;
            try {
                status = getProperty(owner, "PlaybackStatus");
            } catch (Exception ignored) {
                status = null;
            }
            if ("Stopped".equals(status)) continue;

            try {
                Player player = conn.getRemoteObject(name, "/org/mpris/MediaPlayer2", Player.class);
                sessions.add(new LinuxMediaSession(player, owner));
            } catch (Exception ignored) {
            }
        }
        return sessions;
    }

    @SuppressWarnings("unchecked")
    <T> T getProperty(String owner, String property) throws DBusException {
        Properties properties = conn.getRemoteObject(
                "org.mpris.MediaPlayer2." + owner,
                "/org/mpris/MediaPlayer2",
                Properties.class
        );
        return properties.Get("org.mpris.MediaPlayer2.Player", property);
    }

    void setProperty(String owner, String property, Object value) {
        try {
            Properties properties = conn.getRemoteObject(
                    "org.mpris.MediaPlayer2." + owner,
                    "/org/mpris/MediaPlayer2",
                    Properties.class
            );
            properties.Set("org.mpris.MediaPlayer2.Player", property, new Variant<>(value));
        } catch (Exception ignored) {
        }
    }
}
