package dev.loki.lovisual.mixins.network.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.entity.AttackEntityEvent;
import dev.loki.lovisual.events.impl.world.EventBreakBlock;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.damage.NoFriendDamage;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.features.module.modules.misc.sound.HitSounds;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.HitBubbles;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.HitEffect;
import dev.loki.lovisual.mixins.accessors.world.WorldAccessor;
import dev.loki.lovisual.util.combat.tracking.ReachMeter;
import dev.loki.lovisual.util.target.TargetManager;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipTickWithoutPlayer(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            ci.cancel();
        }
    }

    @Inject(method = "ensureHasSentCarriedItem", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipCarriedItemSyncWithoutPlayer(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void lovisual$breakBlockEvent(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!Events.BUS.hasListeners(EventBreakBlock.class)) return;
        Events.BUS.post(new EventBreakBlock(pos));
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItemHead(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;

        PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
        if (cooldowns == null) return;

        // No hard-coded PvP item blocking here anymore. This path is opt-in and item-list based.
        if (cooldowns.shouldBlockItemUse(stack.getItem())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void onInteractItemReturn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (!result.consumesAction()) return;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;

        PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
        if (cooldowns == null) return;

        cooldowns.tryStartLocalCooldown(stack.getItem(), ItemCooldownRulesValue.Trigger.INTERACT_ACCEPT);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void freecam$block(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                               CallbackInfoReturnable<InteractionResult> cir) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && !fc.allowInteract()) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void freecam$item(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && !fc.allowInteract()) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void freecam$entity(Player player, Entity entity, EntityHitResult hit, InteractionHand hand,
                                CallbackInfoReturnable<InteractionResult> cir) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && !fc.allowInteract()) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        NoFriendDamage noFriendDamage = Modules.get(NoFriendDamage.class);
        if (noFriendDamage != null && noFriendDamage.shouldBlockAttack(target)) {
            ci.cancel();
            return;
        }

        if (!((WorldAccessor) player.level()).lovisual$isClient()) return;

        if (Events.BUS.hasListeners(AttackEntityEvent.class)) {
            Events.BUS.post(new AttackEntityEvent(player, target));
        }

        HitSounds hitSounds = Modules.get(HitSounds.class);
        if (hitSounds != null) {
            hitSounds.handleHit(target);
        }

        HitEffect hitEffect = Modules.get(HitEffect.class);
        if (hitEffect != null) {
            hitEffect.handleHit(target);
        }

        HitBubbles hitBubbles = Modules.get(HitBubbles.class);
        if (hitBubbles != null) {
            hitBubbles.handleHit(target);
        }

        TargetHud.notifyHit(target);
        TargetManager.onAttack(target);

        double reachDistance = ReachMeter.distanceToBox(player.getEyePosition(), target.getBoundingBox());
        if (reachDistance >= 0.0) {
            ReachMeter.record(reachDistance, System.currentTimeMillis());
        }
    }

}
