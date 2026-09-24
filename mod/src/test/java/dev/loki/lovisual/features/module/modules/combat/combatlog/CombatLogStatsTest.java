package dev.loki.lovisual.features.module.modules.combat.combatlog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogEntry;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogStats;

public final class CombatLogStatsTest {

    @Test
    void sumsDealtAndTakenSeparately() {
        List<CombatLogEntry> entries = List.of(
                new CombatLogEntry("Bob", 4.0f, true, 1000L),
                new CombatLogEntry("Bob", 2.0f, false, 1200L)
        );

        CombatLogStats.Summary summary = CombatLogStats.summarize(entries, 2000L, 10_000L);

        assertEquals(4.0f, summary.totalDealt());
        assertEquals(2.0f, summary.totalTaken());
    }

    @Test
    void computesDpsOverWindow() {
        List<CombatLogEntry> entries = List.of(
                new CombatLogEntry("Bob", 10.0f, true, 1000L)
        );

        CombatLogStats.Summary summary = CombatLogStats.summarize(entries, 1000L, 5000L);

        assertEquals(2.0f, summary.dpsDealt());
    }

    @Test
    void ignoresEntriesOutsideWindow() {
        List<CombatLogEntry> entries = List.of(
                new CombatLogEntry("Bob", 10.0f, true, 0L)
        );

        CombatLogStats.Summary summary = CombatLogStats.summarize(entries, 20_000L, 5000L);

        assertEquals(0.0f, summary.totalDealt());
    }

    @Test
    void picksTopTargetAndAttackerByTotalDamage() {
        List<CombatLogEntry> entries = List.of(
                new CombatLogEntry("Bob", 3.0f, true, 1000L),
                new CombatLogEntry("Alice", 5.0f, true, 1000L),
                new CombatLogEntry("Carol", 8.0f, false, 1000L),
                new CombatLogEntry("Dan", 1.0f, false, 1000L)
        );

        CombatLogStats.Summary summary = CombatLogStats.summarize(entries, 1000L, 10_000L);

        assertEquals("Alice", summary.topTarget());
        assertEquals("Carol", summary.topAttacker());
    }

    @Test
    void emptyEntriesGiveEmptySummary() {
        CombatLogStats.Summary summary = CombatLogStats.summarize(List.of(), 1000L, 10_000L);

        assertEquals(0.0f, summary.totalDealt());
        assertEquals(0.0f, summary.totalTaken());
        assertNull(summary.topTarget());
        assertNull(summary.topAttacker());
    }
}
