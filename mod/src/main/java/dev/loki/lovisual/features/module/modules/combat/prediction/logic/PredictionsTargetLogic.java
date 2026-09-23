package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.AimPrediction;
import dev.loki.lovisual.util.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PredictionsTargetLogic {
    private PredictionsTargetLogic() {}

    public static void updatePredictionTarget(Predictions facade, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!facade.getTargetHudOnAimValue().get()) {
            TargetManager.setPredictionTarget(null);
            return;
        }
        Player player = mc.player;
        if (player == null || mc.level == null) {
            TargetManager.setPredictionTarget(null);
            return;
        }
        AimPrediction pred = PredictionsProjectileLogic.computeAimPrediction(facade, player, tickDelta);
        if (pred != null && pred.entity() instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            TargetManager.setPredictionTarget(living);
            return;
        }
        TargetManager.setPredictionTarget(null);
    }
}
