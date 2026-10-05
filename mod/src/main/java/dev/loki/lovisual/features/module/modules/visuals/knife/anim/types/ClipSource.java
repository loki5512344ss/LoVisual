package dev.loki.lovisual.features.module.modules.visuals.knife.anim.types;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeClip;

/** What the animator needs from a model: its clips by name and its bone count. */
public interface ClipSource {
    KnifeClip clip(String name);

    int boneCount();

    default boolean hasClip(String name) {
        return clip(name) != null;
    }

    /** Marks the named bones and everything below them in {@code flags}. */
    void markBranches(java.util.List<String> names, boolean[] flags);
}
