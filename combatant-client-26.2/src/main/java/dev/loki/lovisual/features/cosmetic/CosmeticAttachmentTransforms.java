package dev.loki.lovisual.features.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticDisplay;

public final class CosmeticAttachmentTransforms {
    private CosmeticAttachmentTransforms() {
    }

    /**
     * Base attachment transform for a slot, then the user's per-slot offset/rotation/scale
     * override from {@link CosmeticDisplay} (accessory slots only).
     */
    public static void apply(PoseStack matrices, CosmeticSlot slot, CosmeticModel model) {
        applyBase(matrices, slot);
        if (slot.isAccessory()) {
            applyUserOverride(matrices, slot);
        }
    }

    private static void applyUserOverride(PoseStack matrices, CosmeticSlot slot) {
        CosmeticDisplay module = Modules.get(CosmeticDisplay.class);
        if (module == null) return;
        CosmeticDisplay.SlotOffsetSettings offsets = module.offsetsFor(slot);
        if (offsets == null) return;

        matrices.translate(offsets.offsetX().get(), offsets.offsetY().get(), offsets.offsetZ().get());
        matrices.mulPose(Axis.YP.rotationDegrees(offsets.rotation().get()));
        float scale = offsets.scale().get();
        matrices.scale(scale, scale, scale);
    }

    private static void applyBase(PoseStack matrices, CosmeticSlot slot) {
        switch (slot) {
            case PET -> {
                // TODO: replace with CosmeticAnimator.getPetHoverOffset() once the animator is ported.
                float bob = 0f;
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
                matrices.translate(0.45f, 2.15f + bob, 0.12f);
                matrices.scale(0.55f, 0.55f, 0.55f);
            }
            case HEAD -> {
                matrices.translate(0f, 1.72f, 0f);
                matrices.scale(0.52f, 0.52f, 0.52f);
            }
            case SHOULDERS -> {
                matrices.translate(0f, 1.45f, 0f);
                matrices.scale(0.5f, 0.5f, 0.5f);
            }
            case BACK -> {
                matrices.translate(0f, 0.1f, -0.3f);
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
                matrices.scale(0.85f, 0.85f, 0.85f);
            }
            case WAIST -> {
                matrices.translate(0f, 0.55f, 0.25f);
                matrices.scale(0.55f, 0.55f, 0.55f);
            }
            case LEGS -> {
                matrices.translate(0f, 0.3f, 0f);
                matrices.scale(0.5f, 0.5f, 0.5f);
            }
            case HELD -> {
                matrices.translate(0f, 1.3f, 0f);
                matrices.scale(0.5f, 0.5f, 0.5f);
            }
            case MODEL -> {
                // no base transform: full-body model replaces the player 1:1
            }
        }
    }

    public static void applyPreview(PoseStack matrices, CosmeticModel model) {
        if (model == null) return;
        CosmeticSlot slot = model.getSlot();
        if (slot == CosmeticSlot.PET) {
            matrices.translate(6f, 22f, 1f);
            matrices.scale(1.8f, 1.8f, 1.8f);
        } else if (slot == CosmeticSlot.HEAD) {
            matrices.translate(0f, 20f, 0f);
        } else if (slot == CosmeticSlot.BACK) {
            matrices.translate(0f, 2f, -4f);
            matrices.scale(1.3f, 1.3f, 1.3f);
        } else if (slot == CosmeticSlot.WAIST) {
            matrices.translate(0f, 6f, 3f);
        }
    }
}
