package dev.loki.lovisual.mixininterface.device;

public interface IMsaaTexture {
    void lovisual$setSamples(int samples);

    int lovisual$getSamples();

    boolean lovisual$isMsaa();

    int lovisual$getGlId();
}
