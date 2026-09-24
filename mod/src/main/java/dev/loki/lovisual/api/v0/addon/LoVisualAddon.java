package dev.loki.lovisual.api.v0.addon;

/**
 * Fabric entrypoint contract for LoVisual addons.
 *
 * <p>Declare it in fabric.mod.json under the {@value #ENTRYPOINT_KEY} entrypoint.
 */
public interface LoVisualAddon {
    String ENTRYPOINT_KEY = "lovisual:addon";
    int API_VERSION = 0;

    /**
     * Called before LoVisual auto-loads its built-in modules.
     *
     * <p>Use this hook only for startup module exclusions. Normal addon registration
     * still belongs in {@link #onInitialize(LoVisualAddonContext)}.</p>
     */
    default void onConfigureModules(LoVisualModuleLoadContext context) {
    }

    void onInitialize(LoVisualAddonContext context);

    /**
     * Called after all built-in and addon module configs have been loaded and applied.
     * This is the safe startup hook for mutating settings of existing modules.
     */
    default void onClientReady(LoVisualAddonRuntimeContext context) {
    }

    default void onRuntimeSuspended(LoVisualAddonRuntimeContext context) {
    }

    default void onRuntimeResumed(LoVisualAddonRuntimeContext context) {
    }

    default void onShutdown(LoVisualAddonRuntimeContext context) {
    }
}
