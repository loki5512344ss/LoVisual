package dev.loki.lovisual.features.module.phase;

public interface ModuleStateListener {
    void onModuleStateChanged(String name, boolean enabled);
}
