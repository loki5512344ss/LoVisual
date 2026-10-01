package dev.loki.lovisual.features.module.modules.misc.voice;

import dev.loki.lovisual.compat.voicechat.VoiceChatBridge;
import dev.loki.lovisual.compat.voicechat.VoiceState;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Simple Voice Chat companion: pulsing indicators above players who are talking. */
@ModuleInfo(id = "voicechat", displayName = "VoiceChat", category = ModuleCategory.MISC)
public class VoiceChat extends Module {
    private static final float HEAD_OFFSET = 0.55f;
    private static final int MAX_SPEAKERS = 64;

    private final NumberValue<Integer> holdMs = num("voiceChatHoldMs", "hold_ms", 350, 100, 1500);
    private final NumberValue<Float> maxDistance = num("voiceChatMaxDistance", "max_distance", 32.0f, 8.0f, 96.0f);
    private final NumberValue<Float> indicatorSize = num("voiceChatIndicatorSize", "indicator_size", 1.0f, 0.5f, 2.5f);
    private final BooleanValue pulse = bool("voiceChatPulse", "pulse", true);
    private final BooleanValue showSelf = bool("voiceChatShowSelf", "show_self", true);
    private final BooleanValue showMuted = visibleWhen(bool("voiceChatShowMuted", "show_muted", true), showSelf::get);
    private final RGBAColorValue color = color("voiceChatColor", "color", "#FF55FF77");
    private final RGBAColorValue whisperColor = color("voiceChatWhisperColor", "whisper_color", "#FFFFD255");
    private final RGBAColorValue selfColor =
            visibleWhen(color("voiceChatSelfColor", "self_color", "#FF55AAFF"), showSelf::get);
    private final RGBAColorValue mutedColor = visibleWhen(
            color("voiceChatMutedColor", "muted_color", "#FFFF5555"), () -> showSelf.get() && showMuted.get());

    @Override
    protected String getUnavailableReason() {
        return VoiceChatBridge.isInstalled() ? "" : "Simple Voice Chat is not installed";
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null || !VoiceChatBridge.isInstalled()) return;

        MeshBuilder rings = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE, Renderer3D.DepthMode.MAIN);
        MeshBuilder halo = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.BLOOM, Renderer3D.DepthMode.MAIN);
        if (rings == null || halo == null) return;

        VoiceState state = VoiceChatBridge.state();
        long hold = holdMs.get();
        double time = (GameClock.millis() % 100000L) / 1000.0;
        float size = indicatorSize.get();
        double maxSq = (double) maxDistance.get() * maxDistance.get();

        for (VoiceState.Speaker speaker : state.speakers(hold, MAX_SPEAKERS)) {
            Player player = mc.level.getPlayerByUUID(speaker.id());
            if (player == null || player == mc.player) continue;
            Vec3 head = headPosition(player, tickDelta);
            if (head.distanceToSqr(mc.player.position()) > maxSq) continue;
            int argb = speaker.whisper() ? whisperColor.getArgb() : color.getArgb();
            VoiceIndicatorRenderer.speaking(rings, halo, head, size, state.level(speaker.id(), hold), argb, time, pulse.get());
        }

        if (showSelf.get() && !mc.options.getCameraType().isFirstPerson()) {
            renderSelf(mc.player, state, rings, halo, hold, time, size, tickDelta);
        }
    }

    private void renderSelf(Player self, VoiceState state, MeshBuilder rings, MeshBuilder halo,
                            long hold, double time, float size, float tickDelta) {
        Vec3 head = headPosition(self, tickDelta);
        float level = state.selfLevel(hold);
        if (level > 0f) {
            int argb = state.isSelfWhispering(hold) ? whisperColor.getArgb() : selfColor.getArgb();
            VoiceIndicatorRenderer.speaking(rings, halo, head, size, level, argb, time, pulse.get());
        }
        VoiceState.MicStatus mic = state.micStatus(hold);
        boolean silenced = mic == VoiceState.MicStatus.MUTED || mic == VoiceState.MicStatus.DISABLED;
        if (showMuted.get() && silenced) {
            VoiceIndicatorRenderer.muted(rings, head, size * 0.45f, mutedColor.getArgb());
        }
    }

    private static Vec3 headPosition(Player player, float tickDelta) {
        Vec3 pos = RenderMath.getLerpedPos(player, tickDelta);
        return new Vec3(pos.x, pos.y + player.getBbHeight() + HEAD_OFFSET, pos.z);
    }
}
