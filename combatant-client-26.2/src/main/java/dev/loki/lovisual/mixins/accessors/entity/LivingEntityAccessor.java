/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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

