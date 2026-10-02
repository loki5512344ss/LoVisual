package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * История одной ленты: точки с метками времени. Сдвиг меньше {@link #MIN_STEP} точкой не становится,
 * точки старше срока жизни отбрасываются. Общая основа для ProjectileTrails и ElytraTrails.
 */
public final class TrackBuffer {

    /** Минимальный сдвиг между соседними точками (блоки), чтобы лента не копила шум. */
    public static final double MIN_STEP = 0.04;

    private final ArrayDeque<RibbonPoint> points = new ArrayDeque<>();
    private long lastSeen = Long.MIN_VALUE;

    /**
     * Фиксирует позицию, затем убирает устаревшие точки.
     *
     * @return true, если добавлена новая точка
     */
    public boolean add(double x, double y, double z, long now, long lifeMs) {
        lastSeen = now;
        RibbonPoint last = points.peekLast();
        boolean added = false;
        if (last == null || (now != last.ms() && distance(last, x, y, z) >= MIN_STEP)) {
            points.addLast(new RibbonPoint(x, y, z, now));
            added = true;
        }
        trim(now, lifeMs);
        return added;
    }

    /** Больше точек лента не держит: самые старые вытесняются. */
    public static final int MAX_POINTS = 256;

    /**
     * Как {@link #add}, но для непрерывной ленты (концы крыльев): сдвиг меньше {@code minStep} не записывается,
     * скачок дальше {@code maxStep} или пауза дольше {@code gapMs} (телепорт, перерыв в полёте) начинают ленту заново,
     * у каждой точки сохраняется скорость источника.
     */
    public boolean addContinuous(double x, double y, double z, long now, long lifeMs, float speed,
                                 double minStep, double maxStep, long gapMs) {
        RibbonPoint last = points.peekLast();
        if (last != null && (now - lastSeen > gapMs || distance(last, x, y, z) > maxStep)) {
            points.clear();
            last = null;
        }
        lastSeen = now;
        boolean added = false;
        if (last == null || distance(last, x, y, z) >= minStep) {
            points.addLast(new RibbonPoint(x, y, z, now, speed));
            if (points.size() > MAX_POINTS) points.pollFirst();
            added = true;
        }
        trim(now, lifeMs);
        return added;
    }

    public void trim(long now, long lifeMs) {
        while (!points.isEmpty() && now - points.peekFirst().ms() > lifeMs) points.pollFirst();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public int size() {
        return points.size();
    }

    /** Время последнего вызова {@link #add}, даже если точка не добавилась. */
    public long lastSeen() {
        return lastSeen;
    }

    /** Копия точек от хвоста к голове. */
    public List<RibbonPoint> points() {
        return new ArrayList<>(points);
    }

    private static double distance(RibbonPoint a, double x, double y, double z) {
        double dx = a.x() - x, dy = a.y() - y, dz = a.z() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
