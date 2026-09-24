package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Owns the set of live {@link LyricParticle3D}s for {@link KineticLyrics} and everything about
 * placing and coloring them: FOV-clamped spawn spread, non-overlapping spawn position search,
 * theme / gradient / static color resolution and the selected font renderer. Reads the module's
 * settings through its owner; lifecycle (reset) and the queue that decides *what* to spawn live
 * in {@link LyricPlayback}. Extracted so the module class stays under the file-size cap.
 */
final class LyricStage {

    private final KineticLyrics owner;
    private final List<LyricParticle3D> activeParticles = new CopyOnWriteArrayList<>();

    LyricStage(KineticLyrics owner) {
        this.owner = owner;
    }

    List<LyricParticle3D> particles() {
        return activeParticles;
    }

    boolean isEmpty() {
        return activeParticles.isEmpty();
    }

    int size() {
        return activeParticles.size();
    }

    void clear() {
        activeParticles.clear();
    }

    /** Spawn a lyric particle for {@code text}, trimming the oldest when over the line cap. */
    void spawn(String text, long spawnTimeMs, long durationMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || text == null || text.isBlank()) return;
        if (mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 headPos = mc.player.getEyePosition();
        float cameraYaw = camera.yRot();

        int maxAllowed = owner.maxLines.get();
        while (activeParticles.size() >= maxAllowed) {
            activeParticles.remove(0);
        }

        Vec3 spawnPos = findSpawnPos(headPos, cameraYaw);
        if (spawnPos == null) return;

        activeParticles.add(new LyricParticle3D(text, spawnPos, spawnTimeMs, durationMs));
    }

    void pruneDead(long now) {
        activeParticles.removeIf(particle -> particle.isDead(now));
    }

    /**
     * arcSpread is a user-facing "how wide" preference, but it must never exceed what the
     * player's actual camera FOV can show, or particles spawn off-screen. Clamp it to the
     * real horizontal FOV (derived from vertical FOV + window aspect), with a safety margin
     * so text doesn't pop in right at the frustum edge.
     */
    private double resolveSpawnSpreadDeg() {
        double configured = owner.arcSpread.get();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null || mc.getWindow() == null) {
            return configured;
        }

        float verticalFovDeg = mc.gameRenderer.mainCamera().getFov();
        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double halfVerticalRad = Math.toRadians(verticalFovDeg) / 2.0;
        double halfHorizontalDeg = Math.toDegrees(Math.atan(Math.tan(halfVerticalRad) * aspect));

        return Math.min(configured, halfHorizontalDeg * 0.85);
    }

    private Vec3 findSpawnPos(Vec3 headPos, float cameraYaw) {
        double spread = resolveSpawnSpreadDeg();
        double minD = owner.minDistance.get();
        double maxD = Math.max(minD + 0.5, owner.maxDistance.get());

        Vec3 bestPos = null;
        double maxMinDistance = -1.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 candidate = randomCandidate(headPos, cameraYaw, spread, minD, maxD);
            if (activeParticles.isEmpty()) {
                return candidate;
            }
            double minDistanceToOthers = minDistanceToOthers(candidate);
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

    private Vec3 randomCandidate(Vec3 headPos, float cameraYaw, double spread, double minD, double maxD) {
        double randomAngleOffset = ThreadLocalRandom.current().nextDouble(-spread, spread);
        double targetYawDeg = cameraYaw + randomAngleOffset;
        double distance = ThreadLocalRandom.current().nextDouble(minD, maxD);
        double yOffset = ThreadLocalRandom.current().nextDouble(-0.25, 0.55);

        double yawRad = Math.toRadians(targetYawDeg);
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);

        return new Vec3(
                headPos.x + (dirX * distance),
                headPos.y + yOffset,
                headPos.z + (dirZ * distance)
        );
    }

    private double minDistanceToOthers(Vec3 candidate) {
        double minDistanceToOthers = Double.MAX_VALUE;
        for (LyricParticle3D active : activeParticles) {
            double d = candidate.distanceTo(active.getBasePosition());
            if (d < minDistanceToOthers) {
                minDistanceToOthers = d;
            }
        }
        return minDistanceToOthers;
    }

    TextRenderer selectedFont() {
        return Fonts.renderer(owner.fontMode.get().family(), FontInfo.Type.Regular, TextRenderer.get());
    }

    int activeColorRgb() {
        return switch (owner.colorMode.get()) {
            case THEME -> Theme.theme().accent();
            case THEME_GRADIENT -> {
                float ratio = (float) (Math.sin(System.currentTimeMillis() / 450.0) * 0.5 + 0.5);
                int c1 = Theme.theme().accent();
                int c2 = Theme.theme().accentSoft();
                yield mixArgb(c1, c2, ratio);
            }
            case WHITE -> 0xFFFFFF;
            case STATIC -> owner.staticColor.getArgb();
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
}
