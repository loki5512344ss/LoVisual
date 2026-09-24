package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class KillOrthodoxHelper {
    private final List<OrthodoxMark> marks = new ArrayList<>();

    void add(Vec3 pos, long now) {
        marks.add(new OrthodoxMark(pos, now));
    }

    void tick(long now) {
        marks.removeIf(m -> now - m.createdAtMs > 3000);
    }

    void render(Renderer3D renderer, int colorArgb, int ySpeed) {
        int r = (colorArgb >> 16) & 255;
        int g = (colorArgb >> 8) & 255;
        int b = colorArgb & 255;
        int a = (colorArgb >> 24) & 255;
        double offset = ySpeed / 100.0;
        for (OrthodoxMark mark : marks) {
            Vec3 pos = mark.pos;
            renderer.line(pos.x, pos.y + offset, pos.z,
                    pos.x, pos.y + 3.0 + offset, pos.z, r, g, b, a);
            renderer.line(pos.x + 1.0, pos.y + 2.3 + offset, pos.z,
                    pos.x - 1.0, pos.y + 2.3 + offset, pos.z, r, g, b, a);
            renderer.line(pos.x + 0.5, pos.y + 1.2 + offset, pos.z,
                    pos.x - 0.5, pos.y + 0.8 + offset, pos.z, r, g, b, a);
        }
    }

    void clear() {
        marks.clear();
    }

    boolean isEmpty() {
        return marks.isEmpty();
    }

    private record OrthodoxMark(Vec3 pos, long createdAtMs) {}
}
