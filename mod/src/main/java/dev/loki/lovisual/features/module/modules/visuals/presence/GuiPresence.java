package dev.loki.lovisual.features.module.modules.visuals.presence;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.platform.presence.PresenceClient;
import dev.loki.lovisual.features.platform.presence.PresenceTimeline;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shows other LoVisual users' open ClickGUI as a small mirror held in front of them, and shares your own cursor the
 * same way. Needs a linked LoVisual account (device token) and a multiplayer server; data goes through the
 * chat-service presence endpoint and contains only the cursor position and a few menu flags.
 */
@ModuleInfo(
        id = "guipresence",
        displayName = "GuiPresence",
        category = ModuleCategory.VISUALS,
        description = "Shows the ClickGUI other LoVisual users have open, and shares yours"
)
public class GuiPresence extends Module {

    private static final float FADE_RANGE = 6.0f;
    private static final int MAX_WANT = 40;

    private final PresenceClient client = new PresenceClient();
    private final PresenceSampler sampler = new PresenceSampler();
    private final PresencePanelRenderer panel = new PresencePanelRenderer();
    private final BooleanValue share = bool("guiPresenceShare", "share", true);
    private final BooleanValue showOthers = bool("guiPresenceShowOthers", "show_others", true);
    private final NumberValue<Integer> distance = visibleWhen(num("guiPresenceDistance", "distance", 32, 8, 64), showOthers::get);
    private final NumberValue<Float> scale = visibleWhen(num("guiPresenceScale", "scale", 1.0f, 0.5f, 2.0f), showOthers::get);

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onDisable() {
        client.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();
        if (!isEnabled() || mc.level == null || mc.player == null || server == null) return;
        if (!share.get() && !showOthers.get()) return;
        long now = System.currentTimeMillis();
        boolean open = share.get() && sampler.sample(now);
        if (!client.due(now, open)) return;
        client.sync(now, server.ip, mc.player.getUUID(), open ? sampler.drain() : null, share.get(), wanted(mc));
    }

    private List<UUID> wanted(Minecraft mc) {
        List<UUID> want = new ArrayList<>();
        if (!showOthers.get()) return want;
        double max = distance.get() + FADE_RANGE;
        for (Player p : mc.level.players()) {
            if (p == mc.player || mc.player.distanceTo(p) > max) continue;
            want.add(p.getUUID());
            if (want.size() >= MAX_WANT) break;
        }
        return want;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || !showOthers.get() || mc.level == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, PresenceTimeline> e : client.remote().entrySet()) {
            Player p = mc.level.getPlayerByUUID(e.getKey());
            if (p == null || p == mc.player || !e.getValue().live(now)) continue;
            float d = mc.player.distanceTo(p);
            float fade = Math.min(1.0f, (distance.get() - d) / FADE_RANGE + 1.0f);
            panel.draw(renderer, p, tickDelta, e.getValue(), now, scale.get(), Math.max(0.0f, fade));
        }
    }
}
