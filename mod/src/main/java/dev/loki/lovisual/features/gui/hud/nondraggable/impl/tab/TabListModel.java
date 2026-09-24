package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.features.relations.category.CategoryType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public facade for the tab-list HUD data model.
 *
 * <p>Exposes the immutable {@link Snapshot} / {@link Entry} records consumed by
 * {@code CustomTabList} and its renderers, plus the three operations used by the
 * HUD tick loop — full {@link #collect collection}, lightweight
 * {@link #refreshDynamic dynamic refresh}, and the {@link #structureSignature FNV-1a}
 * key used to decide when a full collect is required. Layout constants and helper
 * APIs live package-privately so the {@code model/} collaborators
 * ({@link TabListCollector}, {@link TabListReader}, {@link TabListTextSplitter},
 * {@link TabListSignature}) can share them without re-declaring.</p>
 */
public final class TabListModel {

    // Column / row limits ---------------------------------------------------------------------
    static final int MAX_PLAYERS = 80;
    static final int MAX_ROWS_PER_COLUMN = 13;

    // Shell padding ----------------------------------------------------------------------------
    static final float SHELL_PAD_X = 14f;
    static final float SHELL_PAD_TOP = 16f;
    static final float SHELL_PAD_BOTTOM = 15f;

    // Header / footer geometry -----------------------------------------------------------------
    static final float HEADER_LINE_HEIGHT = 29f;
    static final float HEADER_PAD_Y = 8f;
    static final float HEADER_TO_ROWS_GAP = 4f;
    static final float FOOTER_LINE_HEIGHT = 25f;
    static final float FOOTER_PAD_Y = 7f;
    static final float ROWS_TO_FOOTER_GAP = 4f;

    // Row / column spacing ---------------------------------------------------------------------
    static final float ROW_HEIGHT = 47.5f;
    static final float COLUMN_GAP = 11f;
    static final float MIN_COLUMN_WIDTH = 322f;
    static final float MAX_COLUMN_WIDTH = 482f;
    static final float RESERVED_RIGHT_WIDTH = 82f;

    // Vanilla → HUD pixel scale factors --------------------------------------------------------
    static final float NAME_TEXT_SCALE_FROM_VANILLA = 24.5f / 9.0f;
    static final float SCORE_TEXT_SCALE_FROM_VANILLA = 20.5f / 9.0f;
    static final float HEADER_TEXT_SCALE_FROM_VANILLA = 24.0f / 9.0f;
    static final float FOOTER_TEXT_SCALE_FROM_VANILLA = 20.5f / 9.0f;

    private TabListModel() { }

    /** Cap on players drawn by the tab list (vanilla shows up to 80 in the sidebar). */
    public static int maxPlayers() { return MAX_PLAYERS; }

    /** Max rows before the tab list starts a new column. */
    public static int maxRowsPerColumn() { return MAX_ROWS_PER_COLUMN; }

    /**
     * Full pass: sort online players, measure name / score widths, wrap header /
     * footer lines and pack the result into an immutable {@link Snapshot}.
     */
    public static Snapshot collect(Minecraft mc, int screenW) { return TabListCollector.collect(mc, screenW); }

    /**
     * Cheap per-tick pass: only re-reads latency + score for the entries already
     * present in {@code previous}, preserving object identity when unchanged.
     */
    public static Snapshot refreshDynamic(Minecraft mc, Snapshot previous) { return TabListCollector.refreshDynamic(mc, previous); }

    /** FNV-1a key that changes whenever the tab-list structure (order / names / teams / scores) changes. */
    public static long structureSignature(Minecraft mc, int screenW) { return TabListSignature.compute(mc, screenW); }

    /** One visible tab-list row: profile, relation, ping chip metrics and score box metrics. */
    public record Entry(UUID id,
                        String name,
                        Component displayName,
                        Identifier skin,
                        CategoryType relation,
                        int relationColor,
                        int latency,
                        String pingText,
                        float pingTextWidth,
                        float pingChipWidth,
                        boolean spectator,
                        Component scoreText,
                        float scoreWidth,
                        float scoreBoxWidth) {
    }

    /**
     * Immutable snapshot of everything the tab-list renderer needs for one frame:
     * sorted entries, header / footer wrapped lines and the resolved column layout.
     */
    public record Snapshot(List<Entry> entries,
                           Set<UUID> visibleIds,
                           Component header,
                           Component footer,
                           List<Component> headerLines,
                           List<Component> footerLines,
                           int columns,
                           int rows,
                           float columnWidth,
                           float rowHeight,
                           float columnGap,
                           float rowTop,
                           float headerTop,
                           float headerHeight,
                           float footerTop,
                           float footerHeight,
                           float width,
                           float height) {

        /** Rebuild with new entries; recomputes {@code visibleIds} so identity-based diffs stay cheap. */
        Snapshot withEntries(List<Entry> nextEntries) {
            if (nextEntries == null || nextEntries.isEmpty()) return this;
            Set<UUID> ids = new HashSet<>();
            for (Entry entry : nextEntries) {
                if (entry != null && entry.id() != null) ids.add(entry.id());
            }
            return new Snapshot(List.copyOf(nextEntries), Set.copyOf(ids), header, footer, headerLines, footerLines,
                    columns, rows, columnWidth, rowHeight, columnGap, rowTop, headerTop, headerHeight,
                    footerTop, footerHeight, width, height);
        }

        /** Placeholder snapshot used when there is no server connection or before the first collect. */
        static Snapshot empty() {
            return new Snapshot(List.of(), Set.of(), null, null, List.of(), List.of(), 1, 1, MIN_COLUMN_WIDTH, ROW_HEIGHT, 0f,
                    SHELL_PAD_TOP, SHELL_PAD_TOP, 0f, SHELL_PAD_TOP + ROW_HEIGHT, 0f, 350f,
                    SHELL_PAD_TOP + ROW_HEIGHT + SHELL_PAD_BOTTOM);
        }
    }
}
