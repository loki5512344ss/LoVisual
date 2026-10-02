package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.events.impl.input.SprintControlEvent;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.combat.rotation.SprintController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Holds the sprint key for you while you move. Only the input is pressed, so vanilla
 * rules (hunger, sneaking, blindness) and the shared {@link SprintController} still decide.
 */
@ModuleInfo(
        id = "autosprint",
        displayName = "Auto Sprint",
        aliases = {"autosprint", "sprint"},
        category = ModuleCategory.PLAYER
)
public class AutoSprint extends Module {
    private final BooleanValue allDirections = bool("autoSprintAllDirections", "all_directions", false);
    private final BooleanValue whileUsing = bool("autoSprintWhileUsing", "while_using", false);

    @EventHandler
    private void onSprintControl(SprintControlEvent event) {
        if (event.getSource() != SprintControlEvent.Source.INPUT || event.shouldSprint()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!wantsSprint(event.getForward(), event.getSideways(), allDirections.get(),
                player.isShiftKeyDown(), player.isUsingItem(), whileUsing.get())) return;
        if (SprintController.INSTANCE.canStartSprinting(player)) event.setSprint(true);
    }

    /** Pure decision: does the held-sprint input apply for this movement? */
    static boolean wantsSprint(float forward, float sideways, boolean allDirections,
                               boolean sneaking, boolean usingItem, boolean whileUsing) {
        if (sneaking) return false;
        if (usingItem && !whileUsing) return false;
        return allDirections ? forward != 0.0f || sideways != 0.0f : forward > 0.0f;
    }
}
