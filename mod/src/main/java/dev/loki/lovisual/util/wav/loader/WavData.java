package dev.loki.lovisual.util.wav.loader;

import org.lwjgl.openal.AL10;

/**
 * Parsed WAV payload suitable for uploading into an OpenAL buffer.
 */
public record WavData(int channels, int sampleRate, byte[] data) {

    public int getFormat() {
        return channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
    }

    public int byteSize() {
        return data == null ? 0 : data.length;
    }
}
