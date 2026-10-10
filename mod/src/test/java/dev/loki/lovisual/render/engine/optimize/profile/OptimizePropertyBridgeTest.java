package dev.loki.lovisual.render.engine.optimize.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link OptimizePropertyBridge} — the {@code -Dlovisual.opt.*}
 * override resolver. Each test sets the system property, calls {@link
 * OptimizePropertyBridge#resolve()}, then clears the property.
 */
public final class OptimizePropertyBridgeTest {

    @AfterEach
    void cleanup() {
        for (String k : new String[]{
                "lovisual.opt.all",
                "lovisual.opt.entity_lod", "lovisual.opt.particle_lod",
                "lovisual.opt.spawner_cull", "lovisual.opt.spawner_tick_cache",
                "lovisual.opt.block_entity_cache", "lovisual.opt.prepared_text_cache",
                "lovisual.opt.micro_opts",
                "lovisual.opt.entity_lod_distance", "lovisual.opt.spawner_distance",
                "lovisual.opt.budget_mode", "lovisual.opt.target_fps"
        }) {
            System.clearProperty(k);
        }
        OptimizePropertyBridge.resetCache();
    }

    @Test
    void noPropertiesMeansNoOverrides() {
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertFalse(s.allOff);
        assertFalse(OptimizePropertyBridge.hasOverrides());
        assertNull(s.entityLod);
        assertNull(s.particleLod);
        assertNull(s.budgetMode);
        assertEquals(-1, s.entityLodDistance);
    }

    @Test
    void allFalseDisablesEverything() {
        System.setProperty("lovisual.opt.all", "false");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertTrue(s.allOff);
        // overrides() should return false for every boolean regardless of the per-key override.
        System.setProperty("lovisual.opt.entity_lod", "true");
        s = OptimizePropertyBridge.resolve();
        assertFalse(s.overrides(s.entityLod, true),
                "all=false should disable even an explicit true override");
    }

    @Test
    void entityLodBooleanOverride() {
        System.setProperty("lovisual.opt.entity_lod", "true");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertEquals(Boolean.TRUE, s.entityLod);
        assertTrue(s.overrides(s.entityLod, false));

        System.setProperty("lovisual.opt.entity_lod", "false");
        s = OptimizePropertyBridge.resolve();
        assertEquals(Boolean.FALSE, s.entityLod);
        assertFalse(s.overrides(s.entityLod, true));
    }

    @Test
    void distanceOverride() {
        System.setProperty("lovisual.opt.entity_lod_distance", "100");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertEquals(100, s.entityLodDistance);
        assertEquals(100, s.overrides(s.entityLodDistance, 35));
    }

    @Test
    void distanceOverrideFallsBackWhenUnset() {
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        // -1 means "use the module value".
        assertEquals(35, s.overrides(s.entityLodDistance, 35));
    }

    @Test
    void invalidDistanceFallsBack() {
        System.setProperty("lovisual.opt.entity_lod_distance", "not-a-number");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        // The bridge returns -1 (the fallback) when parsing fails, and overrides()
        // uses the module value when override <= 0.
        assertEquals(-1, s.entityLodDistance);
        assertEquals(35, s.overrides(s.entityLodDistance, 35));
    }

    @Test
    void budgetModeOverride() {
        System.setProperty("lovisual.opt.budget_mode", "soft");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertEquals(FrameBudget.Mode.SOFT, s.budgetMode);

        System.setProperty("lovisual.opt.budget_mode", "aggressive");
        s = OptimizePropertyBridge.resolve();
        assertEquals(FrameBudget.Mode.AGGRESSIVE, s.budgetMode);

        System.setProperty("lovisual.opt.budget_mode", "off");
        s = OptimizePropertyBridge.resolve();
        assertEquals(FrameBudget.Mode.OFF, s.budgetMode);
    }

    @Test
    void budgetModeUnknownStringIsNull() {
        System.setProperty("lovisual.opt.budget_mode", "turbo");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertNull(s.budgetMode);
        // The bridge falls back to the module value when budgetMode is null.
        assertEquals(FrameBudget.Mode.SOFT, s.overrides(s.budgetMode, FrameBudget.Mode.SOFT));
    }

    @Test
    void targetFpsOverride() {
        System.setProperty("lovisual.opt.target_fps", "144");
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        assertEquals(144, s.targetFps);
        assertEquals(144, s.resolveTargetFps(60));
    }

    @Test
    void targetFpsFallsBackWhenUnset() {
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        // Unset = -1 sentinel (the bridge never silently substitutes DEFAULT_TARGET_FPS,
        // so the module value always wins when the property is unset).
        assertEquals(-1, s.targetFps);
        assertEquals(120, s.resolveTargetFps(120));
        assertEquals(60, s.resolveTargetFps(60));
    }

    @Test
    void hasOverridesDetectsAnyProperty() {
        assertFalse(OptimizePropertyBridge.hasOverrides());
        System.setProperty("lovisual.opt.micro_opts", "false");
        assertTrue(OptimizePropertyBridge.hasOverrides());
    }

    @Test
    void allFalseSetsHasOverrides() {
        System.setProperty("lovisual.opt.all", "false");
        assertTrue(OptimizePropertyBridge.hasOverrides());
    }

    @Test
    void lastResolvedMatchesLatest() {
        System.setProperty("lovisual.opt.spawner_cull", "true");
        OptimizePropertyBridge.resolve();
        assertEquals(Boolean.TRUE, OptimizePropertyBridge.lastResolved().spawnerCull);
        System.setProperty("lovisual.opt.spawner_cull", "false");
        OptimizePropertyBridge.resolve();
        assertEquals(Boolean.FALSE, OptimizePropertyBridge.lastResolved().spawnerCull);
    }

    @Test
    void snapshotToStringDoesNotCrash() {
        OptimizePropertyBridge.Snapshot s = OptimizePropertyBridge.resolve();
        String str = s.toString();
        assertTrue(str.contains("entity_lod"));
        assertTrue(str.contains("budget_mode"));
    }
}
