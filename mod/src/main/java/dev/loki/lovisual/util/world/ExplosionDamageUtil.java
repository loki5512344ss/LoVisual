package dev.loki.lovisual.util.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.entity.simulation.PositionExtrapolation;
import dev.loki.lovisual.util.player.simulation.PlayerSimulationCache;
import dev.loki.lovisual.util.world.ExplosionExposure.ExposureDebug;

/**
 * Public entry point for crystal / TNT damage prediction. Simulates the vanilla
 * explosion pipeline (exposure sampling, distance decay, armor, resistance, protection)
 * against a target's bounding box, optionally extrapolated {@code predictedTicks} into
 * the future. Also exposes the intermediate values as a {@link DamageDebug} record for
 * overlays and diagnostics.
 */
public enum ExplosionDamageUtil {
    ;
    private static final float CRYSTAL_POWER = 6.0f;
    /** Convenience overload: end-crystal power, no ghost-obsidian override. */
    public static float getCrystalDamage(LivingEntity entity, Vec3 explosionPos, int predictedTicks, boolean ignoreTerrain) {
        return getExplosionDamage(entity, explosionPos, CRYSTAL_POWER, predictedTicks, ignoreTerrain);
    }
    /** Same as above but treats {@code ghostObsidianPos} as an obsidian block for ray-blocking purposes. */
    public static float getCrystalDamage(LivingEntity entity, Vec3 explosionPos, int predictedTicks, boolean ignoreTerrain, BlockPos ghostObsidianPos) {
        return getExplosionDamage(entity, explosionPos, CRYSTAL_POWER, predictedTicks, ignoreTerrain, ghostObsidianPos);
    }
    public static DamageDebug debugCrystalDamage(LivingEntity entity, Vec3 explosionPos, int predictedTicks, boolean ignoreTerrain) {
        return calculateExplosionDamage(entity, explosionPos, CRYSTAL_POWER, predictedTicks, ignoreTerrain, null);
    }
    public static DamageDebug debugCrystalDamage(LivingEntity entity, Vec3 explosionPos, int predictedTicks, boolean ignoreTerrain, BlockPos ghostObsidianPos) {
        return calculateExplosionDamage(entity, explosionPos, CRYSTAL_POWER, predictedTicks, ignoreTerrain, ghostObsidianPos);
    }
    public static float getExplosionDamage(LivingEntity entity, Vec3 explosionPos, float power, int predictedTicks, boolean ignoreTerrain) {
        return calculateExplosionDamage(entity, explosionPos, power, predictedTicks, ignoreTerrain, null).finalDamage();
    }
    public static float getExplosionDamage(LivingEntity entity, Vec3 explosionPos, float power, int predictedTicks, boolean ignoreTerrain, BlockPos ghostObsidianPos) {
        return calculateExplosionDamage(entity, explosionPos, power, predictedTicks, ignoreTerrain, ghostObsidianPos).finalDamage();
    }
    /** Full pipeline: null-guard → invulnerable → range box → distance → exposure → reductions. */
    private static DamageDebug calculateExplosionDamage(LivingEntity entity, Vec3 explosionPos, float power, int predictedTicks, boolean ignoreTerrain, BlockPos ghostObsidianPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || entity == null || explosionPos == null) return emptyDebug("null", null, 0.0);
        if (entity.isInvulnerable()) return emptyDebug("invulnerable", entity.getBoundingBox(), 0.0);
        AABB box = getPredictedBox(entity, predictedTicks);
        if (!intersectsExplosionRange(explosionPos, box, power)) {
            return emptyDebug("range_box", box, entity.distanceToSqr(explosionPos));
        }
        double explosionDiameter = power * 2.0f;
        double distanceToExplosionSq = getExposureDistanceSq(entity, box, explosionPos, predictedTicks);
        if (distanceToExplosionSq > explosionDiameter * explosionDiameter) {
            return new DamageDebug("distance", 0.0f, 1.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, distanceToExplosionSq, box, 0, 0, null, null, null, false, false);
        }
        ExposureDebug exposureDebug = ExplosionExposure.of(entity, explosionPos, box, ignoreTerrain, ghostObsidianPos);
        float exposure = exposureDebug.exposure();
        double distanceDecay = 1.0 - (Math.sqrt(distanceToExplosionSq) / explosionDiameter);
        double impact = exposure * distanceDecay;
        if (impact <= 0.0) {
            return new DamageDebug("impact", exposure, distanceDecay, impact, 0.0f, 0.0f, 0.0f, 0.0f, distanceToExplosionSq, box, exposureDebug.clearRays(), exposureDebug.totalRays(), exposureDebug.blockerSample(), exposureDebug.blockerPos(), exposureDebug.blockerBlock(), exposureDebug.blockerAtSampleCell(), exposureDebug.blockerAtExplosionCell());
        }
        float preDamage = (float) (((impact * impact + impact) / 2.0) * 7.0 * explosionDiameter + 1.0);
        float difficultyDamage = ExplosionReductions.applyDifficulty(preDamage);
        float armorDamage = ExplosionReductions.applyArmor(entity, difficultyDamage);
        float resistanceDamage = ExplosionReductions.applyResistance(entity, armorDamage);
        float finalDamage = Math.max(ExplosionReductions.applyProtection(entity, resistanceDamage), 0.0f);
        return new DamageDebug("ok", exposure, distanceDecay, impact, preDamage, armorDamage, resistanceDamage, finalDamage, distanceToExplosionSq, box, exposureDebug.clearRays(), exposureDebug.totalRays(), exposureDebug.blockerSample(), exposureDebug.blockerPos(), exposureDebug.blockerBlock(), exposureDebug.blockerAtSampleCell(), exposureDebug.blockerAtExplosionCell());
    }
    private static DamageDebug emptyDebug(String reason, AABB box, double distanceSq) {
        return new DamageDebug(reason, 0.0f, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, distanceSq, box, 0, 0, null, null, null, false, false);
    }
    /** Cheap AABB-vs-AABB rejection before doing any expensive sampling. */
    private static boolean intersectsExplosionRange(Vec3 explosionPos, AABB box, float power) {
        double maxDist = power * 2.0;
        return new AABB(
                Mth.floor(explosionPos.x - maxDist - 1.0), Mth.floor(explosionPos.y - maxDist - 1.0), Mth.floor(explosionPos.z - maxDist - 1.0),
                Mth.floor(explosionPos.x + maxDist + 1.0), Mth.floor(explosionPos.y + maxDist + 1.0), Mth.floor(explosionPos.z + maxDist + 1.0)
        ).intersects(box);
    }
    /** Squared distance from the (predicted) entity centre-of-mass to the explosion origin. */
    private static double getExposureDistanceSq(LivingEntity entity, AABB box, Vec3 explosionPos, int predictedTicks) {
        if (predictedTicks <= 0) return entity.distanceToSqr(explosionPos);
        return box.getCenter().add(0.0, -0.9, 0.0).distanceToSqr(explosionPos);
    }
    /** Predicted bounding box: local-player simulation cache first, generic extrapolation as fallback. */
    private static AABB getPredictedBox(LivingEntity entity, int predictedTicks) {
        if (entity == null) return AABB.unitCubeFromLowerCorner(Vec3.ZERO);
        if (predictedTicks <= 0) return entity.getBoundingBox();
        if (entity instanceof Player player) {
            PlayerSimulationCache.SimulatedPlayerCache simulation = resolveSimulation(player);
            if (simulation != null) {
                PlayerSimulationCache.SimulatedPlayerSnapshot snapshot = simulation.getSnapshotAt(predictedTicks);
                EntityDimensions dimensions = entity.getDimensions(entity.getPose());
                return dimensions.makeBoundingBox(snapshot.pos());
            }
        }
        return PositionExtrapolation.getBestForEntity(entity).getBoxInTicks(predictedTicks);
    }
    private static PlayerSimulationCache.SimulatedPlayerCache resolveSimulation(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && player == mc.player) return PlayerSimulationCache.getSimulationForLocalPlayer();
        return PlayerSimulationCache.getSimulationForOtherPlayers(player);
    }
    /** Full damage-pipeline breakdown for overlays; {@code reason} explains why {@code finalDamage} is 0. */
    public record DamageDebug(
            String reason, float exposure, double distanceFactor, double impact,
            float preDamage, float postArmorDamage, float postResistanceDamage, float finalDamage,
            double distanceSq, AABB box, int clearRays, int totalRays,
            Vec3 blockerSample, BlockPos blockerPos, String blockerBlock,
            boolean blockerAtSampleCell, boolean blockerAtExplosionCell
    ) {}
}
