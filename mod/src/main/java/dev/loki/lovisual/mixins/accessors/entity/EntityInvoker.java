package dev.loki.lovisual.mixins.accessors.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityInvoker {

    @Invoker("getInputVector")
    static Vec3 lovisual$movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        throw new AssertionError();
    }
}
