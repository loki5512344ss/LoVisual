package dev.loki.lovisual.mixins.accessors.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Это новый Mixin для доступа к полю или методу, который управляет временем последней атаки
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    // Пример использования нового поля или метода, если оно называется ticksSinceLastAttack
    @Accessor("attackStrengthTicker")
    void setTicksSinceLastAttack(int ticks);

    @Accessor("noJumpDelay")
    void lovisual$setJumpingCooldown(int cooldown);
}

