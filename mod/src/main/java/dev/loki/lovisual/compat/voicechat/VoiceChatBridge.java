package dev.loki.lovisual.compat.voicechat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Safe entry point to Simple Voice Chat data. Never touches the voice chat API itself, so
 * any class may use it whether or not the mod is installed; the plugin feeds it when present.
 */
public final class VoiceChatBridge {
    public static final String MOD_ID = "voicechat";

    private static final VoiceState STATE = new VoiceState(System::currentTimeMillis);
    private static volatile SelfProbe probe;

    private VoiceChatBridge() {
    }

    /** Live reads of the local microphone, implemented by the plugin on top of the voice chat API. */
    public interface SelfProbe {
        boolean muted();

        boolean disabled();

        boolean disconnected();

        boolean pushToTalk();
    }

    public static boolean isInstalled() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** Current state with the local microphone flags refreshed; cheap enough to call every frame. */
    public static VoiceState state() {
        SelfProbe p = probe;
        if (p != null) {
            STATE.setMuted(p.muted());
            STATE.setDisabled(p.disabled());
            STATE.setPushToTalk(p.pushToTalk());
            STATE.setConnected(!p.disconnected());
        }
        return STATE;
    }

    static VoiceState rawState() {
        return STATE;
    }

    static void attach(SelfProbe selfProbe) {
        probe = selfProbe;
    }
}
