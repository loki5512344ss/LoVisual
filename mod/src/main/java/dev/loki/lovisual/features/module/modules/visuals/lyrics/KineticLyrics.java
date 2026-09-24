package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.mode.LyricModes.AnimationMode;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.mode.LyricModes.ColorMode;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.mode.LyricModes.FontMode;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.mode.LyricModes.SplitMode;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * KineticLyrics: renders the current song's lyrics as animated 3D particles floating in the
 * world around the player. The module itself only declares settings and forwards lifecycle /
 * render callbacks; playback tracking lives in {@link LyricPlayback}, particle placement and
 * styling in {@link LyricStage}, and the selectable modes in {@code LyricModes}.
 */
@ModuleInfo(
        id = "kineticlyrics",
        displayName = "KineticLyrics",
        aliases = {"lyrics"},
        category = ModuleCategory.VISUALS,
        description = "Рендерит текст текущей песни в 3D-пространстве вокруг игрока"
)
public final class KineticLyrics extends Module {

    private final EnumValue<AnimationMode> animation =
            enumMode("animation", AnimationMode.LYRIC_FLOW, AnimationMode.values());
    final EnumValue<ColorMode> colorMode =
            enumMode("color_mode", ColorMode.THEME, ColorMode.values());
    final RGBAColorValue staticColor =
            visibleWhen(color("static_color", "#FFFFFFFF"), () -> colorMode.get() == ColorMode.STATIC);
    final EnumValue<FontMode> fontMode =
            enumMode("font", FontMode.ONEST_BOLD, FontMode.values());
    final EnumValue<SplitMode> splitMode =
            enumMode("split_mode", SplitMode.SMART_SPLIT, SplitMode.values());
    final NumberValue<Integer> maxLines =
            num("max_lines", 3, 1, 6);
    final NumberValue<Float> minDistance =
            num("min_distance", 2.0f, 1.2f, 4.0f);
    final NumberValue<Float> maxDistance =
            num("max_distance", 4.5f, 2.0f, 7.0f);
    private final NumberValue<Float> textScale =
            num("text_scale", 2.5f, 0.5f, 5.0f);
    final NumberValue<Float> arcSpread =
            num("arc_spread", 70.0f, 20.0f, 85.0f);
    final NumberValue<Integer> timeOffsetMs =
            num("time_offset_ms", -1000, -5000, 5000);
    final BooleanValue debugMode =
            bool("debug", false);

    private final LyricStage stage = new LyricStage(this);
    private final LyricPlayback playback = new LyricPlayback(this, stage);

    @Override
    public void onEnable() {
        playback.reset();
    }

    @Override
    public void onDisable() {
        playback.reset();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null) return;
        playback.sync();
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        if (stage.isEmpty()) return;

        TextRenderer font = stage.selectedFont();
        String animMode = animation.get().name();
        int colorRgb = stage.activeColorRgb();
        long now = System.currentTimeMillis();

        for (LyricParticle3D particle : stage.particles()) {
            particle.render(renderer, font, colorRgb, animMode, now, textScale.get());
        }
    }

    @Override
    public void onRenderHudEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || !debugMode.get() || mc.player == null) return;

        TextRenderer tr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, textRenderer);
        String title = playback.trackTitle();
        String trackLine = "Track: " + (title.isEmpty() ? "None" : title + " - " + playback.trackArtist());
        long displayTimeMs = Math.max(0L, playback.lastAudioTimeMs());
        String timerLine = String.format("Audio time: %02d:%02d.%03d (%d ms)",
                displayTimeMs / 60_000,
                (displayTimeMs % 60_000) / 1000,
                displayTimeMs % 1000,
                displayTimeMs
        );
        String particlesLine = "Active particles: " + stage.size();
        String queueLine = "Queue: " + playback.currentLyricIndex() + " / " + playback.queueSize() + " lines";

        tr.begin(0.85);
        tr.render(trackLine, 10.0, 70.0, new RenderColor(0xFFDCDCDC), false);
        tr.render(timerLine, 10.0, 82.0, new RenderColor(0xFFFFFFFF), false);
        tr.render(particlesLine, 10.0, 94.0, new RenderColor(0xFF78FF78), false);
        tr.render(queueLine, 10.0, 106.0, new RenderColor(0xFFB4B4FF), false);
        tr.end();
    }
}
