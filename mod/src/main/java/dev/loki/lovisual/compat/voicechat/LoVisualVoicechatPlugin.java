package dev.loki.lovisual.compat.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;

/**
 * Simple Voice Chat plugin entrypoint ({@code "voicechat"} in fabric.mod.json). Only loaded
 * by the voice chat mod itself, so nothing here is ever resolved when it is not installed.
 */
public final class LoVisualVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "lovisual";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatClientApi client) {
            VoiceChatBridge.attach(new ClientProbe(client));
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        VoiceState state = VoiceChatBridge.rawState();
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class,
                e -> state.markSpeaking(e.getEntityId(), e.isWhispering()));
        registration.registerEvent(ClientSoundEvent.class, e -> state.markSelfSpeaking(e.isWhispering()));
        registration.registerEvent(MicrophoneMuteEvent.class, e -> state.setMuted(e.isDisabled()));
        registration.registerEvent(ClientVoicechatConnectionEvent.class, e -> state.setConnected(e.isConnected()));
    }

    private record ClientProbe(VoicechatClientApi api) implements VoiceChatBridge.SelfProbe {
        @Override
        public boolean muted() {
            return api.isMuted();
        }

        @Override
        public boolean disabled() {
            return api.isDisabled();
        }

        @Override
        public boolean disconnected() {
            return api.isDisconnected();
        }

        @Override
        public boolean pushToTalk() {
            return api.isPushToTalkKeyPressed();
        }
    }
}
