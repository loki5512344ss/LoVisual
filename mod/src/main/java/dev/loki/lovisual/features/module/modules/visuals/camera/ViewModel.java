package dev.loki.lovisual.features.module.modules.visuals.camera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.camera.swing.SwingAnimator;

/**
 * ViewModel: mini items + custom swing animations
 */
@ModuleInfo(id = "viewmodel", displayName = "ViewModel", category = ModuleCategory.VISUALS)
public class ViewModel extends Module {

    private static final String SETTING_MINI_ALL = "mini_all_items";
    private static final String SETTING_MINI_ITEMS = "mini_items";
    private static final String SETTING_MINI_SCALE = "mini_scale";
    private static final String SETTING_SWING_ENABLED = "swing_enabled";
    private static final String SETTING_SWING_ALL = "swing_all_items";
    private static final String SETTING_SWING_ITEMS = "swing_items";
    private static final String SETTING_ROTATION_BYPASS_ITEMS = "rotation_bypass_items";
    private static final String SETTING_SWING_OFFSET_X = "swing_offset_x";
    private static final String SETTING_SWING_OFFSET_Y = "swing_offset_y";
    private static final String SETTING_SWING_OFFSET_Z = "swing_offset_z";
    private static final String SETTING_LIQUID_OFFSET_Z = "liquid_offset_z";
    private static final String SETTING_ANIM_MODE = "anim_mode";
    private static final String SETTING_ANIM_SPEED = "anim_speed";
    private static final String SETTING_ANIM_STRENGTH = "anim_strength";
    private static final String SETTING_ANIM_SPIN_ENABLED = "anim_spin_enabled";
    private static final String SETTING_ANIM_SPIN_SPEED = "anim_spin_speed";
    private static final String SETTING_ANIM_SPIN_AXIS = "anim_spin_axis";
    private static final String SETTING_EQUIP_LOWERING = "equip_lowering_factor";
    private static final String SETTING_SMOOTH_RUN_LOWERING = "smooth_run_lowering";
    private static final String SETTING_SMOOTH_RUN_LOWERING_AMOUNT = "smooth_run_lowering_amount";
    public final NumberValue<Float> liquidOffsetZ = num("liquid_offset_z", SETTING_LIQUID_OFFSET_Z, 0.0f, -2.0f, 2.0f);
    /* -----------------------------
     * MINI ITEMS
     * ----------------------------- */
    private final BooleanValue miniAll = bool("mini_all_items", SETTING_MINI_ALL, false);
    private final ItemIdSetValue miniItems =
            visibleWhen(itemList("mini_items", SETTING_MINI_ITEMS, TextListSetting.PickerMode.ITEMS), () -> !miniAll.get());
    private final NumberValue<Float> miniScale = num("mini_scale", SETTING_MINI_SCALE, 0.65f, 0.05f, 2.0f);
    /* -----------------------------
     * OFFSETS
     * ----------------------------- */
    private final BooleanValue swingEnabled = bool("swing_enabled", SETTING_SWING_ENABLED, true);
    public final NumberValue<Float> swingX = visibleWhen(num("swing_offset_x", SETTING_SWING_OFFSET_X, 0f, -2f, 2f), swingEnabled::get);
    public final NumberValue<Float> swingY = visibleWhen(num("swing_offset_y", SETTING_SWING_OFFSET_Y, 0f, -2f, 2f), swingEnabled::get);
    public final NumberValue<Float> swingZ = visibleWhen(num("swing_offset_z", SETTING_SWING_OFFSET_Z, 0f, -2f, 2f), swingEnabled::get);
    /* -----------------------------
     * ANIMATION SYSTEM
     * ----------------------------- */
    public final ModeValue animMode = visibleWhen(modeSetting(
            "anim_mode",
            SETTING_ANIM_MODE,
            "Smooth",
            "Back", "Smooth Down", "Block", "Smooth", "Swipe Back", "Block Down",
            "New", "7", "Forward", "Glide", "Tap", "Blocking", "Touch", "Slant",
            "Spin", "Helicopter", "Stab", "Circle", "Wave", "360 Swing",
            "SmoothVanilla", "SwipeBackDown", "Vanilla", "Flip", "Side", "Overhead",
            "Hammer", "Chop", "Arc", "Double", "Shake", "Shove", "Slash", "Thrust"), swingEnabled::get);
    public final NumberValue<Float> animSpeed = visibleWhen(num("anim_speed", SETTING_ANIM_SPEED, 1.0f, 0.1f, 4.0f), swingEnabled::get);
    public final NumberValue<Float> animStrength = visibleWhen(num("anim_strength", SETTING_ANIM_STRENGTH, 5.0f, 0.1f, 10.0f), swingEnabled::get);
    public final BooleanValue continuousSpin = visibleWhen(bool("anim_spin_enabled", SETTING_ANIM_SPIN_ENABLED, false), swingEnabled::get);
    public final NumberValue<Float> spinSpeed =
            visibleWhen(num("anim_spin_speed", SETTING_ANIM_SPIN_SPEED, 1.0f, 0.1f, 15.0f), () -> swingEnabled.get() && continuousSpin.get());
    public final ModeValue spinAxis =
            visibleWhen(modeSetting("anim_spin_axis", SETTING_ANIM_SPIN_AXIS, "Y", "X", "Y", "Z"), () -> swingEnabled.get() && continuousSpin.get());
    public final NumberValue<Float> equipLowering = visibleWhen(num("equip_lowering_factor", SETTING_EQUIP_LOWERING, 1.0f, 0.0f, 2.0f), swingEnabled::get);
    private final BooleanValue swingAll = visibleWhen(bool("swing_all_items", SETTING_SWING_ALL, false), swingEnabled::get);
    private final ItemIdSetValue swingItems =
            visibleWhen(itemList("swing_items", SETTING_SWING_ITEMS, TextListSetting.PickerMode.ITEMS),
                    () -> swingEnabled.get() && !swingAll.get());
    private final ItemIdSetValue rotationBypassItems =
            visibleWhen(itemList("rotation_bypass_items", SETTING_ROTATION_BYPASS_ITEMS, TextListSetting.PickerMode.ITEMS), swingEnabled::get);
    private final BooleanValue smoothRunLowering = bool("smooth_run_lowering", SETTING_SMOOTH_RUN_LOWERING, false);
    private final NumberValue<Float> smoothRunLoweringAmount =
            visibleWhen(num("smooth_run_lowering_amount", SETTING_SMOOTH_RUN_LOWERING_AMOUNT, 0.20f, 0.0f, 1.0f), smoothRunLowering::get);

