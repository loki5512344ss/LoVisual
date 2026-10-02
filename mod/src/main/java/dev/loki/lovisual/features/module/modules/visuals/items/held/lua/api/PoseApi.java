package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * Lua-глобал {@code M}: преобразования матрицы руки/предмета (moveX/Y/Z, rotateX/Y/Z с pivot,
 * scale, push/pop) и математика. PoseStack подставляется на каждый вызов скрипта; лишние push
 * закрываются {@link #finish()}, лишние pop игнорируются.
 */
public final class PoseApi {

    private PoseStack stack;
    private int depth;

    public PoseApi bind(PoseStack stack) {
        this.stack = stack;
        this.depth = 0;
        return this;
    }

    /** Текущая матрица позы (копия) либо null, если скрипт не выполняется. */
    public org.joml.Matrix4f matrix() {
        return stack == null ? null : new org.joml.Matrix4f(stack.last().pose());
    }

    /** Закрывает push, оставленные скриптом незакрытыми. */
    public void finish() {
        while (stack != null && depth > 0) {
            stack.popPose();
            depth--;
        }
        stack = null;
    }

    private void rotate(Axis axis, double deg, double px, double py, double pz) {
        if (stack == null) return;
        stack.translate((float) px, (float) py, (float) pz);
        stack.mulPose(axis.rotationDegrees((float) deg));
        stack.translate((float) -px, (float) -py, (float) -pz);
    }

    public LuaTable table() {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "moveX", a -> move(LuaTables.num(a, 2, 0), 0, 0));
        LuaTables.put(t, "moveY", a -> move(0, LuaTables.num(a, 2, 0), 0));
        LuaTables.put(t, "moveZ", a -> move(0, 0, LuaTables.num(a, 2, 0)));
        LuaTables.put(t, "translate", a -> move(LuaTables.num(a, 2, 0), LuaTables.num(a, 3, 0), LuaTables.num(a, 4, 0)));
        LuaTables.put(t, "rotateX", a -> rot(Axis.XP, a));
        LuaTables.put(t, "rotateY", a -> rot(Axis.YP, a));
        LuaTables.put(t, "rotateZ", a -> rot(Axis.ZP, a));
        LuaTables.put(t, "scale", a -> {
            if (stack != null) {
                double x = LuaTables.num(a, 2, 1);
                stack.scale((float) x, (float) LuaTables.num(a, 3, x), (float) LuaTables.num(a, 4, x));
            }
            return LuaValue.NIL;
        });
        LuaTables.put(t, "push", a -> {
            if (stack != null) { stack.pushPose(); depth++; }
            return LuaValue.NIL;
        });
        LuaTables.put(t, "pop", a -> {
            if (stack != null && depth > 0) { stack.popPose(); depth--; }
            return LuaValue.NIL;
        });
        LuaTables.put(t, "sin", a -> LuaValue.valueOf(Math.sin(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "cos", a -> LuaValue.valueOf(Math.cos(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "abs", a -> LuaValue.valueOf(Math.abs(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "round", a -> LuaValue.valueOf((double) Math.round(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "floor", a -> LuaValue.valueOf(Math.floor(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "ceil", a -> LuaValue.valueOf(Math.ceil(LuaTables.num(a, 2, 0))));
        LuaTables.put(t, "pow", a -> LuaValue.valueOf(Math.pow(LuaTables.num(a, 2, 0), LuaTables.num(a, 3, 1))));
        LuaTables.put(t, "clamp", a -> LuaValue.valueOf(
                Math.max(LuaTables.num(a, 3, 0), Math.min(LuaTables.num(a, 4, 1), LuaTables.num(a, 2, 0)))));
        LuaTables.put(t, "lerp", a -> {
            double from = LuaTables.num(a, 2, 0);
            return LuaValue.valueOf(from + (LuaTables.num(a, 3, 0) - from) * LuaTables.num(a, 4, 0));
        });
        return t;
    }

    private LuaValue move(double x, double y, double z) {
        if (stack != null) stack.translate((float) x, (float) y, (float) z);
        return LuaValue.NIL;
    }

    private LuaValue rot(Axis axis, org.luaj.vm2.Varargs a) {
        rotate(axis, LuaTables.num(a, 2, 0), LuaTables.num(a, 3, 0), LuaTables.num(a, 4, 0), LuaTables.num(a, 5, 0));
        return LuaValue.NIL;
    }
}
