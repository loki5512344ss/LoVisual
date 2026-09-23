package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsAim;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.AimPrediction;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.Kind;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsSimulation;
import dev.loki.lovisual.mixins.accessors.projectile.PersistentProjectileEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PredictionsProjectileLogic {
    private PredictionsProjectileLogic() {}

    public static TrajectoryResult simulateEntity(Predictions facade, Entity e, Kind kind, float tickDelta) {
        return PredictionsSimulation.simulateEntity(e, kind, tickDelta,
                facade.getMaxSimTicks(), facade.getPearlMaxSimTicks(),
                facade.getPearlColorToggle(), facade.getPearlColorValue());
    }

    public static AimPrediction computeAimPrediction(Predictions facade, Player player, float tickDelta) {
        return PredictionsAim.computeAimPrediction(player, tickDelta,
                facade.getMaxSimTicks(), facade.getPearlMaxSimTicks(),
                facade.getPearlColorToggle(), facade.getPearlColorValue());
    }

    public static List<TrajectoryResult> simulateProjectiles(Predictions facade, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return Collections.emptyList();
        List<TrajectoryResult> out = new ArrayList<>();
        List<Entity> candidates = new ArrayList<>();
        for (Entity e : world.entitiesForRendering()) candidates.add(e);
        candidates.sort((a, b) -> {
            double da = mc.player == null ? 0.0 : a.distanceToSqr(mc.player);
            double db = mc.player == null ? 0.0 : b.distanceToSqr(mc.player);
            return Double.compare(da, db);
        });
        int limit = Math.max(1, facade.getMaxProjectiles().get());
        int processed = 0;
        int pearlProcessed = 0;
        for (Entity e : candidates) {
            if (!e.isAlive() || !(e instanceof ThrownEnderpearl)) continue;
            if (pearlProcessed >= facade.getPearlMaxProjectiles().get()) break;
            TrajectoryResult res = simulateEntity(facade, e, Kind.PEARL_ENTITY, tickDelta);
            if (res != null) out.add(res);
            pearlProcessed++;
        }
        for (Entity e : candidates) {
            if (!e.isAlive() || e instanceof ThrownEnderpearl) continue;
            if (processed >= limit) break;
            switch (e) {
                case ThrownTrident trident -> {
                    if (isKindDisabled(facade, "trident") || trident.isNoPhysics() || isInGround(trident)) continue;
                    TrajectoryResult res = simulateEntity(facade, trident, Kind.TRIDENT, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case AbstractArrow arrow -> {
                    if (isKindDisabled(facade, "arrow") || arrow.isNoPhysics() || isInGround(arrow)) continue;
                    TrajectoryResult res = simulateEntity(facade, arrow, Kind.ARROW, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case Fireball fireball -> {
                    if (isKindDisabled(facade, "fireball")) continue;
                    TrajectoryResult res = simulateEntity(facade, fireball, Kind.FIREBALL, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case WindCharge windCharge -> {
                    if (isKindDisabled(facade, "wind_charge")) continue;
                    TrajectoryResult res = simulateEntity(facade, windCharge, Kind.WIND_CHARGE, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case FireworkRocketEntity firework -> {
                    if (isKindDisabled(facade, "firework")) continue;
                    TrajectoryResult res = simulateEntity(facade, firework, Kind.FIREWORK, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case Snowball snowball -> {
                    if (isKindDisabled(facade, "snowball")) continue;
                    TrajectoryResult res = simulateEntity(facade, snowball, Kind.SNOWBALL, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case ThrownEgg egg -> {
                    if (isKindDisabled(facade, "egg")) continue;
                    TrajectoryResult res = simulateEntity(facade, egg, Kind.EGG, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                case AbstractThrownPotion potion -> {
                    boolean lingering = potion.getItem().is(Items.LINGERING_POTION);
                    boolean splash = potion.getItem().is(Items.SPLASH_POTION) || !lingering;
                    if (lingering && isKindDisabled(facade, "potion_lingering")) continue;
                    if (splash && isKindDisabled(facade, "potion_splash")) continue;
                    Kind kind = lingering ? Kind.POTION_LINGERING : Kind.POTION_SPLASH;
                    TrajectoryResult res = simulateEntity(facade, potion, kind, tickDelta);
                    if (res != null) out.add(res);
                    processed++;
                }
                default -> {}
            }
        }
        return out;
    }

    private static boolean isKindDisabled(Predictions facade, String key) {
        return !facade.getKindToggles().get(key);
    }

    private static boolean isInGround(AbstractArrow arrow) {
        if (arrow == null) return false;
        try {
            return ((PersistentProjectileEntityAccessor) arrow).lovisual$isInGround();
        } catch (Throwable t) {
            return false;
        }
    }
}
