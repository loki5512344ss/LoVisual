package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.Varargs;

import java.util.HashMap;
import java.util.Map;

/**
 * Один загруженный скрипт: скомпилированный чанк, свой {@code registry} и флаг «упал».
 * После ошибки скрипт не запускается, пока его не перезагрузят (правка файла или реинициализация).
 */
public final class ScriptSlot {

    private final String name;
    private final LuaSandbox sandbox;
    private final Map<String, LuaValue> store = new HashMap<>();
    private final LuaTable registry = buildRegistry();
    private LuaValue chunk;
    private String error;

    public ScriptSlot(String name, String source, LuaSandbox sandbox) {
        this.name = name;
        this.sandbox = sandbox;
        try {
            this.chunk = sandbox.globals().load(LuaPreprocessor.process(source), name);
        } catch (LuaError e) {
            this.error = e.getMessage();
        }
    }

    public String name() {
        return name;
    }

    public String error() {
        return error;
    }

    public boolean canRun() {
        return chunk != null && error == null;
    }

    /** Таблица {@code registry} для глобалов скрипта: getOrDefault/put. */
    public LuaTable registryTable() {
        return registry;
    }

    private LuaTable buildRegistry() {
        LuaTable t = new LuaTable();
        t.set("getOrDefault", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs a) {
                LuaValue v = store.get(a.arg(2).tojstring());
                return v != null ? v : a.arg(3);
            }
        });
        t.set("put", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs a) {
                store.put(a.arg(2).tojstring(), a.arg(3));
                return NONE;
            }
        });
        return t;
    }

    /** Запускает чанк; ошибка Lua блокирует скрипт и сохраняется в {@link #error()}. */
    public void run() {
        if (!canRun()) return;
        sandbox.beginRun();
        try {
            chunk.call();
        } catch (LuaError e) {
            error = e.getMessage();
        }
    }
}
