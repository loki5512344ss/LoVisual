package dev.loki.lovisual.features.platform.serverpolicy;

import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Fabric side of the {@code lovisual:policy} channel: registers the payload,
 * asks the proxy for a policy on every join, applies incoming policies (force
 * disabling denied modules) and forgets everything on disconnect so
 * singleplayer has no policy.
 */
public final class ServerPolicyClient {

    private ServerPolicyClient() {
    }

    /** The one-field JSON payload travelling on {@code lovisual:policy}. */
    public record Payload(String json) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Payload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lovisual", "policy"));
        public static final StreamCodec<ByteBuf, Payload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Payload::json, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Called once from {@code LoVisual#onInitializeClient}. */
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        ClientPlayNetworking.registerReceiver(Payload.TYPE,
                (payload, context) -> onPolicy(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(new Payload(ServerPolicyCodec.REQUEST_JSON)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ServerPolicyState.clear());
    }

    private static void onPolicy(String json) {
        ServerPolicy policy = ServerPolicyCodec.parse(json);
        if (policy == null) {
            return;
        }
        ServerPolicyState.apply(policy);
        forceDisableDenied(policy);
    }

    /** A lock policy must stop modules that are already running. */
    private static void forceDisableDenied(ServerPolicy policy) {
        if (!policy.isLocked()) {
            return;
        }
        boolean stoppedAny = false;
        for (String id : policy.deny()) {
            Module module = ModuleManager.get(id);
            if (module != null && module.isEnabled()) {
                module.setEnabledInternal(false);
                stoppedAny = true;
            }
        }
        if (stoppedAny) {
            CommandOutput.warning(I18n.get("lovisual.policy.applied", policy.reason()));
        }
    }
}
