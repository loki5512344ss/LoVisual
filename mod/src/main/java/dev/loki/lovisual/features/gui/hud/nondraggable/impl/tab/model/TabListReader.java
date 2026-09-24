package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import com.mojang.authlib.GameProfile;
import dev.loki.lovisual.mixins.accessors.hud.PlayerTabOverlayAccessor;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;

/**
 * Thin accessors that resolve the fields {@link TabListCollector} and
 * {@link TabListSignature} need from Minecraft's tab-list overlay, scoreboard and
 * player-info entries. Each method normalises legacy colour codes where relevant.
 */
final class TabListReader {

    private TabListReader() { }

    /** Overlay display name → tab-list override → literal fallback; always legacy-code converted. */
    static Component displayName(Minecraft mc, PlayerInfo info, String fallbackName) {
        Component display = null;
        if (mc != null && mc.gui != null && mc.gui.hud != null && mc.gui.hud.getTabList() != null) {
            display = mc.gui.hud.getTabList().getNameForDisplay(info);
        }
        if (display == null) display = info.getTabListDisplayName();
        if (display == null) display = Component.literal(fallbackName);
        return LegacyTextUtil.convertLegacyCodes(display);
    }

    /** Header / footer component pulled from the vanilla overlay via mixin accessor; blank → null. */
    static Component tabText(PlayerTabOverlay overlay, boolean header) {
        if (!(overlay instanceof PlayerTabOverlayAccessor accessor)) return null;
        Component value = header ? accessor.lovisual$getHeader() : accessor.lovisual$getFooter();
        if (value == null || value.getString().isBlank()) return null;
        return LegacyTextUtil.convertLegacyCodes(value);
    }

    /** The score the vanilla tab-list shows for {@code profile}, or null when no objective is bound. */
    static Component scoreText(Minecraft mc, GameProfile profile, Objective objective) {
        if (mc == null || mc.level == null || profile == null || objective == null) return null;
        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) return null;
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ScoreHolder.fromGameProfile(profile), objective);
        if (score == null && profile.name() != null && !profile.name().isBlank()) {
            score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(profile.name()), objective);
        }
        return score == null ? null : Component.literal(String.valueOf(score.value()));
    }

    static Objective listObjective(Minecraft mc) {
        if (mc == null || mc.level == null) return null;
        Scoreboard scoreboard = mc.level.getScoreboard();
        return scoreboard != null ? scoreboard.getDisplayObjective(DisplaySlot.LIST) : null;
    }

    static String teamName(PlayerInfo info) {
        return info.getTeam() != null && info.getTeam().getName() != null ? info.getTeam().getName() : "";
    }

    static String profileName(PlayerInfo info) {
        GameProfile profile = info.getProfile();
        return profile != null && profile.name() != null ? profile.name() : "";
    }

    /** Widest vanilla-font line, scaled to the LoVisual HUD pixel budget. */
    static float maxVanillaLineWidth(List<Component> lines, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || lines == null || lines.isEmpty()) return 0f;
        int max = 0;
        for (Component line : lines) if (line != null) max = Math.max(max, mc.font.width(line));
        return max * scale;
    }

    static boolean sameText(Component a, Component b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getString().equals(b.getString());
    }
}
