package dev.loki.lovisual.features.module.modules.visuals.lyrics.mode;

import dev.loki.lovisual.config.values.mode.EnumValue;

/**
 * Selection enums for the KineticLyrics module: animation style, coloring mode,
 * font family and line-splitting strategy. Grouped in one file so the module class stays
 * under the project file-size cap.
 */
public final class LyricModes {

    private LyricModes() { }

    public enum AnimationMode implements EnumValue.IdProvider {
        LYRIC_FLOW("lyric_flow"),
        TYPEWRITER("typewriter"),
        POP_SCALE("pop_scale"),
        KINETIC_SLIDE("kinetic_slide"),
        LETTER_FALL("letter_fall"),
        FADE("fade");

        private final String id;

        AnimationMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum ColorMode implements EnumValue.IdProvider {
        THEME("theme"),
        THEME_GRADIENT("theme_gradient"),
        WHITE("white"),
        STATIC("static");

        private final String id;

        ColorMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum FontMode implements EnumValue.IdProvider {
        ONEST_BOLD("onest_bold", "OnestBold"),
        ONEST_MEDIUM("onest_medium", "OnestMedium"),
        ONEST("onest", "Onest"),
        INTER_MEDIUM("inter_medium", "InterMedium");

        private final String id;
        private final String family;

        FontMode(String id, String family) {
            this.id = id;
            this.family = family;
        }

        @Override
        public String id() {
            return id;
        }

        public String family() {
            return family;
        }
    }

    public enum SplitMode implements EnumValue.IdProvider {
        SMART_SPLIT("smart_split"),
        FULL_LINE("full_line");

        private final String id;

        SplitMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
