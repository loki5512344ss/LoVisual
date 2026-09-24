package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import com.mojang.authlib.GameProfile;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab.TabListModel.Entry;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab.TabListModel.Snapshot;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Reads the vanilla {@link PlayerTabOverlay} + scoreboard once and builds an
 * immutable {@link Snapshot} describing the tab-list layout. Extracted from
 * {@link TabListModel} so the facade stays within the two-hundred-line cap;
 * sibling folder sharing the parent's package. Field access goes through
 * {@link TabListReader}, line splitting through {@link TabListTextSplitter}.
 */
final class TabListCollector {

    private TabListCollector() { }

    /** Sort + trim the online-player list, then measure name / score widths for column layout. */
    static Snapshot collect(Minecraft mc, int screenW) {
        if (mc == null || mc.getConnection() == null) return Snapshot.empty();
        PlayerTabOverlay overlay = mc.gui != null && mc.gui.hud != null ? mc.gui.hud.getTabList() : null;
        Component header = TabListReader.tabText(overlay, true);
        Component footer = TabListReader.tabText(overlay, false);
        List<Component> headerLines = TabListTextSplitter.split(mc, header, Math.max(40, screenW - 50));
        List<Component> footerLines = TabListTextSplitter.split(mc, footer, Math.max(40, screenW - 50));
        Objective listObjective = TabListReader.listObjective(mc);
        List<Entry> entries = new ArrayList<>();
        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
        players.sort(Comparator
                .comparingInt((PlayerInfo info) -> -info.getTabListOrder())
                .thenComparing(info -> info.getGameMode() == GameType.SPECTATOR)
                .thenComparing(info -> TabListReader.teamName(info).toLowerCase(Locale.ROOT))
                .thenComparing(info -> TabListReader.profileName(info).toLowerCase(Locale.ROOT), String.CASE_INSENSITIVE_ORDER));
        int maxNameVanillaWidth = 0, maxScoreVanillaWidth = 0;
        Set<UUID> visibleIds = new HashSet<>();
        int limit = Math.min(players.size(), TabListModel.MAX_PLAYERS);
        for (int i = 0; i < limit; i++) {
            PlayerInfo info = players.get(i);
            GameProfile profile = info.getProfile();
            if (profile == null || profile.id() == null) continue;
            UUID id = profile.id();
            String name = profile.name() != null ? profile.name() : "";
            Component display = TabListReader.displayName(mc, info, name);
            Component scoreText = TabListReader.scoreText(mc, profile, listObjective);
            CategoryType relation = CategoryService.get(name);
            int relationColor = relation == CategoryType.DEFAULT ? 0 : CategoryService.getColor(name);
            Identifier skin = PlayerSkinResolver.resolveProfileSkin(profile);
            if (mc.font != null) {
                maxNameVanillaWidth = Math.max(maxNameVanillaWidth, mc.font.width(display));
                if (scoreText != null) maxScoreVanillaWidth = Math.max(maxScoreVanillaWidth, mc.font.width(scoreText));
            }
            int latency = info.getLatency();
            EntryMetrics m = entryMetrics(scoreText, latency);
            entries.add(new Entry(id, name, display, skin, relation, relationColor, latency,
                    m.pingText(), m.pingTextWidth(), m.pingChipWidth(),
                    info.getGameMode() == GameType.SPECTATOR, scoreText, m.scoreWidth(), m.scoreBoxWidth()));
            visibleIds.add(id);
        }
        return buildSnapshot(entries, visibleIds, header, footer, headerLines, footerLines, maxNameVanillaWidth, maxScoreVanillaWidth, screenW);
    }

