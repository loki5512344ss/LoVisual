package dev.loki.lovisual.features.module.modules.visuals.knife.anim.types;

import java.util.List;

/** A look of one skin: clips carry {@code suffix} and the {@code hidden} bone branches are not drawn. */
public record KnifeVariant(String suffix, List<String> hidden) {
    public static final KnifeVariant DEFAULT = new KnifeVariant("", List.of());
}
