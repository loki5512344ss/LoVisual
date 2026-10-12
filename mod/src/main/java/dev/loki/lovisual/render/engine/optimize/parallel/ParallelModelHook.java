package dev.loki.lovisual.render.engine.optimize.parallel;

import dev.loki.lovisual.mixins.optimize.entity.parallel.ModelFeatureRendererInvoker;
import dev.loki.lovisual.mixins.optimize.entity.parallel.RenderTypeFeatureRendererInvoker;
import dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.lwjgl.system.MemoryStack;

/**
 * Stage 3 step 8: drives one parallel batch per model group. 26.2 hands
 * models over in groups that are already ordered and batched, so a whole
 * group is one parallel run; groups below {@link ParallelModels#MIN_SUBMITS}
 * and submits that cannot go raw (block cracks, non-entity formats, frozen
 * animations) fall back to the vanilla path per model.
 */
public final class ParallelModelHook {

    private static final List<ParallelModels.Job> JOBS = new ArrayList<>();

    private ParallelModelHook() {
    }

    /** false: too few models in this group, vanilla builds it. */
    public static boolean buildGroup(ModelFeatureRenderer renderer, List<ModelFeatureRenderer.Submit<?>> submits) {
        if (!ParallelModels.enabled() || submits.size() < ParallelModels.MIN_SUBMITS) {
            return false;
        }
        JOBS.clear();
        ParallelModels.begin();
        try {
            for (ModelFeatureRenderer.Submit<?> s : submits) {
                // block cracks go through a decal generator: those few go the vanilla way
                JOBS.add(s.sheetedDecalPose() != null || s.renderType().format() != ParallelModels.FORMAT
                        || AnimFreeze.freezes(s.state(), s.pose().pose())
                        ? null
                        : ParallelModels.add(s.model(), s.state(), s.pose(), s.lightCoords(), s.overlayCoords(),
                                s.tintedColor(), s.sprite()));
            }
        } finally {
            ParallelModels.end();
        }
        RenderTypeFeatureRendererInvoker builders = (RenderTypeFeatureRendererInvoker) renderer;
        ModelFeatureRendererInvoker vanilla = (ModelFeatureRendererInvoker) renderer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int k = 0; k < submits.size(); k++) {
                ModelFeatureRenderer.Submit<?> s = submits.get(k);
                ParallelModels.Job job = JOBS.get(k);
                // one builder call per model, as vanilla does: it decides where a new draw starts
                if (job == null || !ParallelModels.emit(job, builders.lovisual$vertexBuilder(s.renderType()), stack)) {
                    vanilla.lovisual$prepareModel(s);
                }
            }
        }
        return true;
    }
}
