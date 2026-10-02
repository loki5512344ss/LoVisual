package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext;

/**
 * Чистая математика аниматора частей модели: операции над вершиной (x,y,z в блоках, 0..1).
 * Углы в градусах, сдвиги и pivot приходят из Lua в пикселях (1/16 блока) и уже переведены вызывающим.
 */
public final class QuadOps {

    public enum Type { ROT_X, ROT_Y, ROT_Z, MOVE_X, MOVE_Y, MOVE_Z, SCALE }

    /** Операция над квадами с индексами [from, to]; a: градусы/блоки/множитель, p: pivot в блоках. */
    public record Op(Type type, int from, int to, float a, float px, float py, float pz) {
        public boolean covers(int quadIndex) {
            return quadIndex >= from && quadIndex <= to;
        }
    }

    private QuadOps() {
    }

    /** Применяет операцию к вершине in-place. */
    public static void apply(Op op, float[] v) {
        switch (op.type()) {
            case MOVE_X -> v[0] += op.a();
            case MOVE_Y -> v[1] += op.a();
            case MOVE_Z -> v[2] += op.a();
            case SCALE -> {
                v[0] = op.px() + (v[0] - op.px()) * op.a();
                v[1] = op.py() + (v[1] - op.py()) * op.a();
                v[2] = op.pz() + (v[2] - op.pz()) * op.a();
            }
            default -> rotate(op, v);
        }
    }

    private static void rotate(Op op, float[] v) {
        double rad = Math.toRadians(op.a());
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float x = v[0] - op.px();
        float y = v[1] - op.py();
        float z = v[2] - op.pz();
        float nx = x, ny = y, nz = z;
        switch (op.type()) {
            case ROT_X -> { ny = y * c - z * s; nz = y * s + z * c; }
            case ROT_Y -> { nx = x * c + z * s; nz = -x * s + z * c; }
            default -> { nx = x * c - y * s; ny = x * s + y * c; }
        }
        v[0] = nx + op.px();
        v[1] = ny + op.py();
        v[2] = nz + op.pz();
    }
}
