package dev.loki.lovisual.util.player.inventory.firework;

import dev.loki.lovisual.mixins.accessors.inventory.PlayerInventoryAccessor;
import dev.loki.lovisual.mixins.accessors.misc.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;

/**
 * Shared firework-use path for elytra modules — hotbar-only, без InventorySwap.
 */
public final class FireworkUseController {
    public static final FireworkUseController INSTANCE = new FireworkUseController();

    public static final int DEFAULT_MANUAL_COOLDOWN_TICKS = 20;
    private static final long MILLIS_PER_TICK = 50L;

    private long lastUseMs;

    private FireworkUseController() {
    }

    public boolean use(float yaw,
                       float pitch,
                       boolean allowInventory,
                       boolean restore,
                       boolean silent,
                       int cooldownTicks,
                       boolean syncCombat) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiPlayerGameMode manager = mc.gameMode;
        if (player == null || manager == null || mc.level == null) return false;
        if (player.inventoryMenu == null) return false;
        if (player.isUsingItem()) return false;
        if (!canPassCooldown(cooldownTicks)) return false;
        if (hasOwnedActiveFirework(player)) return false;
        if (syncCombat && isKillAuraAttackWindowBusy()) return false;

        boolean used;
        if (player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            used = useWithTemporaryRotation(player, yaw, pitch, InteractionHand.OFF_HAND);
        } else {
            used = useMainHandRocket(player, yaw, pitch, restore);
        }

        if (used) {
            lastUseMs = System.currentTimeMillis();
        }
        return used;
    }

    public boolean canPassCooldown(int cooldownTicks) {
        int safeTicks = Math.max(0, cooldownTicks);
        long delayMs = safeTicks * MILLIS_PER_TICK;
        return System.currentTimeMillis() - lastUseMs >= delayMs;
    }

    public boolean isCooling(int cooldownTicks) {
        return !canPassCooldown(cooldownTicks);
    }

    private boolean useMainHandRocket(LocalPlayer player, float yaw, float pitch, boolean restore) {
        int slot = findFireworkInHotbar(player);
        if (slot == -1) return false;
        Minecraft mc = Minecraft.getInstance();
        int original = ((PlayerInventoryAccessor) player.getInventory()).lovisual$getSelectedSlot();
        if (slot != original) {
            ((PlayerInventoryAccessor) player.getInventory()).lovisual$setSelectedSlot(slot);
            syncSlot(slot);
        }
        boolean ok = useWithTemporaryRotation(player, yaw, pitch, InteractionHand.MAIN_HAND);
        if (restore && slot != original) {
            ((PlayerInventoryAccessor) player.getInventory()).lovisual$setSelectedSlot(original);
            syncSlot(original);
        }
        return ok;
    }

    private int findFireworkInHotbar(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) return i;
        }
        return -1;
    }

    private void syncSlot(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        if (mc.gameMode instanceof MultiPlayerGameModeAccessor accessor) {
            accessor.lovisual$setLastSelectedSlot(slot);
        }
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    private boolean useWithTemporaryRotation(LocalPlayer player, float yaw, float pitch, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode manager = mc.gameMode;
        if (manager == null) return false;
        float oldYaw = player.getYRot();
        float oldPitch = player.getXRot();
        player.setYRot(yaw);
        player.setXRot(pitch);
        try {
            InteractionResult result = manager.useItem(player, hand);
            if (!result.consumesAction()) {
                return false;
            }
            player.swing(hand);
            return true;
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
        }
    }

    private boolean hasOwnedActiveFirework(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity firework
                    && firework.isAlive()
                    && firework.getOwner() == player) {
                return true;
            }
        }
        return false;
    }

    private boolean isKillAuraAttackWindowBusy() {
        return false;
    }
}
