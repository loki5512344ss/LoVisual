package dev.loki.lovisual.mixins.network.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.features.relations.StaffTracker;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;

import java.util.UUID;

/**
 * Entity and tab-list packet bodies for {@link ClientPacketListenerMixin}: feeds the
 * staff tracker and totem-pop counter from spawn/despawn and player-info updates.
 * Physically under {@code handlers/} but declares the parent package for direct access.
 */
final class ClientPacketListenerStaffHandler {

    private ClientPacketListenerStaffHandler() {
    }

    static void onEntitiesDestroy(ClientboundRemoveEntitiesPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return;

        for (int id : packet.getEntityIds()) {

            Entity e = world.getEntity(id);
            if (!(e instanceof Player player)) continue;

            StaffTracker.INSTANCE.onEntityDisappear(player.getUUID());
        }
    }

    static void onEntitySpawn(ClientboundAddEntityPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return;

        if (packet.getType() != net.minecraft.world.entity.EntityTypes.PLAYER) return;

        Entity entity = world.getEntity(packet.getId());
        if (!(entity instanceof Player player)) return;

        StaffTracker.INSTANCE.onEntityAppear(player.getUUID(), player.getGameProfile().name());
    }

    static void onPlayerList(ClientboundPlayerInfoUpdatePacket packet) {
        var actions = packet.actions();
        boolean listedAffects = actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED)
                || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

        boolean gmAffects = actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)
                || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

        for (var entry : packet.entries()) {

            var uuid = entry.profileId();
            String name = entry.profile() != null ? entry.profile().name() : null;

            Boolean listed = listedAffects ? entry.listed() : null;
            var gm = gmAffects ? entry.gameMode() : null;

            // ВАЖНО: берём ПОЛНЫЙ Text из TAB
            Component displayName = entry.displayName();
            if (displayName == null && name != null) {
                displayName = Component.literal(name);
            }

            StaffTracker.INSTANCE.onTabUpdate(uuid, name, displayName, listed, gm);
        }
    }

    static void onPlayerRemove(ClientboundPlayerInfoRemovePacket packet) {
        for (UUID id : packet.profileIds()) {

            // убрали из TAB
            StaffTracker.INSTANCE.onTabUpdate(id, null, null, false, null);
            TotemPopCounter.onPlayerLogout(id);
            // ВАЖНО — фикс VANISH после кика
            StaffTracker.INSTANCE.onEntityDisappear(id);
        }
    }
}
