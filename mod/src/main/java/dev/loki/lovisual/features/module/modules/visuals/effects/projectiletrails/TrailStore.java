package dev.loki.lovisual.features.module.modules.visuals.effects.projectiletrails;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;

/** История позиций летящих снарядов: точки с метками времени, устаревшие и пропавшие следы вычищаются. */
final class TrailStore {

    /** Минимальный сдвиг между соседними точками (блоки), чтобы лента не копила шум. */
    private static final double MIN_STEP = 0.04;

    record Point(double x, double y, double z, long ms) {
    }

    static final class Track {
        final ArrayDeque<Point> points = new ArrayDeque<>();
        long lastSeen;
    }

    private final Map<Integer, Track> tracks = new HashMap<>();

    Map<Integer, Track> tracks() {
        return tracks;
    }

    void clear() {
        tracks.clear();
    }

    /** Ключ вида снаряда для настроек: arrow, trident, pearl, snowball, egg, potion, fireball, wind_charge, firework. */
    static String kindOf(Entity entity) {
        if (!(entity instanceof Projectile)) return null;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        if (path.contains("arrow")) return "arrow";
        if (path.contains("trident")) return "trident";
        if (path.contains("pearl")) return "pearl";
        if (path.contains("snowball")) return "snowball";
        if (path.contains("egg")) return "egg";
        if (path.contains("potion")) return "potion";
        if (path.contains("wind_charge")) return "wind_charge";
        if (path.contains("fireball") || path.contains("fire_charge")) return "fireball";
        if (path.contains("firework")) return "firework";
        return null;
    }

    /** Добавляет текущую позицию снаряда (интерполированную) и отбрасывает старые точки. */
    void sample(Entity entity, float partialTicks, long now, long lifeMs) {
        Track track = tracks.computeIfAbsent(entity.getId(), id -> new Track());
        track.lastSeen = now;
        Vec3 p = entity.getPosition(partialTicks);
        Point last = track.points.peekLast();
        if (last == null || last.ms() != now && dist(last, p) >= MIN_STEP) {
            track.points.addLast(new Point(p.x, p.y, p.z, now));
        }
        trim(track, now, lifeMs);
    }

    /** Чистит следы: пропавшие из мира снаряды дотлевают, потом удаляются. */
    void prune(long now, long lifeMs, Predicate<Integer> alive) {
        for (Iterator<Map.Entry<Integer, Track>> it = tracks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Track> e = it.next();
            if (!alive.test(e.getKey())) trim(e.getValue(), now, lifeMs);
            if (e.getValue().points.isEmpty() || now - e.getValue().lastSeen > lifeMs * 2) it.remove();
        }
    }

    private static void trim(Track track, long now, long lifeMs) {
        while (!track.points.isEmpty() && now - track.points.peekFirst().ms() > lifeMs) track.points.pollFirst();
    }

    private static double dist(Point a, Vec3 b) {
        double dx = a.x() - b.x, dy = a.y() - b.y, dz = a.z() - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
