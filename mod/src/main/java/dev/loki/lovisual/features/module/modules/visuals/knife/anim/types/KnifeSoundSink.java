package dev.loki.lovisual.features.module.modules.visuals.knife.anim.types;

/** Where animation sound events go; handles let a track silence what it started when its clip is replaced. */
public interface KnifeSoundSink {
    KnifeSoundSink SILENT = new KnifeSoundSink() {
        @Override
        public Object play(String effect) {
            return null;
        }

        @Override
        public boolean active(Object handle) {
            return false;
        }

        @Override
        public void stop(Object handle) {
        }
    };

    /** Starts the sound event and returns a handle, or null when nothing plays. */
    Object play(String effect);

    boolean active(Object handle);

    void stop(Object handle);
}
