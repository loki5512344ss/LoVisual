package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.render.engine.color.RenderColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.Locale;
import java.util.UUID;

public final class CustomBarModel {

    // Layout, alpha and vanilla-texture constants shared by the bar/locator/jump renderers.
    public static final String COLOR_THEME = "Theme";
    public static final String COLOR_CUSTOM = "Custom";
    public static final float XP_BAR_WIDTH = 182f;
    public static final double XP_TEXT_SCALE = 0.59;
    public static final int XP_BAR_OFFSET = 2;
    public static final int XP_TEXT_OFFSET = 0;
    public static final float XP_BAR_HEIGHT = 5.0f;
    public static final float XP_BAR_RADIUS = 2.5f;
    public static final float BAR_SOFTNESS = 1.0f;
    public static final float BAR_BLUR_ALPHA = 0.72f;
    public static final float BAR_GLASS_ALPHA = 0.58f;
    public static final float BAR_TRACK_ALPHA = 0.54f;
    public static final float BAR_FILL_ALPHA = 0.82f;
    public static final float BAR_STROKE_ALPHA = 0.48f;
    public static final Identifier XP_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/experience_bar_background");
    public static final Identifier XP_PROGRESS = Identifier.fromNamespaceAndPath("minecraft", "hud/experience_bar_progress");
    public static final Identifier LOCATOR_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_background");
    public static final Identifier JUMP_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_background");
    public static final Identifier JUMP_COOLDOWN = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_cooldown");
    public static final Identifier JUMP_PROGRESS = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_progress");
    public static final float LOCATOR_BAR_HEIGHT = XP_BAR_HEIGHT;
    public static final int LOCATOR_TICK_COUNT = 6;
    public static final float LOCATOR_HEAD_SIZE = 6.5f;
    public static final float LOCATOR_HEAD_RADIUS = 1.6f;
    public static final float LOCATOR_HEAD_GAP = 0f;
    public static final float LOCATOR_HEAD_RANGE = XP_BAR_WIDTH - LOCATOR_HEAD_SIZE;
    public static final float LOCATOR_ANGLE_RANGE = 60f;
    public static final float LOCATOR_HEAD_SCALE_START = 24f;
    public static final float LOCATOR_HEAD_SCALE_END = 96f;
    public static final float LOCATOR_HEAD_MIN_SCALE = 0.7f;
    public static final float LOCATOR_OUTLINE_DISTANCE = 64f;
    public static final float LOCATOR_OUTLINE_MIN = 0.6f;
    public static final float LOCATOR_OUTLINE_MAX = 1.6f;
    public static final Identifier LOCATOR_ARROW_UP = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_up");
    public static final Identifier LOCATOR_ARROW_DOWN = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_down");
    public static final int LOCATOR_ARROW_W = 6;
    public static final int LOCATOR_ARROW_H = 4;
    public static final int LOCATOR_ARROW_GAP = 1;
    public static final float LOCATOR_LABEL_SCALE = 0.285f;
    public static final float LOCATOR_LABEL_PADDING_X = 3.0f;
    public static final float LOCATOR_LABEL_PADDING_Y = 2.0f;
    public static final float LOCATOR_LABEL_RADIUS = 4.8f;
    public static final float LOCATOR_LABEL_OFFSET = 3.4f;
    public static final float LOCATOR_LABEL_ANIM_SPEED = 14f;
    public static final float LOCATOR_LABEL_ROW_GAP = 0.35f;
    public static final float LOCATOR_LABEL_CHIP_PAD_X = 3.1f;
    public static final float LOCATOR_LABEL_CHIP_PAD_Y = 1.35f;
    public static final float LOCATOR_LABEL_CHIP_RADIUS = 3.6f;
    public static final float LOCATOR_LABEL_ACCENT_W = 1.15f;
    public static final int LOCATOR_LABEL_MAX_ROWS = 4;

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
