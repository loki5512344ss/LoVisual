package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.LuaTables;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * Lua-глобалы {@code V} и {@code C}.
 * V: {@code V:cancelEquip(true)} отключает ванильный подъём предмета, {@code V:cancelSwing(true)} ванильный свинг
 * (скрипт сам рисует их через hand_pose); флаги читаются миксином на следующем кадре.
 * C: {@code C:setCamRot(yaw, pitch)} добавка к повороту камеры в градусах, {@code C:setCamPos(surge, heave, sway)}
 * сдвиг камеры в блоках; действует, пока скрипт обновляет значения (до 150 мс без вызова).
 */
public final class VanillaApi {

    private static final long CAM_TTL_MS = 150L;

    private final boolean[] cancelEquip = new boolean[2];
    private final boolean[] cancelSwing = new boolean[2];
    private final float[] cam = new float[5];
    private long camStamp;
    private int current;

    /** Начало кадра руки: флаги этой руки сбрасываются и выставляются скриптом заново. */
    public void begin(boolean mainHand) {
        current = mainHand ? 0 : 1;
        cancelEquip[current] = false;
        cancelSwing[current] = false;
    }

    public boolean cancelEquip(boolean mainHand) {
        return cancelEquip[mainHand ? 0 : 1];
    }

    public boolean cancelSwing(boolean mainHand) {
        return cancelSwing[mainHand ? 0 : 1];
    }

    /** Добавка камеры {yaw, pitch, surge, heave, sway}, либо null, если скрипт её не задаёт. */
    public float[] cameraOffset() {
        return System.currentTimeMillis() - camStamp > CAM_TTL_MS ? null : cam;
    }

    public LuaTable vanillaTable() {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "cancelEquip", a -> flag(cancelEquip, a.arg(2).toboolean()));
        LuaTables.put(t, "cancelSwing", a -> flag(cancelSwing, a.arg(2).toboolean()));
        return t;
    }

    public LuaTable cameraTable() {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "setCamRot", a -> cam(0, a.arg(2).optdouble(0), a.arg(3).optdouble(0)));
        LuaTables.put(t, "setCamPos", a -> {
            cam(2, a.arg(2).optdouble(0), a.arg(3).optdouble(0));
            cam[4] = (float) a.arg(4).optdouble(0);
            return LuaValue.NIL;
        });
        return t;
    }

    private LuaValue flag(boolean[] array, boolean value) {
        array[current] = value;
        return LuaValue.NIL;
    }

    private LuaValue cam(int at, double first, double second) {
        cam[at] = (float) first;
        cam[at + 1] = (float) second;
        camStamp = System.currentTimeMillis();
        return LuaValue.NIL;
    }
}
