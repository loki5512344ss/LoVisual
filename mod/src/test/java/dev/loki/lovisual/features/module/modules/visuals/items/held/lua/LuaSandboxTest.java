package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.Easings;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.LuaPreprocessor;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.LuaSandbox;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.ScriptSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LuaSandboxTest {

    private static ScriptSlot slot(LuaSandbox sb, String src) {
        ScriptSlot s = new ScriptSlot("t", src, sb);
        sb.globals().set("registry", s.registryTable());
        return s;
    }

    @Test
    void preprocessorTurnsGlobalIntoRegistryLocal() {
        String out = LuaPreprocessor.process("global.x = 5;\nx = x + 1");
        assertTrue(out.contains("local x = registry:getOrDefault('x', 5)"));
        assertTrue(out.endsWith("registry:put('x', x)"));
    }

    @Test
    void registryPersistsBetweenRuns() {
        LuaSandbox sb = new LuaSandbox();
        ScriptSlot s = slot(sb, "global.n = 0\nn = n + 1\nresult = n");
        s.run();
        s.run();
        s.run();
        assertTrue(s.canRun());
        assertEquals(3, sb.globals().get("result").toint());
    }

    @Test
    void dangerousLibrariesAreAbsent() {
        LuaSandbox sb = new LuaSandbox();
        for (String name : new String[]{"io", "os", "luajava", "require", "dofile", "loadfile", "load", "debug", "package"}) {
            assertTrue(sb.globals().get(name).isnil(), name);
        }
    }

    @Test
    void infiniteLoopIsStopped() {
        LuaSandbox sb = new LuaSandbox();
        ScriptSlot s = slot(sb, "while true do end");
        s.run();
        assertFalse(s.canRun());
        assertTrue(s.error().contains("too slow"), s.error());
    }

    @Test
    void errorDisablesOnlyThatScript() {
        LuaSandbox sb = new LuaSandbox();
        ScriptSlot bad = slot(sb, "error('boom')");
        ScriptSlot good = slot(sb, "ok = 1");
        bad.run();
        good.run();
        assertFalse(bad.canRun());
        assertTrue(good.canRun());
    }

    @Test
    void syntaxErrorIsReportedNotThrown() {
        ScriptSlot s = slot(new LuaSandbox(), "if then");
        assertFalse(s.canRun());
        assertNotNull(s.error());
    }

    @Test
    void easingsHitEndpoints() {
        for (var e : Easings.all().entrySet()) {
            assertEquals(0.0, e.getValue().applyAsDouble(0.0), 1e-6, e.getKey());
            assertEquals(1.0, e.getValue().applyAsDouble(1.0), 1e-6, e.getKey());
        }
    }
}
