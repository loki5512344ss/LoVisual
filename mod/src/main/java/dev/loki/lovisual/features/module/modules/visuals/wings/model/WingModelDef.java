package dev.loki.lovisual.features.module.modules.visuals.wings.model;

import java.util.List;

/**
 * One entry of the wing model catalog. {@code animation} names an {@code .animation.json} next to the model, or
 * is one of the procedural ids ("angel", "demon", "dragon"). {@code scale}, {@code offsetY} and {@code offsetZ}
 * place the model on the back; {@code frames} is the number of texture frames stacked vertically.
 */
public record WingModelDef(String name, String geometry, String texture, String animation,
                           float scale, float offsetY, float offsetZ, int frames) {

    public static final List<WingModelDef> ALL = List.of(
            new WingModelDef("Baby Dragon", "dragon", "baby_dragon_white", "dragon", 1.0f, 0.45f, 0.0f, 1),
            new WingModelDef("Angel", "angel", "angel_white", "angel", 1.0f, 0.45f, 0.0f, 1),
            new WingModelDef("Demon Red", "demon", "demon_red", "demon", 1.0f, 0.45f, 0.0f, 1),
            new WingModelDef("Demon Black", "demon", "demon_black", "demon", 1.0f, 0.45f, 0.0f, 1),
            new WingModelDef("Pulse", "pulse", "pulse", "pulse", 1.0f, -1.2f, 0.0f, 1),
            new WingModelDef("Dark", "dark", "dark", "dark", 0.9f, -1.05f, 0.05f, 1),
            new WingModelDef("Ghoul", "ghoul", "ghoul", "ghoul", 0.6f, -0.65f, 0.0f, 1),
            new WingModelDef("Rocker", "rocker", "rocker", "rocker", 0.9f, -1.05f, 0.0f, 1),
            new WingModelDef("Wota", "wota", "wota", "wota", 0.9f, -1.05f, 0.0f, 10),
            new WingModelDef("Trident", "trident", "trident", "trident", 0.9f, -1.05f, 0.2f, 1),
            new WingModelDef("Easter", "easter", "easter", "easter", 0.9f, -1.05f, 0.0f, 1),
            new WingModelDef("Blue Dragon", "blue_dragon", "blue_dragon", "blue_dragon", 0.9f, -1.05f, 0.0f, 1),
            new WingModelDef("Spider", "spider", "spider", "spider", 1.0f, 0.0f, 0.0f, 1),
            new WingModelDef("Vulcano", "vulcano", "vulcano", "vulcano", 1.0f, 0.0f, 0.0f, 1));

    public static final String[] NAMES = ALL.stream().map(WingModelDef::name).toArray(String[]::new);

    public static WingModelDef byName(String name) {
        for (WingModelDef def : ALL) {
            if (def.name.equals(name)) return def;
        }
        return ALL.get(0);
    }

    public boolean procedural() {
        return animation.equals("angel") || animation.equals("demon") || animation.equals("dragon");
    }
}
