package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.function.Supplier;

/** Lua-глобалы {@code P} (игрок) и {@code I} (предмет текущей руки), только чтение. */
public final class EntityApi {

    private EntityApi() {
    }

    public static LuaTable player(Supplier<Player> p) {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "getSpeed", a -> LuaValue.valueOf(p.get().getDeltaMovement().horizontalDistance()));
        LuaTables.put(t, "getXSpeed", a -> LuaValue.valueOf(p.get().getDeltaMovement().x));
        LuaTables.put(t, "getYSpeed", a -> LuaValue.valueOf(p.get().getDeltaMovement().y));
        LuaTables.put(t, "getZSpeed", a -> LuaValue.valueOf(p.get().getDeltaMovement().z));
        LuaTables.put(t, "getYaw", a -> LuaValue.valueOf(p.get().getYRot()));
        LuaTables.put(t, "getPitch", a -> LuaValue.valueOf(p.get().getXRot()));
        LuaTables.put(t, "getAge", a -> LuaValue.valueOf(p.get().tickCount));
        LuaTables.put(t, "getHealth", a -> LuaValue.valueOf(p.get().getHealth()));
        LuaTables.put(t, "isOnGround", a -> LuaValue.valueOf(p.get().onGround()));
        LuaTables.put(t, "isSneaking", a -> LuaValue.valueOf(p.get().isShiftKeyDown()));
        LuaTables.put(t, "isSprinting", a -> LuaValue.valueOf(p.get().isSprinting()));
        LuaTables.put(t, "isSwimming", a -> LuaValue.valueOf(p.get().isSwimming()));
        LuaTables.put(t, "isUsingItem", a -> LuaValue.valueOf(p.get().isUsingItem()));
        LuaTables.put(t, "isTouchingWater", a -> LuaValue.valueOf(p.get().isInWater()));
        LuaTables.put(t, "isClimbing", a -> LuaValue.valueOf(p.get().onClimbable()));
        LuaTables.put(t, "hasVehicle", a -> LuaValue.valueOf(p.get().isPassenger()));
        return t;
    }

    public static LuaTable item(Supplier<ItemStack> s) {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "isEmpty", a -> LuaValue.valueOf(s.get().isEmpty()));
        LuaTables.put(t, "isOf", a -> LuaValue.valueOf(id(s.get()).equals(withNamespace(a.arg(2).tojstring()))));
        LuaTables.put(t, "getName", a -> LuaValue.valueOf(id(s.get())));
        LuaTables.put(t, "isBlock", a -> LuaValue.valueOf(s.get().getItem() instanceof net.minecraft.world.item.BlockItem));
        LuaTables.put(t, "isEnchanted", a -> LuaValue.valueOf(s.get().isEnchanted()));
        LuaTables.put(t, "isSword", a -> LuaValue.valueOf(s.get().is(ItemTags.SWORDS)));
        LuaTables.put(t, "getUseAction", a -> LuaValue.valueOf(useAction(s.get())));
        return t;
    }

    static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static String withNamespace(String name) {
        return name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    private static String useAction(ItemStack stack) {
        ItemUseAnimation anim = stack.getUseAnimation();
        return anim.name().toLowerCase(java.util.Locale.ROOT);
    }
}
