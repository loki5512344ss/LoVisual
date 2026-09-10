/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.render.engine.color.RenderColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.Locale;
import java.util.UUID;

public final class CustomBarModel {

    private CustomBarModel() {
    }

    public enum BarMode implements EnumValue.IdProvider {
        LEVEL("level"),
        LOCATOR("locator"),
        AUTO("auto");

        private final String id;

        BarMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum XpBarStyle implements EnumValue.IdProvider {
        GRADIENT("gradient"),
        RAINBOW("rainbow"),
        CUSTOM("custom");

        private final String id;

        XpBarStyle(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum LocatorLabelMode implements EnumValue.IdProvider {
        ALWAYS("always"),
        CENTER("center"),
        TARGETS("targets"),
        CENTER_TARGETS("center_targets");

        private final String id;

        LocatorLabelMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public static final class XpTextInfo {
        public final String text;
        public final int x;
        public final int y;
        public final int color;
        public final float scale;

        XpTextInfo(String text, int x, int y, int color, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.scale = scale;
        }
    }

    public record LocatorLabel(String name, String distance, float alpha, boolean target, boolean active, String sort) {

    }

    public record BarColors(int track,

                     int fillStart,
                     int fillEnd,
                     int stroke,
                     int trackTexture,
                     int fillTexture,
                     int locatorTexture) {
    }

    public static final class LocatorCandidate {

        public final TrackedWaypoint waypoint;

        public final UUID uuid;

        public final float yaw;

        public final float dist;

        public final String name;

        public final String nameSort;

        public final String key;

        public final String keySort;


        public LocatorCandidate(TrackedWaypoint waypoint, UUID uuid, float yaw, float dist, String name, String key) {

            this.waypoint = waypoint;
            this.uuid = uuid;
            this.yaw = yaw;
            this.dist = dist;
            this.name = name != null ? name : "";
            this.nameSort = this.name.toLowerCase(Locale.ROOT);
            this.key = key;
            this.keySort = key != null ? key : "";
        }
    }

    public record LocatorHead(float x, float y, float size, float radius, AbstractClientPlayer player,

                       Identifier skin, RenderColor color, RenderColor outlineColor, float outlineThickness,
                       boolean target) {
    }
}
