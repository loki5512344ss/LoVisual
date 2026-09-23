package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;

/**
 * Физика капель: гравитация, ветер, спавн, столкновения.
 */
final class RainPhysics {
    private RainPhysics() {}

    static float presetDensity(Rain rain) {
        return switch (rain.preset.get()) {
            case DRIZZLE -> 0.45f; case DOWNPOUR -> 1.9f; case STORM -> 2.8f; case RAIN -> 1.0f;
        };
    }

    static float presetWind(Rain rain) {
        return switch (rain.preset.get()) {
            case DRIZZLE -> 0.5f; case DOWNPOUR -> 1.35f; case STORM -> 2.4f; case RAIN -> 1.0f;
        };
    }

    static float presetMass(Rain rain) {
        return switch (rain.preset.get()) {
            case DRIZZLE -> 0.55f; case DOWNPOUR -> 1.25f; case STORM -> 1.5f; case RAIN -> 1.0f;
        };
    }

    static int targetDropCount(Rain rain) {
        float r = rain.radius.get();
        float area = r * r * 0.0155f;
        int count = (int) (area * rain.density.get() * presetDensity(rain) * 26.0f);
        return Mth.clamp(count, 40, Rain.MAX_DROPS);
    }

    static int surfaceY(Rain rain, int x, int z) {
        Minecraft mc = Minecraft.getInstance();
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        Integer cached = rain.groundCache.get(key);
        if (cached != null) return cached;
        int top;
        try { top = mc.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY(); }
        catch (Throwable ignored) { top = mc.level.getMinY(); }
        if (rain.groundCache.size() < 20000) rain.groundCache.put(key, top);
        return top;
    }

    static boolean isBlocking(Rain rain, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        rain.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        BlockState state = mc.level.getBlockState(rain.scratchPos);
        if (!state.getFluidState().isEmpty()) return true;
        if (state.isAir()) return false;
        try { return !state.getCollisionShape(mc.level, rain.scratchPos).isEmpty(); }
        catch (Throwable ignored) { return true; }
    }

    static boolean isWater(Rain rain, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        rain.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        try { return !mc.level.getBlockState(rain.scratchPos).getFluidState().isEmpty(); }
        catch (Throwable ignored) { return false; }
    }

    static boolean skyVisible(Rain rain, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!rain.skyCheck.get() || mc.level == null) return true;
        rain.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        try { return mc.level.canSeeSky(rain.scratchPos); }
        catch (Throwable ignored) { return true; }
    }

    static void spawnDrop(Rain rain, Vec3 center, boolean fromTop) {
        float r = rain.radius.get();
        double angle = rain.random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(rain.random.nextDouble()) * r;
        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        double y = fromTop ? center.y + rain.altitude.get() * rain.random(0.75f, 1.0f)
                : center.y + rain.random(-4.0f, rain.altitude.get());
        if (!skyVisible(rain, x, y, z)) return;
        RainParticles.Drop drop = new RainParticles.Drop();
        drop.x = x; drop.y = y; drop.z = z;
        drop.mass = rain.random(0.6f, 1.4f) * presetMass(rain);
        drop.vy = -rain.random(6.0f, 11.0f) * drop.mass;
        drop.phase = rain.random(0.0f, 62.8f);
        drop.brightness = rain.random(0.55f, 1.0f);
        drop.ground = surfaceY(rain, Mth.floor(x), Mth.floor(z));
        rain.drops.add(drop);
    }

    static void updateDrops(Rain rain, Vec3 center, float dt) {
        int target = targetDropCount(rain);
        rain.drops.removeIf(d -> {
            double dx = d.x - center.x; double dz = d.z - center.z;
            float r = rain.radius.get() + 6.0f;
            return d.dead || dx * dx + dz * dz > r * r || d.y < center.y - rain.altitude.get() - 12.0;
        });
        boolean fill = rain.drops.size() < target / 2;
        int spawnBudget = fill ? Math.min(target - rain.drops.size(), 900) : Math.min(target - rain.drops.size(), 260);
        for (int i = 0; i < spawnBudget; i++) spawnDrop(rain, center, !fill);
        float speedMul = rain.fallSpeed.get();
        float windMul = rain.wind.get() * presetWind(rain);
        for (int i = 0, size = rain.drops.size(); i < size; i++) {
            RainParticles.Drop d = rain.drops.get(i);
            double gust = 0.65 + 0.35 * Math.sin(rain.clock * 0.55 + d.phase * 0.15) + 0.18 * Math.sin(rain.clock * 1.7 + d.phase);
            double windX = windMul * 3.1 * gust;
            double windZ = windMul * 1.7 * Math.sin(rain.clock * 0.31 + d.phase * 0.05) * gust;
            double drag = 1.0 - Math.min(0.92, Math.abs(d.vy) / (Rain.TERMINAL_VELOCITY * d.mass));
            d.vy -= Rain.GRAVITY * d.mass * drag * dt;
            if (d.vy < -Rain.TERMINAL_VELOCITY * d.mass) d.vy = -Rain.TERMINAL_VELOCITY * d.mass;
            d.vx += (windX - d.vx) * Math.min(1.0, dt * 3.2);
            d.vz += (windZ - d.vz) * Math.min(1.0, dt * 3.2);
            double nx = d.x + d.vx * dt * speedMul;
            double ny = d.y + d.vy * dt * speedMul;
            double nz = d.z + d.vz * dt * speedMul;
            boolean hit = false;
            if (ny <= d.ground + 2.5) {
                if (isBlocking(rain, nx, ny, nz)) hit = true;
                else if (ny <= d.ground - 3.0) d.dead = true;
            }
            if (hit) {
                if (rain.splashes.get()) RainParticles.addSplash(rain, nx, ny, nz, d);
                d.dead = true; continue;
            }
            d.x = nx; d.y = ny; d.z = nz;
        }
    }
}
