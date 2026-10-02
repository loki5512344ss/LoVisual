package dev.loki.lovisual.features.module.modules.visuals.effects.projectiletrails;

import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.TrackBuffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntPredicate;

/** История позиций летящих снарядов по id сущности; пропавшие из мира снаряды дотлевают и удаляются. */
final class TrailStore {

    private final Map<Integer, TrackBuffer> tracks = new HashMap<>();

    Map<Integer, TrackBuffer> tracks() {
        return tracks;
    }

    void clear() {
        tracks.clear();
    }

    /** Ключ вида снаряда для настроек: arrow, trident, pearl, snowball, egg, potion, fireball, wind_charge, firework. */
    static String kindOf(Entity entity) {
        if (!(entity instanceof Projectile)) return null;
        return kindOfPath(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
    }

    /** Вид снаряда по пути id типа сущности (чистая логика); null для неподдерживаемых. */
    static String kindOfPath(String path) {
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

    /** Добавляет текущую (интерполированную) позицию снаряда и отбрасывает старые точки. */
    void sample(Entity entity, float partialTicks, long now, long lifeMs) {
        Vec3 p = entity.getPosition(partialTicks);
        sample(entity.getId(), p.x, p.y, p.z, now, lifeMs);
    }

    void sample(int id, double x, double y, double z, long now, long lifeMs) {
        tracks.computeIfAbsent(id, key -> new TrackBuffer()).add(x, y, z, now, lifeMs);
    }

    /** Чистит следы: пропавшие снаряды дотлевают, потом след удаляется. */
    void prune(long now, long lifeMs, IntPredicate alive) {
        for (Iterator<Map.Entry<Integer, TrackBuffer>> it = tracks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, TrackBuffer> e = it.next();
            if (!alive.test(e.getKey())) e.getValue().trim(now, lifeMs);
            if (e.getValue().isEmpty()) it.remove();
        }
    }
}
