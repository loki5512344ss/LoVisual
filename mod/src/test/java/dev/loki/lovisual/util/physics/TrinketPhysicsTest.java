package dev.loki.lovisual.util.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrinketPhysicsTest {

    @Test
    void restIsZeroAfterReset() {
        TrinketPhysics p = new TrinketPhysics();
        p.tick(0.05, 20.0, 1.0, 0.98, 0.7, 0.9, 1.0);
        p.reset();
        assertEquals(0.0, p.x());
        assertEquals(0.0, p.y());
        assertEquals(0.0, p.z());
        assertEquals(0.0, p.speed());
    }

    @Test
    void restingBallBouncesImmediatelyUnderGravity() {
        // y=0 — это уровень земли (у ног владельца), поэтому шарик из состояния покоя
        // на первом же тике проваливается под гравитацией и тут же отскакивает обратно —
        // это и есть наблюдаемый эффект гравитации в модели "ноль высоты = земля".
        TrinketPhysics p = new TrinketPhysics();
        assertTrue(p.tick(0.05, 20.0, 1.0, 1.0, 0.7, 0.9, 5.0), "гравитация должна вжать шарик в землю на первом тике");
        assertEquals(0.0, p.y(), 1.0e-9);
        assertTrue(p.speed() > 0.0, "после отскока есть исходящая от земли скорость");
    }

    @Test
    void bouncesWhenHittingGroundFromAbove() {
        TrinketPhysics p = new TrinketPhysics();
        boolean bouncedAtLeastOnce = false;
        for (int i = 0; i < 200; i++) {
            if (p.tick(0.05, 20.0, 1.0, 0.999, 0.7, 0.9, 5.0)) {
                bouncedAtLeastOnce = true;
                break;
            }
        }
        assertTrue(bouncedAtLeastOnce, "падение с гравитацией должно рано или поздно удариться о землю (y=0)");
        assertEquals(0.0, p.y(), 1.0e-9, "после отскока шарик стоит ровно на земле");
    }

    @Test
    void repeatedBouncesLoseEnergyOverTime() {
        // Каждый отскок домножает вертикальную скорость на bounce=0.5 — пик высоты после
        // каждого следующего отскока должен быть меньше предыдущего (затухающий мяч).
        TrinketPhysics p = new TrinketPhysics();
        double firstBounceSpeed = -1;
        double secondBounceSpeed = -1;
        int bounces = 0;
        for (int i = 0; i < 1000 && bounces < 2; i++) {
            if (p.tick(0.02, 20.0, 1.0, 1.0, 0.5, 1.0, 100.0)) {
                bounces++;
                if (bounces == 1) firstBounceSpeed = p.speed();
                else secondBounceSpeed = p.speed();
            }
        }
        assertTrue(firstBounceSpeed > 0 && secondBounceSpeed > 0, "должно было произойти минимум два отскока");
        assertTrue(secondBounceSpeed < firstBounceSpeed,
                "второй отскок должен быть слабее первого: " + secondBounceSpeed + " >= " + firstBounceSpeed);
    }

    @Test
    void tetherKeepsBallWithinBoundedDistance() {
        // Прямой доступ к полям недоступен (инкапсуляция) — проверяем инвариант на реальной
        // траектории: с гравитацией/отскоками и малым tetherRadius шарик не должен улетать
        // в бесконечность, пружина стабилизирует расстояние от центра.
        TrinketPhysics p = new TrinketPhysics();
        double maxDist = 0;
        for (int i = 0; i < 2000; i++) {
            p.tick(0.05, 20.0, 1.0, 0.98, 0.6, 0.9, 1.5);
            double dist = Math.sqrt(p.x() * p.x() + p.z() * p.z());
            maxDist = Math.max(maxDist, dist);
        }
        assertTrue(maxDist < 50.0, "пружина должна удерживать шарик в разумных пределах, было " + maxDist);
    }
}
