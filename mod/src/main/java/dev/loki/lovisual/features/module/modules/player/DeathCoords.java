package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Reports the position of your latest death as a client-side chat message and
 * HUD notification, once per death.
 */
@ModuleInfo(
        id = "deathcoords",
        displayName = "Death Coords",
        aliases = {"deathcoords"},
        category = ModuleCategory.PLAYER
)
public class DeathCoords extends Module {
    private static final String SETTING_SHOW_DIMENSION = "show_dimension";

    private final BooleanValue showDimension =
            bool("deathCoordsShowDimension", SETTING_SHOW_DIMENSION, true);

    private final DeathDetector detector = new DeathDetector();

    @Override
    public void onDisable() {
        detector.reset();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            detector.reset();
            return;
        }

        if (!detector.onTick(mc.player.isDeadOrDying())) return;

        String message = formatDeathMessage(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.level.dimension().identifier().toString(),
                showDimension.get()
        );
        CommandOutput.warning(message);
        Notifier.warning(message);
    }

    /**
     * Pure alive-to-dead edge detector, unit-testable without Minecraft.
     */
    public static final class DeathDetector {
        private boolean wasDead;

        /**
         * @return true exactly on the tick the player transitions to dead
         */
        public boolean onTick(boolean dead) {
            boolean justDied = dead && !wasDead;
            wasDead = dead;
            return justDied;
        }

        public void reset() {
            wasDead = false;
        }
    }

    static String formatDeathMessage(double x, double y, double z, String dimension, boolean withDimension) {
        String coords = String.format(Locale.ROOT, "%.2f, %.2f, %.2f", x, y, z);
        if (withDimension && dimension != null && !dimension.isBlank()) {
            return "You died at " + coords + " (" + dimension + ")";
        }
        return "You died at " + coords;
    }
}