    private float currentSpin = 0.0f;

    public boolean smoothRunLoweringEnabled() {
        return isEnabled() && smoothRunLowering.get();
    }

    public float getSmoothRunLoweringAmount() {
        return smoothRunLoweringAmount.get();
    }

    /* --------------------------------------------
     * Item checks
     * -------------------------------------------- */

    public boolean shouldScale(ItemStack st) {
        if (miniAll.get()) return true;
        if (st == null || st.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
        return miniItems.get().contains(id);
    }

    public boolean shouldSwing(ItemStack st) {
        if (!swingEnabled.get()) return false;
        if (swingAll.get()) return true;
        if (st == null || st.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
        return swingItems.get().contains(id);
    }

    public boolean shouldBypassRotationTransform(ItemStack st) {
        if (st == null || st.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
        return rotationBypassItems.get().contains(id);
    }

    /* --------------------------------------------
     * MINI SCALE
     * -------------------------------------------- */

    public void applyMini(PoseStack ms, ItemStack st) {
        if (!shouldScale(st)) return;
        float sc = miniScale.get();
        ms.scale(sc, sc, sc);
    }

    /* --------------------------------------------
     * SWING ANIMATIONS (custom baked)
     * -------------------------------------------- */

    public void applySwingAnimation(float swingProg, PoseStack ms, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();

        if (arm == HumanoidArm.LEFT) return;
        if (mc.player == null) return;

        ItemStack st = mc.player.getMainHandItem();
        if (!shouldSwing(st)) return;

        float speed = animSpeed.get();
        float progress = Mth.clamp(swingProg, 0.0f, 1.0f);
        float animProgress = speed >= 1.0f
                ? Mth.clamp(progress * speed, 0.0f, 1.0f)
                : progress; // keep full swing so slow speeds don't get cut at cooldown end
        float anim = Mth.sin(animProgress * (float) Math.PI);
        float sin1 = Mth.sin(animProgress * animProgress * (float) Math.PI);
        float sin2 = Mth.sin(Mth.sqrt(animProgress) * (float) Math.PI);
        float power = animStrength.get();

        // Apply user offsets
        ms.translate(swingX.get(), swingY.get(), swingZ.get());

        boolean applySpin = continuousSpin.get();
        if (applySpin) {
            currentSpin += spinSpeed.get() * 0.5f;
            if (currentSpin > 360.0f) currentSpin -= 360.0f;
        }

        SwingAnimator.apply(animMode.get(), ms, new SwingAnimator.Params(progress, anim, sin1, sin2, power));

        if (applySpin) {
            switch (spinAxis.get()) {
                case "X" -> ms.mulPose(Axis.XP.rotationDegrees(currentSpin));
                case "Z" -> ms.mulPose(Axis.ZP.rotationDegrees(currentSpin));
                default -> ms.mulPose(Axis.YP.rotationDegrees(currentSpin));
            }
        }
    }
}
