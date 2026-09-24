package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import com.mojang.authlib.GameProfile;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.mixins.accessors.hud.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;

import java.util.UUID;

/**
 * FNV-1a style rolling hash over every field that changes the tab-list structure
 * (order, name, team, relation, spectator flag, score text). Used to detect when
 * a full {@link TabListCollector#collect} pass is required vs a lightweight
 * latency / score refresh. Sibling folder sharing the parent's package.
 */
final class TabListSignature {

    private TabListSignature() { }

    /** Stable across frames as long as the tab-list structure (not just ping / score) is unchanged. */
    static long compute(Minecraft mc, int screenW) {
        if (mc == null || mc.getConnection() == null) return 0L;
        long h = 0x6A09E667F3BCC909L;
        h = mix(h, screenW);
        PlayerTabOverlay overlay = mc.gui != null && mc.gui.hud != null ? mc.gui.hud.getTabList() : null;
        h = mix(h, readTabTextRaw(overlay, true));
        h = mix(h, readTabTextRaw(overlay, false));
        Objective objective = TabListReader.listObjective(mc);
        h = mix(h, objective != null ? objective.getName() : "");
        h = mix(h, objective != null && objective.getRenderType() != null ? objective.getRenderType().name() : "");
        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            if (profile == null || profile.id() == null) continue;
            h = mix(h, profile.id());
            h = mix(h, profile.name());
            h = mix(h, info.getTabListOrder());
            h = mix(h, info.getGameMode() == GameType.SPECTATOR ? 1 : 0);
            h = mix(h, TabListReader.teamName(info));
            h = mix(h, info.getTabListDisplayName());
            String name = profile.name() != null ? profile.name() : "";
            CategoryType relation = CategoryService.get(name);
            h = mix(h, relation.ordinal());
            h = mix(h, relation == CategoryType.DEFAULT ? 0 : CategoryService.getColor(name));
            h = mix(h, TabListReader.scoreText(mc, profile, objective));
        }
        return h;
    }

    private static Component readTabTextRaw(PlayerTabOverlay overlay, boolean header) {
        if (!(overlay instanceof PlayerTabOverlayAccessor accessor)) return null;
        return header ? accessor.lovisual$getHeader() : accessor.lovisual$getFooter();
    }

    private static long mix(long h, int value) { return (h ^ value) * 0x100000001B3L; }

    private static long mix(long h, UUID value) {
        if (value == null) return mix(h, 0);
        h = (h ^ value.getMostSignificantBits()) * 0x100000001B3L;
        return (h ^ value.getLeastSignificantBits()) * 0x100000001B3L;
    }

    private static long mix(long h, String value) { return (h ^ (value != null ? value.hashCode() : 0)) * 0x100000001B3L; }

    private static long mix(long h, Component value) { return mix(h, value != null ? value.getString() : ""); }
}
