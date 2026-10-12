package dev.loki.lovisual.render.engine.optimize.parallel;

import dev.loki.lovisual.mixins.optimize.entity.parallel.ModelPartAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

/**
 * The baked part tree of one model in render order — the static half of the
 * parallel model build: which parts exist, where each subtree ends and the
 * cube geometry folded onto distinct corners. Cached per model root in
 * {@link ParallelModels}' weak map, rebuilt only on resource reload.
 */
final class ModelGeometry {

    final ModelPart[] parts;
    /** end[i] is where the subtree of part i stops in the flat order. */
    final int[] end;
    final int[] vertices;
    final boolean[] empty;
    final Cube[][] cubes;

    ModelGeometry(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        add(root, list, ends);
        int n = list.size();
        parts = list.toArray(new ModelPart[0]);
        end = new int[n];
        vertices = new int[n];
        empty = new boolean[n];
        cubes = new Cube[n][];
        for (int i = 0; i < n; i++) {
            ModelPartAccessor accessor = (ModelPartAccessor) (Object) parts[i];
            end[i] = ends.get(i);
            List<ModelPart.Cube> source = accessor.lovisual$cubes();
            cubes[i] = new Cube[source.size()];
            for (int c = 0; c < cubes[i].length; c++) {
                cubes[i][c] = new Cube(source.get(c));
                vertices[i] += cubes[i][c].index.length;
            }
            empty[i] = cubes[i].length == 0 && accessor.lovisual$children().isEmpty();
        }
        // drop the strong ref from the value back to the weak-map key, or the
        // root model part would never be collected (leak fix)
        parts[0] = null;
    }

    Map<String, ModelPart> children(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).lovisual$children();
    }

    private void add(ModelPart part, List<ModelPart> list, List<Integer> ends) {
        int at = list.size();
        list.add(part);
        ends.add(0);
        for (ModelPart child : children(part).values()) {
            add(child, list, ends);
        }
        ends.set(at, list.size());
    }

    /** One cube in vanilla's polygon and vertex order, folded onto distinct corners. */
    static final class Cube {
        final float[] corners;
        final int[] index;
        final float[] uv;
        final float[] normals;
        final int[] sizes;

        Cube(ModelPart.Cube cube) {
            List<Float> corner = new ArrayList<>();
            List<Integer> idx = new ArrayList<>();
            List<Float> uvs = new ArrayList<>();
            normals = new float[cube.polygons.length * 3];
            sizes = new int[cube.polygons.length];
            for (int q = 0; q < cube.polygons.length; q++) {
                ModelPart.Polygon polygon = cube.polygons[q];
                normals[q * 3] = polygon.normal().x();
                normals[q * 3 + 1] = polygon.normal().y();
                normals[q * 3 + 2] = polygon.normal().z();
                sizes[q] = polygon.vertices().length;
                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    int found = -1;
                    for (int c = 0; c < corner.size(); c += 3) {
                        if (corner.get(c) == vertex.worldX()
                                && corner.get(c + 1) == vertex.worldY()
                                && corner.get(c + 2) == vertex.worldZ()) {
                            found = c / 3;
                            break;
                        }
                    }
                    if (found < 0) {
                        found = corner.size() / 3;
                        corner.add(vertex.worldX());
                        corner.add(vertex.worldY());
                        corner.add(vertex.worldZ());
                    }
                    idx.add(found);
                    uvs.add(vertex.u());
                    uvs.add(vertex.v());
                }
            }
            corners = new float[corner.size()];
            for (int c = 0; c < corners.length; c++) {
                corners[c] = corner.get(c);
            }
            index = idx.stream().mapToInt(Integer::intValue).toArray();
            uv = new float[uvs.size()];
            for (int c = 0; c < uv.length; c++) {
                uv[c] = uvs.get(c);
            }
        }
    }
}
