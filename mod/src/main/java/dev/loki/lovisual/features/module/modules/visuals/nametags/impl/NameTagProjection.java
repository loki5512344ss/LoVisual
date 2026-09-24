package dev.loki.lovisual.features.module.modules.visuals.nametags;

import dev.loki.lovisual.mixininterface.entity.IEntity;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Pure world-to-screen projection math shared by nametag collection: entity position
 * interpolation and bounding-box projection. Lives in the {@code impl/} subfolder but
 * the {@code nametags} package so it can reuse {@link NameTagModel.ScreenRect}.
 */
final class NameTagProjection {

    private NameTagProjection() {
    }

    /** Screen-space placement anchor for a nametag, or {@code null} if off-screen. */
    record Anchor(int centerX, int anchorY, NameTagModel.ScreenRect rect) {
    }

    static Vec3 lerpedPos(Entity e, float tickDelta) {
        try {
            return e.getPosition(tickDelta);
        } catch (NoSuchMethodError ex) {
            if (e instanceof IEntity access) {
                return access.get$InstantRenderPos().lerp(e.position(), tickDelta);
            }
            return e.position();
        }
    }

    static Anchor resolveAnchor(Player player, Vec3 pos, float tickDelta) {
        AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
        NameTagModel.ScreenRect rect = projectBoxScreen(box, tickDelta);
        if (rect != null) {
            int centerX = (int) Math.round((rect.minX() + rect.maxX()) * 0.5);
            int anchorY = (int) Math.round(rect.minY());
            return new Anchor(centerX, anchorY, rect);
        }
        Vec3 topCenter = new Vec3(box.getCenter().x, box.maxY + 0.5, box.getCenter().z);
        Vec3 screen = ScreenProjection.worldToScreen(topCenter, tickDelta);
        if (screen == null) return null;
        return new Anchor((int) screen.x, (int) screen.y, null);
    }

    static NameTagModel.ScreenRect projectBoxScreen(AABB box, float tickDelta) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean any = false;
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vec3 screen = ScreenProjection.worldToScreen(new Vec3(x, y, z), tickDelta);
                    if (screen == null) continue;
                    any = true;
                    minX = Math.min(minX, screen.x);
                    minY = Math.min(minY, screen.y);
                    maxX = Math.max(maxX, screen.x);
                    maxY = Math.max(maxY, screen.y);
                }
            }
        }
        if (!any) return null;
        return new NameTagModel.ScreenRect(minX, minY, maxX, maxY);
    }
}
