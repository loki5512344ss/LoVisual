package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

/** Виды скриптов HoldMyItems: основной файл и необязательный аддон, который выполняется сразу после него. */
public enum ScriptKind {
    HAND("hand_pose", "hand_addon"),
    RELATIVE("hand_relative_pose", "hand_relative_addon"),
    ITEM("item_pose", "item_addon"),
    MODEL("item_model", "item_model_addon");

    public final String main;
    public final String addon;

    ScriptKind(String main, String addon) {
        this.main = main;
        this.addon = addon;
    }

    public String file(int part) {
        return part == 0 ? main : addon;
    }
}
