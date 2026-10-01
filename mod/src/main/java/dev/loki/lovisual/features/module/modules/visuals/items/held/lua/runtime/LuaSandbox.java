package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

/**
 * Песочница LuaJ: только base/bit32/table/string/math. Нет io, os, luajava, package, файловых
 * загрузчиков и debug; Java-объекты в Lua не отдаются вообще (API собирается из таблиц с функциями).
 * Бесконечные циклы обрезаются счётчиком инструкций через хук, который ставится один раз.
 */
public final class LuaSandbox {

    /** Шаг хука в инструкциях и допустимое число шагов на один запуск (≈ 400k инструкций). */
    private static final int HOOK_STEP = 1000;
    private static final int HOOK_LIMIT = 400;
    private static final String[] REMOVED = {"package", "require", "dofile", "loadfile", "load", "loadstring", "debug", "collectgarbage"};

    private final Globals globals;
    private int budget;

    public LuaSandbox() {
        Globals g = new Globals();
        g.load(new BaseLib());
        g.load(new PackageLib());
        g.load(new Bit32Lib());
        g.load(new TableLib());
        g.load(new StringLib());
        g.load(new JseMathLib());
        g.load(new DebugLib());
        LoadState.install(g);
        LuaC.install(g);
        LuaValue hook = new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (++budget > HOOK_LIMIT) throw new LuaError("script is too slow (instruction limit)");
                return NONE;
            }
        };
        g.get("debug").get("sethook").invoke(LuaValue.varargsOf(
                new LuaValue[]{hook, LuaValue.valueOf(""), LuaValue.valueOf(HOOK_STEP)}));
        for (String name : REMOVED) g.set(name, LuaValue.NIL);
        this.globals = g;
    }

    public Globals globals() {
        return globals;
    }

    /** Сбрасывает бюджет инструкций перед очередным запуском скрипта. */
    public void beginRun() {
        budget = 0;
    }
}
