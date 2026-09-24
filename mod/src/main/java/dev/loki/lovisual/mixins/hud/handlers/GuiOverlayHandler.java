package dev.loki.lovisual.mixins.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.pvp.PvpOverlayEvent;
import dev.loki.lovisual.features.gui.hud.draggable.impl.panels.Scoreboard;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.status.Potions;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.protect.MessageFilter;
import dev.loki.lovisual.features.module.modules.visuals.player.Crosshair;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.text.format.TextJsonUtil;

/**
 * Vanilla HUD element hide/replace toggles for {@link GuiMixin}: scoreboard, overlay messages,
 * potion effects, crosshair, vignette and the portal/sleep overlays. Each entry point cancels the
 * vanilla {@code extract*}/{@code display*} call when a module or custom HUD takes over.
 */
final class GuiOverlayHandler {
    private GuiOverlayHandler() {
    }

    static void hideScoreboardSidebar(CallbackInfo ci) {
        NoRender noRender = Modules.get(NoRender.class);
        if ((noRender != null && noRender.hideScoreboard()) || Scoreboard.shouldReplaceVanilla()) {
            ci.cancel();
        }
    }

    static void filterOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        if (DebugLog.serverOnly()) {
            if (message == null) {
                // DebugLog.server("HUD overlay message: tinted=%s raw=null", tinted);
            } else {
                String raw = message.getString();
                String json = TextJsonUtil.toJson(message);
                // DebugLog.server("HUD overlay message: tinted=%s raw=\"%s\" json=%s", tinted, raw, json);
            }
        }
        if (Events.BUS.hasListeners(PvpOverlayEvent.class)) {
            Events.BUS.post(new PvpOverlayEvent(message, tinted));
        }

        MessageFilter messageFilter = Modules.get(MessageFilter.class);
        if (messageFilter != null && messageFilter.shouldHideHud(message)) {
            ci.cancel();
        }
    }

    static void hideVanillaPotions(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (!DraggableHudElementRegistry.isEnabled(Potions.class)) return;
        if (mc.player.getActiveEffects().isEmpty()) return;
        ci.cancel();
    }

    static void replaceCrosshair(CallbackInfo ci) {
        Crosshair crosshair = Modules.get(Crosshair.class);
        if (crosshair != null && crosshair.shouldHideVanilla()) {
            ci.cancel();
        }
    }

    static void killVignette(CallbackInfo ci) {
        // Completely remove vanilla vignette post-processing
        ci.cancel();
    }

    static void disablePortalOverlay(CallbackInfo ci) {
        ci.cancel();
    }

    static void disableSleepOverlay(CallbackInfo ci) {
        ci.cancel();
    }
}
