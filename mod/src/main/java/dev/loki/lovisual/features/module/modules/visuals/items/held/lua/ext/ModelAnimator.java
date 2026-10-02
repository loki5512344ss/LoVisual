package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.LuaTables;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.QuadOps.Op;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.QuadOps.Type;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua-глобал {@code animator} для item_model.lua: анимация частей (квадов) модели предмета.
 * {@code animator:rotateX(from, to, deg, px, py, pz)}, {@code moveX/Y/Z(from, to, px)}, {@code scale(from, to, k)}
 * по диапазону индексов квадов; pivot и сдвиги в пикселях. Операции действуют на один рендер предмета.
 */
public final class ModelAnimator {

    public static final ModelAnimator INSTANCE = new ModelAnimator();
    private static final float PX = 1.0f / 16.0f;

    private final List<Op> ops = new ArrayList<>();
    private int lastQuadCount;

    private ModelAnimator() {
    }

    public void clear() {
        ops.clear();
    }

    /** Число квадов последнего отрендеренного слоя (скрипту отдаётся глобалом quadCount). */
    public int lastQuadCount() {
        return lastQuadCount;
    }

    public LuaTable table() {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "rotateX", a -> rotate(Type.ROT_X, a));
        LuaTables.put(t, "rotateY", a -> rotate(Type.ROT_Y, a));
        LuaTables.put(t, "rotateZ", a -> rotate(Type.ROT_Z, a));
        LuaTables.put(t, "moveX", a -> move(Type.MOVE_X, a));
        LuaTables.put(t, "moveY", a -> move(Type.MOVE_Y, a));
        LuaTables.put(t, "moveZ", a -> move(Type.MOVE_Z, a));
        LuaTables.put(t, "scale", a -> {
            ops.add(new Op(Type.SCALE, (int) LuaTables.num(a, 2, 0), (int) LuaTables.num(a, 3, 0),
                    (float) LuaTables.num(a, 4, 1), (float) LuaTables.num(a, 5, 8) * PX,
                    (float) LuaTables.num(a, 6, 8) * PX, (float) LuaTables.num(a, 7, 8) * PX));
            return LuaValue.NIL;
        });
        return t;
    }

    private LuaValue rotate(Type type, Varargs a) {
        ops.add(new Op(type, (int) LuaTables.num(a, 2, 0), (int) LuaTables.num(a, 3, 0), (float) LuaTables.num(a, 4, 0),
                (float) LuaTables.num(a, 5, 8) * PX, (float) LuaTables.num(a, 6, 8) * PX, (float) LuaTables.num(a, 7, 8) * PX));
        return LuaValue.NIL;
    }

    private LuaValue move(Type type, Varargs a) {
        ops.add(new Op(type, (int) LuaTables.num(a, 2, 0), (int) LuaTables.num(a, 3, 0),
                (float) LuaTables.num(a, 4, 0) * PX, 0, 0, 0));
        return LuaValue.NIL;
    }

    /** Вызывается миксином перед отправкой квадов на рендер: возвращает изменённую копию либо исходный список. */
    public List<BakedQuad> apply(List<BakedQuad> quads) {
        lastQuadCount = quads.size();
        if (ops.isEmpty()) return quads;
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad q = quads.get(i);
            out.add(covered(i) ? transform(q, i) : q);
        }
        return out;
    }

    private boolean covered(int index) {
        for (Op op : ops) if (op.covers(index)) return true;
        return false;
    }

    private BakedQuad transform(BakedQuad q, int index) {
        Vector3f[] p = new Vector3f[4];
        for (int v = 0; v < 4; v++) {
            float[] pos = {q.position(v).x(), q.position(v).y(), q.position(v).z()};
            for (Op op : ops) if (op.covers(index)) QuadOps.apply(op, pos);
            p[v] = new Vector3f(pos[0], pos[1], pos[2]);
        }
        return new BakedQuad(p[0], p[1], p[2], p[3], q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                q.direction(), q.materialInfo());
    }
}
