package dev.loki.lovisual.config.io;

import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.util.logging.DebugLog;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public enum ConfigSerializer {
    ;

    public static void loadAll() {
        try (ScanResult sc = new ClassGraph().enableClassInfo().acceptPackages("dev.loki.lovisual").scan()) {
            for (ClassInfo ci : sc.getClassesImplementing(ConfigObject.class)) {
                ConfigObject inst = singletonConfigObjectFor(ci.loadClass());
                if (inst != null) load(inst);
            }
        } catch (Exception e) {
            DebugLog.error("ConfigSerializer.loadAll failed", e);
        }
    }

    public static void saveAll() {
        try (ScanResult sc = new ClassGraph().enableClassInfo().acceptPackages("dev.loki.lovisual").scan()) {
            for (ClassInfo ci : sc.getClassesImplementing(ConfigObject.class)) {
                ConfigObject inst = singletonConfigObjectFor(ci.loadClass());
                if (inst != null) save(inst);
            }
        } catch (Exception e) {
            DebugLog.error("ConfigSerializer.saveAll failed", e);
        }
    }

    private static ConfigObject singletonConfigObjectFor(Class<?> cls) throws IllegalAccessException {
        if (cls == null) return null;
        for (Field field : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!ConfigObject.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof ConfigObject co) return co;
        }
        return null;
    }

    public static void save(ConfigObject obj) {
        if (obj instanceof HudGlobalConfig) return;
        ConfigSaveScheduler.saveNow(buildWrite(obj));
    }

    public static void requestSave(ConfigObject obj) {
        if (obj == null || obj instanceof HudGlobalConfig) return;
        ConfigSaveScheduler.PendingWrite write;
        try {
            write = buildWrite(obj);
        } catch (Exception e) {
            DebugLog.error("Failed to serialize config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
            return;
        }
        ConfigSaveScheduler.schedule(write);
    }

    public static void load(ConfigObject obj) {
        if (obj instanceof HudGlobalConfig) return;
        if (isModule(obj)) ConfigGsonSupport.loadModule(obj);
        else if (isJsonConfig(obj)) ConfigGsonSupport.loadJson(obj);
        else ConfigJson5Support.loadJson5(obj);
    }

    private static boolean isModule(ConfigObject obj) { return obj instanceof Module; }
    private static boolean isJsonConfig(ConfigObject obj) { return obj instanceof JsonConfigObject; }

    private static ConfigSaveScheduler.PendingWrite buildWrite(ConfigObject obj) {
        if (obj == null) return null;
        if (isModule(obj)) return ConfigGsonSupport.buildModuleWrite(obj);
        if (isJsonConfig(obj)) return ConfigGsonSupport.buildJsonWrite(obj);
        return ConfigJson5Support.buildJson5Write(obj);
    }
}