    /** Recompute latency / score per entry against the previous snapshot; keeps identity when unchanged. */
    static Snapshot refreshDynamic(Minecraft mc, Snapshot previous) {
        if (mc == null || mc.getConnection() == null || previous == null || previous.entries().isEmpty()) return previous;
        Objective listObjective = TabListReader.listObjective(mc);
        List<Entry> entries = new ArrayList<>(previous.entries().size());
        boolean changed = false;
        for (Entry entry : previous.entries()) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(entry.id());
            if (info == null || info.getProfile() == null) return previous;
            Component scoreText = TabListReader.scoreText(mc, info.getProfile(), listObjective);
            int latency = info.getLatency();
            Entry next = withDynamic(entry, latency, scoreText);
            changed |= next != entry;
            entries.add(next);
        }
        return changed ? previous.withEntries(entries) : previous;
    }

    private static Snapshot buildSnapshot(List<Entry> entries, Set<UUID> visibleIds, Component header, Component footer,
                                          List<Component> headerLines, List<Component> footerLines,
                                          int maxNameVanillaWidth, int maxScoreVanillaWidth, int screenW) {
        int count = entries.size();
        int columns = Math.max(1, (int) Math.ceil(count / (float) TabListModel.MAX_ROWS_PER_COLUMN));
        int rows = Math.max(1, Math.min(TabListModel.MAX_ROWS_PER_COLUMN, count));
        float nameDrivenWidth = 72f + maxNameVanillaWidth * TabListModel.NAME_TEXT_SCALE_FROM_VANILLA;
        float scoreDrivenWidth = maxScoreVanillaWidth > 0 ? maxScoreVanillaWidth * TabListModel.SCORE_TEXT_SCALE_FROM_VANILLA + 18f : 0f;
        float wantedColumnWidth = nameDrivenWidth + scoreDrivenWidth + TabListModel.RESERVED_RIGHT_WIDTH;
        float maxUsableWidth = Math.max(TabListModel.MIN_COLUMN_WIDTH, screenW - 58f - (columns - 1) * TabListModel.COLUMN_GAP - TabListModel.SHELL_PAD_X * 2f);
        float columnWidth = Math.max(TabListModel.MIN_COLUMN_WIDTH, Math.min(TabListModel.MAX_COLUMN_WIDTH, Math.min(wantedColumnWidth, maxUsableWidth / Math.max(1, columns))));
        float columnGap = columns > 1 ? TabListModel.COLUMN_GAP : 0f;
        float contentWidth = columns * columnWidth + (columns - 1) * columnGap;
        float headerWidth = TabListReader.maxVanillaLineWidth(headerLines, TabListModel.HEADER_TEXT_SCALE_FROM_VANILLA);
        float footerWidth = TabListReader.maxVanillaLineWidth(footerLines, TabListModel.FOOTER_TEXT_SCALE_FROM_VANILLA);
        float width = Math.max(contentWidth, Math.max(headerWidth, footerWidth)) + TabListModel.SHELL_PAD_X * 2f;
        float headerTop = TabListModel.SHELL_PAD_TOP;
        float headerHeight = headerLines.isEmpty() ? 0f : headerLines.size() * TabListModel.HEADER_LINE_HEIGHT + TabListModel.HEADER_PAD_Y * 2f;
        float rowTop = TabListModel.SHELL_PAD_TOP + (headerLines.isEmpty() ? 0f : headerHeight + TabListModel.HEADER_TO_ROWS_GAP);
        float footerTop = rowTop + rows * TabListModel.ROW_HEIGHT + (footerLines.isEmpty() ? 0f : TabListModel.ROWS_TO_FOOTER_GAP);
        float footerHeight = footerLines.isEmpty() ? 0f : footerLines.size() * TabListModel.FOOTER_LINE_HEIGHT + TabListModel.FOOTER_PAD_Y * 2f;
        float height = footerTop + footerHeight + TabListModel.SHELL_PAD_BOTTOM;
        return new Snapshot(List.copyOf(entries), Set.copyOf(visibleIds), header, footer, headerLines, footerLines, columns, rows,
                columnWidth, TabListModel.ROW_HEIGHT, columnGap, rowTop, headerTop, headerHeight, footerTop, footerHeight, width, height);
    }

    private static Entry withDynamic(Entry entry, int latency, Component scoreText) {
        if (entry.latency() == latency && TabListReader.sameText(entry.scoreText(), scoreText)) return entry;
        EntryMetrics m = entryMetrics(scoreText, latency);
        return new Entry(entry.id(), entry.name(), entry.displayName(), entry.skin(), entry.relation(), entry.relationColor(),
                latency, m.pingText(), m.pingTextWidth(), m.pingChipWidth(), entry.spectator(), scoreText, m.scoreWidth(), m.scoreBoxWidth());
    }

    private static EntryMetrics entryMetrics(Component scoreText, int latency) {
        String pingText = latency < 0 ? "?" : String.valueOf(latency);
        float pingWidth = TabRichTextRenderer.widthPlain(pingText, 17.5f);
        float pingChipWidth = Math.max(43f, pingWidth + 24f);
        float scoreWidth = scoreText != null ? Math.min(64f, TabRichTextRenderer.width(scoreText, 20.5f)) : 0f;
        float scoreBoxWidth = scoreWidth > 0f ? scoreWidth + 16f : 0f;
        return new EntryMetrics(pingText, pingWidth, pingChipWidth, scoreWidth, scoreBoxWidth);
    }

    /** Latency + score pixel measurements needed to size the ping chip and score box. */
    record EntryMetrics(String pingText, float pingTextWidth, float pingChipWidth, float scoreWidth, float scoreBoxWidth) { }
}
