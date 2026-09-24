package dev.loki.lovisual.mixins.accessors.blaze3d;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$ShaderSourceKey")
public interface ShaderSourceKeyAccessor {
    @Accessor("id")
    Identifier lovisual$getId();

    @Accessor("type")
    ShaderType lovisual$getType();
}
