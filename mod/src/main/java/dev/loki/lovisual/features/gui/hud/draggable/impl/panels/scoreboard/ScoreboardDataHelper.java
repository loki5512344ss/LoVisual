package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.util.logging.ServerDumpUtil;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;

import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.screen.ClientScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ScoreboardDataHelper {

    private static final Comparator<PlayerScoreEntry> ENTRY_COMPARATOR =
            Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                    .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    static final List<Scoreboard.SidebarLine> PREVIEW_SAMPLE = List.of(
            new Scoreboard.SidebarLine(Component.literal("Entry One"), Component.literal("12"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Two"), Component.literal("34"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Three"), Component.literal("56"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Four"), Component.literal("78"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Five"), Component.literal("90"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Six"), Component.literal("12%"), false),
            new Scoreboard.SidebarLine(Component.literal("Entry Seven"), Component.literal("3"), false),
            new Scoreboard.SidebarLine(Component.literal("Footer Text"), Component.empty(), true)
    );

    private ScoreboardDataHelper() {}

    static Objective resolveObjective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return null;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = null;
        PlayerTeam team = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
        if (team != null) {
            DisplaySlot slot = team.getColor().map(net.minecraft.world.scores.TeamColor::displaySlot).orElse(null);
            if (slot != null) objective = scoreboard.getDisplayObjective(slot);
        }
        return objective != null ? objective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    static Component resolveTitle(Objective objective, boolean preview) {
        if (objective == null) return preview ? Component.literal("Scoreboard") : Component.empty();
        Component title = LegacyTextUtil.convertLegacyCodes(objective.getDisplayName());
        return isBlankText(title) ? Component.literal("Scoreboard") : title;
    }

    static List<Scoreboard.SidebarLine> resolveLines(Objective objective, boolean preview) {
        if (objective == null) return preview ? PREVIEW_SAMPLE : List.of();
        net.minecraft.world.scores.Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<Scoreboard.SidebarLine> resolved = new ArrayList<>();
        List<PlayerScoreEntry> visible = scoreboard.listPlayerScores(objective).stream()
                .filter(e -> !e.isHidden()).sorted(ENTRY_COMPARATOR).limit(15).toList();
        for (PlayerScoreEntry entry : visible) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component label = LegacyTextUtil.convertLegacyCodes(PlayerTeam.formatNameForTeam(team, entry.ownerName()));
            Component value = LegacyTextUtil.convertLegacyCodes(entry.formatValue(numberFormat));
            if (isBlankText(label) && isBlankText(value)) continue;
            resolved.add(new Scoreboard.SidebarLine(label, value, isBlankText(value)));
        }
        ServerDumpUtil.dumpScoreboardSidebar(objective, visible,
                resolved.stream().map(l -> l.label().getString()).toList(),
                resolved.stream().map(l -> l.value().getString()).toList());
        return resolved.isEmpty() && preview ? PREVIEW_SAMPLE : resolved;
    }

    static void toggleNoRenderHidden() {
        NoRender noRender = Modules.get(NoRender.class);
        if (noRender == null) return;
        noRender.setHideScoreboardRaw(!noRender.hideScoreboardRaw());
    }

    static void handleTick(Scoreboard board) {
        Minecraft mc = Minecraft.getInstance();
        if (!board.toggleInit) {
            if (mc == null || mc.getWindow() == null) return;
            board.toggleArmed = true;
            String combo = board.toggleBind.get();
            if (combo != null && !combo.isBlank() && !"NONE".equalsIgnoreCase(combo)
                    && ClientScreen.current() == null && KeyManager.isComboHeldAllowScreen(combo)) {
                board.toggleArmed = false;
            }
            board.toggleInit = true;
        }
        String combo = board.toggleBind.get();
        if (combo == null || combo.isBlank() || "NONE".equalsIgnoreCase(combo)) return;
        if (mc == null || mc.getWindow() == null) return;
        boolean pressed = ClientScreen.current() == null && KeyManager.isComboHeldAllowScreen(combo);
        if (!pressed) {
            board.toggleArmed = true;
            return;
        }
        if (board.toggleArmed) {
            toggleNoRenderHidden();
            board.toggleArmed = false;
        }
    }

    static boolean isBlankText(Component text) {
        return text == null || text.getString().isBlank();
    }

    static Component normalizeHeaderTitle(Component text) {
        if (text == null) return Component.empty();
        MutableComponent out = Component.empty();
        text.visit((style, value) -> {
            if (value != null && !value.isEmpty()) {
                out.append(Component.literal(normalizeHeaderGlyphs(value)).setStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }

    static String normalizeHeaderGlyphs(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            out.append(switch (ch) {
                case 'ᴀ' -> 'a'; case 'ʙ' -> 'b'; case 'ᴄ' -> 'c'; case 'ᴅ' -> 'd';
                case 'ᴇ' -> 'e'; case 'ꜰ' -> 'f'; case 'ɢ' -> 'g'; case 'ʜ' -> 'h';
                case 'ɪ' -> 'i'; case 'ᴊ' -> 'j'; case 'ᴋ' -> 'k'; case 'ʟ' -> 'l';
                case 'ᴍ' -> 'm'; case 'ɴ' -> 'n'; case 'ᴏ' -> 'o'; case 'ᴘ' -> 'p';
                case 'ǫ' -> 'q'; case 'ʀ' -> 'r'; case 'ꜱ' -> 's'; case 'ᴛ' -> 't';
                case 'ᴜ' -> 'u'; case 'ᴠ' -> 'v'; case 'ᴡ' -> 'w'; case 'x' -> 'x';
                case 'ʏ' -> 'y'; case 'ᴢ' -> 'z'; default -> ch;
            });
        }
        return out.toString();
    }
}
