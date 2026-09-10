package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcLibClient;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLineSplitter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.LyricParticle3D;

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
    private final EnumValue<ColorMode> colorMode =
            enumMode("color_mode", ColorMode.THEME, ColorMode.values());
    private final RGBAColorValue staticColor =
            visibleWhen(color("static_color", "#FFFFFFFF"), () -> colorMode.get() == ColorMode.STATIC);
    private final EnumValue<FontMode> fontMode =
            enumMode("font", FontMode.ONEST_BOLD, FontMode.values());
    private final EnumValue<SplitMode> splitMode =
            enumMode("split_mode", SplitMode.SMART_SPLIT, SplitMode.values());
    private final NumberValue<Integer> maxLines =
            num("max_lines", 3, 1, 6);
    private final NumberValue<Float> minDistance =
            num("min_distance", 2.0f, 1.2f, 4.0f);
    private final NumberValue<Float> maxDistance =
            num("max_distance", 4.5f, 2.0f, 7.0f);
    private final NumberValue<Float> textScale =
            num("text_scale", 2.5f, 0.5f, 5.0f);
    private final NumberValue<Float> arcSpread =
            num("arc_spread", 70.0f, 20.0f, 85.0f);
    private final NumberValue<Float> floatHeight =
            num("float_height", 0.5f, 0.1f, 2.0f);
    private final NumberValue<Integer> timeOffsetMs =
            num("time_offset_ms", -1000, -5000, 5000);
    private final BooleanValue debugMode =
            bool("debug", false);

    private final List<LyricLine> rawLyrics = new ArrayList<>();
    private final List<LyricLine> lyricsQueue = new ArrayList<>();
    private final List<LyricParticle3D> activeParticles = new CopyOnWriteArrayList<>();
    private int currentLyricIndex = 0;

    private String lastTrackKey = "";
    private String currentTrackTitle = "";
    private String currentTrackArtist = "";
    private long lastAudioTimeMs = -1L;
    private boolean isPlaying = false;

    @Override
    public void onEnable() {
        resetPlaybackState();
    }

    @Override
    public void onDisable() {
        resetPlaybackState();
    }

    private void resetPlaybackState() {
        activeParticles.clear();
        rawLyrics.clear();
        lyricsQueue.clear();
        lastTrackKey = "";
        currentTrackTitle = "";
        currentTrackArtist = "";
        currentLyricIndex = 0;
        lastAudioTimeMs = -1L;
        isPlaying = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null) return;
        syncPlaybackTime();
    }

    private void syncPlaybackTime() {
        MediaSessionService.Snapshot snapshot = MediaSessionService.get().snapshot();
        if (!snapshot.hasSession() || snapshot.title().isBlank()) {
            if (isPlaying) {
                isPlaying = false;
                activeParticles.clear();
            }
            return;
        }

        String title = snapshot.title();
        String artist = snapshot.artist();
        String key = (title + " - " + artist).toLowerCase();

        if (!key.equalsIgnoreCase(lastTrackKey)) {
            loadTrack(title, artist, key);
            return;
        }

        if (!isPlaying && !lyricsQueue.isEmpty()) {
            isPlaying = true;
        }
        if (lyricsQueue.isEmpty()) return;

        // Позиция плеера (MPRIS/SMTC) — это внутренний таймер декодера: он
        // опережает слышимый вывод на латентность аудио-тракта (0.3-1с+, до
        // нескольких секунд через Bluetooth), а LRC-таймкоды привязаны к
        // слышимому звуку. Дефолтный сдвиг назад компенсирует это (решение
        // совпадает с lrcsnc: у автора ~0.75с задержки для mpd).
        long audioTimeMs = Math.max(0L, snapshot.predictedPositionSeconds() * 1000L + timeOffsetMs.get());

        if (lastAudioTimeMs >= 0 && Math.abs(audioTimeMs - lastAudioTimeMs) > 1500L) {
            resyncQueue(audioTimeMs);
        }
        lastAudioTimeMs = audioTimeMs;

        advanceQueue(audioTimeMs);
    }

    private void loadTrack(String title, String artist, String key) {
        this.currentTrackTitle = title;
        this.currentTrackArtist = artist;
        this.lastTrackKey = key;

        activeParticles.clear();
        rawLyrics.clear();
        lyricsQueue.clear();
        currentLyricIndex = 0;
        lastAudioTimeMs = -1L;
        isPlaying = false;

        LrcLibClient.fetchLyricsAsync(title, artist, debugMode.get()).thenAccept(lines -> {
            if (!this.lastTrackKey.equalsIgnoreCase(key)) return;

            activeParticles.clear();
            rawLyrics.clear();
            lyricsQueue.clear();
            rawLyrics.addAll(lines);
            rebuildLyricsQueue();
            currentLyricIndex = 0;
            isPlaying = !lyricsQueue.isEmpty();
        });
    }

    private void rebuildLyricsQueue() {
        lyricsQueue.clear();
        if (splitMode.get() == SplitMode.SMART_SPLIT) {
            lyricsQueue.addAll(LyricLineSplitter.splitLongLines(rawLyrics));
        } else {
            lyricsQueue.addAll(rawLyrics);
        }
    }

    private void resyncQueue(long audioTimeMs) {
        activeParticles.clear();
        currentLyricIndex = 0;
        long now = System.currentTimeMillis();

        for (int i = 0; i < lyricsQueue.size(); i++) {
            LyricLine line = lyricsQueue.get(i);
            long nextStartMs = (i + 1 < lyricsQueue.size()) ? lyricsQueue.get(i + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1200L, Math.min(5000L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    spawnLyricParticle(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex = i + 1;
            } else {
                break;
            }
        }
    }

    private void advanceQueue(long audioTimeMs) {
        Minecraft mc = Minecraft.getInstance();
        if (!isPlaying || lyricsQueue.isEmpty() || mc.player == null) return;

        long now = System.currentTimeMillis();

        while (currentLyricIndex < lyricsQueue.size()) {
            LyricLine line = lyricsQueue.get(currentLyricIndex);
            long nextStartMs = (currentLyricIndex + 1 < lyricsQueue.size()) ? lyricsQueue.get(currentLyricIndex + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1400L, Math.min(5500L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    spawnLyricParticle(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex++;
            } else {
                break;
            }
        }

        activeParticles.removeIf(particle -> particle.isDead(now));
    }

    private Vec3 findNonOverlappingSpawnPos(Vec3 headPos, float cameraYaw) {
        double spread = arcSpread.get();
        double minD = minDistance.get();
        double maxD = Math.max(minD + 0.5, maxDistance.get());

        Vec3 bestPos = null;
        double maxMinDistance = -1.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            double randomAngleOffset = ThreadLocalRandom.current().nextDouble(-spread, spread);
            double targetYawDeg = cameraYaw + randomAngleOffset;
            double distance = ThreadLocalRandom.current().nextDouble(minD, maxD);
            double yOffset = ThreadLocalRandom.current().nextDouble(-0.25, 0.55);

            double yawRad = Math.toRadians(targetYawDeg);
            double dirX = -Math.sin(yawRad);
            double dirZ = Math.cos(yawRad);

            Vec3 candidate = new Vec3(
                    headPos.x + (dirX * distance),
                    headPos.y + yOffset,
                    headPos.z + (dirZ * distance)
            );

            if (activeParticles.isEmpty()) {
                return candidate;
            }

            double minDistanceToOthers = Double.MAX_VALUE;
            for (LyricParticle3D active : activeParticles) {
                double d = candidate.distanceTo(active.getBasePosition());
                if (d < minDistanceToOthers) {
                    minDistanceToOthers = d;
                }
            }

            if (minDistanceToOthers >= 1.35) {
                return candidate;
            }

            if (minDistanceToOthers > maxMinDistance) {
                maxMinDistance = minDistanceToOthers;
                bestPos = candidate;
            }
        }

        return bestPos;
    }

    private void spawnLyricParticle(String text, long spawnTimeMs, long durationMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || text == null || text.isBlank()) return;
        if (mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 headPos = mc.player.getEyePosition();
        float cameraYaw = camera.yRot();

        int maxAllowed = maxLines.get();
        while (activeParticles.size() >= maxAllowed) {
            activeParticles.remove(0);
        }

        Vec3 spawnPos = findNonOverlappingSpawnPos(headPos, cameraYaw);
        if (spawnPos == null) return;

        activeParticles.add(new LyricParticle3D(text, spawnPos, spawnTimeMs, durationMs));
    }

    private TextRenderer getSelectedFont() {
        return Fonts.renderer(fontMode.get().family, FontInfo.Type.Regular, TextRenderer.get());
    }

    private int getActiveColorRgb() {
        return switch (colorMode.get()) {
            case THEME -> Theme.theme().accent();
            case THEME_GRADIENT -> {
                float ratio = (float) (Math.sin(System.currentTimeMillis() / 450.0) * 0.5 + 0.5);
                int c1 = Theme.theme().accent();
                int c2 = Theme.theme().accentSoft();
                yield mixArgb(c1, c2, ratio);
            }
            case WHITE -> 0xFFFFFF;
            case STATIC -> staticColor.getArgb();
        };
    }

    private static int mixArgb(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = mixChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = mixChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = mixChannel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int mixChannel(int from, int to, float t) {
        return from + Math.round((to - from) * t);
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        if (activeParticles.isEmpty()) return;

        TextRenderer font = getSelectedFont();
        String animMode = animation.get().name();
        int colorRgb = getActiveColorRgb();
        long now = System.currentTimeMillis();

        for (LyricParticle3D particle : activeParticles) {
            particle.render(renderer, font, colorRgb, animMode, now, textScale.get());
        }
    }

    @Override
    public void onRenderHudEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || !debugMode.get() || mc.player == null) return;

        TextRenderer tr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, textRenderer);
        String trackLine = "Track: " + (currentTrackTitle.isEmpty() ? "None" : currentTrackTitle + " - " + currentTrackArtist);
        long displayTimeMs = Math.max(0L, lastAudioTimeMs);
        String timerLine = String.format("Audio time: %02d:%02d.%03d (%d ms)",
                displayTimeMs / 60_000,
                (displayTimeMs % 60_000) / 1000,
                displayTimeMs % 1000,
                displayTimeMs
        );
        String particlesLine = "Active particles: " + activeParticles.size();
        String queueLine = "Queue: " + currentLyricIndex + " / " + lyricsQueue.size() + " lines";

        tr.begin(0.85);
        tr.render(trackLine, 10.0, 70.0, new RenderColor(0xFFDCDCDC), false);
        tr.render(timerLine, 10.0, 82.0, new RenderColor(0xFFFFFFFF), false);
        tr.render(particlesLine, 10.0, 94.0, new RenderColor(0xFF78FF78), false);
        tr.render(queueLine, 10.0, 106.0, new RenderColor(0xFFB4B4FF), false);
        tr.end();
    }

    public enum AnimationMode implements EnumValue.IdProvider {
        LYRIC_FLOW("lyric_flow"),
        TYPEWRITER("typewriter"),
        POP_SCALE("pop_scale"),
        KINETIC_SLIDE("kinetic_slide"),
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
