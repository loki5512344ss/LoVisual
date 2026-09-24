package dev.loki.lovisual.features.module.modules.combat.predictions;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.AimPrediction;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.Kind;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.*;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.features.module.modules.combat.prediction.Predictions;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsSimulation;

/**
 * Held-item aim prediction for the Predictions module: builds the hypothetical
 * projectile for the item in hand and simulates its trajectory.
 */
public final class PredictionsAim {
    private PredictionsAim() {
    }

    public static AimPrediction computeAimPrediction(Player player, float tickDelta,
                                                     NumberValue<Integer> maxSimTicks,
                                                     NumberValue<Integer> pearlMaxSimTicks,
                                                     BooleanValue pearlColorToggle,
                                                     RGBColorValue pearlColorValue) {
        Projectile projectile = createHeldProjectile(player, tickDelta);
        if (projectile == null) {
            return null;
        }

        Kind kind = PredictionsModel.kindFromProjectile(projectile);
        TrajectoryResult result = PredictionsSimulation.simulateEntity(projectile, kind, tickDelta,
                maxSimTicks, pearlMaxSimTicks, pearlColorToggle, pearlColorValue);
        if (result == null || result.points().isEmpty()) {
            return null;
        }

        Vec3 hitPos = result.impactPos() != null ? result.impactPos() : result.points().getLast();
        Entity hitEntity = result.hitEntity();
        Vec3 anchorPos;
        if (hitEntity instanceof LivingEntity living) {
            anchorPos = living.getPosition(tickDelta).add(0.0, living.getBbHeight() * 0.5, 0.0);
        } else if (hitEntity != null) {
            anchorPos = hitEntity.getBoundingBox().getCenter();
        } else {
            anchorPos = hitPos;
        }

        return new AimPrediction(hitEntity, hitPos, anchorPos, result.impactNormal(), result.iconStack(), result);
    }

    private static Projectile createHeldProjectile(Player player, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) {
            return null;
        }

        InteractionHand hand = null;
        ItemStack handStack = ItemStack.EMPTY;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isAimUsable(player, mainHand, InteractionHand.MAIN_HAND)) {
            hand = InteractionHand.MAIN_HAND;
            handStack = mainHand;
        } else {
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (isAimUsable(player, offHand, InteractionHand.OFF_HAND)) {
                hand = InteractionHand.OFF_HAND;
                handStack = offHand;
            }
        }

        if (hand == null || handStack.isEmpty()) {
            return null;
        }

        Projectile projectile = null;
        float speed = 0.0f;
        float roll = 0.0f;

        if (handStack.is(Items.ENDER_PEARL)) {
            projectile = new ThrownEnderpearl(world, player, handStack);
            speed = 1.5f;
        } else if (handStack.is(Items.SNOWBALL)) {
            projectile = new Snowball(world, player, handStack);
            speed = 1.5f;
        } else if (handStack.is(Items.EGG)) {
            projectile = new ThrownEgg(world, player, handStack);
            speed = 1.5f;
        } else if (handStack.is(Items.SPLASH_POTION) || handStack.is(Items.LINGERING_POTION)) {
            projectile = handStack.is(Items.LINGERING_POTION)
                    ? new ThrownLingeringPotion(world, player, handStack)
                    : new ThrownSplashPotion(world, player, handStack);
            speed = 0.5f;
            roll = -20.0f;
        } else if (handStack.is(Items.EXPERIENCE_BOTTLE)) {
            projectile = new ThrownExperienceBottle(world, player, handStack);
            speed = 0.7f;
            roll = -20.0f;
        } else if (handStack.is(Items.WIND_CHARGE)) {
            projectile = new WindCharge(player, world, player.position().x, player.getEyePosition().y, player.position().z);
            speed = 1.5f;
        } else if (handStack.is(Items.FIRE_CHARGE)) {
            projectile = new SmallFireball(world, player, Vec3.ZERO);
            speed = 1.0f;
        } else if (handStack.is(Items.TRIDENT) && player.isUsingItem() && player.getUsedItemHand() == hand) {
            projectile = new ThrownTrident(world, player, handStack);
            speed = 2.5f;
        } else if (handStack.is(Items.BOW) && player.isUsingItem() && player.getUsedItemHand() == hand) {
            float pull = BowItem.getPowerForTime(player.getTicksUsingItem());
            if (pull <= 0.0f) {
                return null;
            }
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            projectile = new Arrow(world, player, arrowStack, handStack);
            speed = 3.0f * pull;
        } else if (handStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(handStack)) {
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            projectile = new Arrow(world, player, arrowStack, handStack);
            speed = 3.15f;
        }

        if (projectile == null) {
            return null;
        }

        Vec3 eye = player.getEyePosition(tickDelta);
        projectile.absSnapTo(eye.x, eye.y, eye.z);
        setHeldProjectileVelocity(projectile, player, player.getXRot(), player.getYRot(), roll, speed);
        return projectile;
    }

    private static void setHeldProjectileVelocity(Projectile projectile, Entity shooter, float pitch, float yaw, float roll, float speed) {
        float x = -Mth.sin(yaw * ((float) Math.PI / 180.0f)) * Mth.cos(pitch * ((float) Math.PI / 180.0f));
        float y = -Mth.sin((pitch + roll) * ((float) Math.PI / 180.0f));
        float z = Mth.cos(yaw * ((float) Math.PI / 180.0f)) * Mth.cos(pitch * ((float) Math.PI / 180.0f));

        Vec3 velocity = new Vec3(x, y, z).normalize().scale(speed);
        projectile.setDeltaMovement(velocity);

        Vec3 movement = shooter.getKnownMovement();
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(movement.x, shooter.onGround() ? 0.0 : movement.y, movement.z));
    }

    private static boolean isAimUsable(Player player, ItemStack stack, InteractionHand hand) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.is(Items.BOW) || stack.is(Items.TRIDENT)) {
            return player.isUsingItem() && player.getUsedItemHand() == hand && player.getUseItem() == stack;
        }

        if (stack.is(Items.CROSSBOW)) {
            return net.minecraft.world.item.CrossbowItem.isCharged(stack);
        }

        return stack.is(Items.ENDER_PEARL)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.EXPERIENCE_BOTTLE)
                || stack.is(Items.FIRE_CHARGE)
                || stack.is(Items.WIND_CHARGE);
    }
}
