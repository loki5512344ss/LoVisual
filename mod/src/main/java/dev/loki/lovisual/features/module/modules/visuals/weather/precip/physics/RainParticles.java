package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import dev.loki.lovisual.features.module.modules.visuals.weather.precip.Rain;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Партиклы: Splash/Droplet/Mist + логика обновления.
 */
final class RainParticles {
    private RainParticles() {}

    static void addSplash(Rain rain, double x, double y, double z, Drop d) {
        if (rain.splashList.size() > 700) return;
        double impactY = Math.floor(y) + 1.0005;
        Splash splash = new Splash();
        splash.x = x; splash.y = impactY; splash.z = z;
        splash.life = 0.0f;
        splash.maxLife = rain.random(0.30f, 0.52f);
        splash.scale = (float) (0.32 + 0.22 * d.mass);
        splash.water = RainPhysics.isWater(rain, x, y - 0.15, z);
        rain.splashList.add(splash);
        if (!rain.droplets.get() || rain.dropletList.size() > 1400) return;
        int shards = splash.water ? 4 : 3;
        for (int i = 0; i < shards; i++) {
            double a = rain.random.nextDouble() * Math.PI * 2.0;
            double sp = rain.random(1.1f, 2.6f) * d.mass;
            Droplet dr = new Droplet();
            dr.x = x; dr.y = impactY + 0.02; dr.z = z;
            dr.vx = Math.cos(a) * sp * 0.45 + d.vx * 0.08;
            dr.vz = Math.sin(a) * sp * 0.45 + d.vz * 0.08;
            dr.vy = rain.random(1.9f, 4.1f);
            dr.maxLife = rain.random(0.28f, 0.5f);
            rain.dropletList.add(dr);
        }
    }

    static void updateDroplets(Rain rain, float dt) {
        for (int i = rain.dropletList.size() - 1; i >= 0; i--) {
            Droplet dr = rain.dropletList.get(i);
            dr.life += dt;
            if (dr.life >= dr.maxLife) { rain.dropletList.remove(i); continue; }
            dr.vy -= Rain.GRAVITY * 0.55 * dt;
            dr.x += dr.vx * dt; dr.y += dr.vy * dt; dr.z += dr.vz * dt;
        }
    }

    static void updateSplashes(Rain rain, float dt) {
        for (int i = rain.splashList.size() - 1; i >= 0; i--) {
            Splash s = rain.splashList.get(i);
            s.life += dt;
            if (s.life >= s.maxLife) rain.splashList.remove(i);
        }
    }

    static void updateMist(Rain rain, Vec3 center, float dt) {
        if (!rain.groundMist.get()) { rain.mistList.clear(); return; }
        int target = (int) Mth.clamp(22 * rain.density.get() * RainPhysics.presetDensity(rain), 6, Rain.MAX_MIST);
        rain.mistList.removeIf(m -> {
            m.life += dt;
            double dx = m.x - center.x; double dz = m.z - center.z;
            float r = rain.radius.get();
            m.x += m.vx * dt; m.z += m.vz * dt;
            return m.life > m.maxLife || dx * dx + dz * dz > r * r;
        });
        while (rain.mistList.size() < target) {
            double angle = rain.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rain.random.nextDouble()) * rain.radius.get() * 0.85;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            int ground = RainPhysics.surfaceY(rain, Mth.floor(x), Mth.floor(z));
            Mist m = new Mist();
            m.x = x; m.z = z; m.y = ground + rain.random(0.05f, 0.9f);
            m.size = rain.random(2.2f, 5.6f);
            m.maxLife = rain.random(2.4f, 5.5f);
            m.life = rain.random(0.0f, 1.0f);
            m.vx = rain.random(-0.35f, 0.35f) * rain.wind.get();
            m.vz = rain.random(-0.35f, 0.35f) * rain.wind.get();
            rain.mistList.add(m);
        }
    }

    static void updateLightning(Rain rain, float dt) {
        if (!rain.lightning.get()) { rain.flash = 0.0f; return; }
        rain.flash = Math.max(0.0f, rain.flash - dt * 3.4f);
        rain.nextFlashIn -= dt;
        if (rain.nextFlashIn <= 0.0f) {
            rain.flash = rain.random(0.55f, 1.0f);
            rain.nextFlashIn = rain.random(6.0f, 16.0f);
        }
    }

    static final class Drop {
        double x, y, z; double vx, vy, vz;
        double mass = 1.0; double ground;
        float phase; float brightness = 1.0f; boolean dead;
    }

    static final class Droplet {
        double x, y, z; double vx, vy, vz;
        float life; float maxLife = 0.4f;
    }

    static final class Splash {
        double x, y, z; float life; float maxLife = 0.4f;
        float scale = 0.4f; boolean water;
    }

    static final class Mist {
        double x, y, z; double vx, vz;
        float size = 3.0f; float life; float maxLife = 4.0f;
    }
}
