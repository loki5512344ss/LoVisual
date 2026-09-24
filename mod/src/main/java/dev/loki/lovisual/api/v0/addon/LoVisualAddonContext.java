package dev.loki.lovisual.api.v0.addon;

import dev.loki.lovisual.events.meta.UsedImplicitly;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.api.v0.clickgui.LoVisualClickGuiSection;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.api.v0.module.LoVisualModuleExtension;
import dev.loki.lovisual.api.v0.render.callback.LoVisualPostProcessCallback;
import dev.loki.lovisual.api.v0.render.callback.LoVisualRenderCallback;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;

public interface LoVisualAddonContext extends LoVisualAddonRuntimeContext {
    @UsedImplicitly
    ModContainer modContainer();
    @UsedImplicitly
    Path gameDir();

    Path configDir();
    @UsedImplicitly
    Path addonConfigDir();
    @UsedImplicitly
    void registerModule(Module module);
    @UsedImplicitly
    void registerDraggableHudElement(DraggableHudElement element);
    @UsedImplicitly
    void registerStaticHudElement(AbstractHudElement element);
    @UsedImplicitly
    void registerCommand(ClientCommand command);
    @UsedImplicitly
    void registerClickGuiSection(String sectionId, String label, LoVisualClickGuiSection section);
    @UsedImplicitly
    void registerModuleExtension(String moduleId, LoVisualModuleExtension extension);
    @UsedImplicitly
    void registerIrisShaderPatchManifest(String manifestResourcePath);
    @UsedImplicitly
    void registerRenderCallback(String callbackId, LoVisualRenderStage stage, LoVisualRenderCallback callback);
    @UsedImplicitly
    void registerPostProcessPass(String passId, PostProcessPass.Phase phase, int priority, LoVisualPostProcessCallback callback);
    @UsedImplicitly
    RenderPipeline registerRenderPipeline(RenderPipeline pipeline);
}
