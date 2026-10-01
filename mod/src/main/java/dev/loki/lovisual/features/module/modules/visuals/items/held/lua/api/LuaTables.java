package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

/** Сборка Lua-таблиц из Java-лямбд. Вызовы `T:name(x)` приходят с self в arg(1). */
public final class LuaTables {

    @FunctionalInterface
    public interface Fn {
        LuaValue call(Varargs args);
    }

    private LuaTables() {
    }

    public static void put(LuaTable table, String name, Fn fn) {
        table.set(name, new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return fn.call(args);
            }
        });
    }

    /** Число-аргумент по индексу (учитывая self в arg(1)); отсутствует = def. */
    public static double num(Varargs a, int i, double def) {
        return a.arg(i).isnil() ? def : a.arg(i).todouble();
    }
}
