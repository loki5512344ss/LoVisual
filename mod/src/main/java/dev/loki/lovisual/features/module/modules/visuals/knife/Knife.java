package dev.loki.lovisual.features.module.modules.visuals.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeAnimator;
import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModel;
import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModels;
import dev.loki.lovisual.features.module.modules.visuals.knife.render.KnifeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.random.RandomGenerator;

/**
 * Replaces the sword in your hand with an animated knife skin: first person view model with draw, inspect,
 * attack and run animations and your own arms, a resting knife in the third person hand, and the skin's sounds.
 * Hooks: KnifeItemInHandMixin, KnifeHandLayerMixin, KnifeInputMixin.
 */
@ModuleInfo(
        id = "knife",
        displayName = "Knife",
        aliases = {"knife", "meleeskins"},
        category = ModuleCategory.ACCESSORIES
)
public final class Knife extends Module {
    private static final String ACTION_INSPECT = "inspect";
    private static final float MAX_DELTA_SECONDS = 0.25f;

    private final ModeValue skin = modeSetting("knifeSkin", "skin", defaultSkin(), KnifeSkin.KEYS);
    private final BooleanValue firstPerson = bool("knifeFirstPerson", "first_person", true);
    private final BooleanValue thirdPerson = bool("knifeThirdPerson", "third_person", true);
    private final BooleanValue sounds = bool("knifeSounds", "sounds", true);
    private final NumberValue<Float> volume = visibleWhen(num("knifeVolume", "volume", 0.8f, 0.0f, 1.0f), sounds::get);

    private final KnifeAnimator animator = new KnifeAnimator(KnifeSounds.INSTANCE, RandomGenerator.getDefault());
    private final KnifeRenderer renderer = new KnifeRenderer();
    private Item heldItem;
    private int heldSlot = -1;
    private long lastNanos;

    {
        action(ACTION_INSPECT, "R", BindMode.PRESS);
    }

    private static String defaultSkin() {
        return KnifeSkin.KEYS.length == 0 ? "" : java.util.Arrays.asList(KnifeSkin.KEYS).contains("karambit") ? "karambit" : KnifeSkin.KEYS[0];
    }

    boolean soundsEnabled() {
        return sounds.get();
    }

    float soundVolume() {
        return volume.get();
    }

    @Override
    public void onTick() {
        if (isActionPressedOnce(ACTION_INSPECT) && isEnabled()) animator.onInspect();
    }

    @Override
    public void onDisable() {
        animator.reset();
        heldItem = null;
        heldSlot = -1;
    }

    public static boolean isKnife(ItemStack stack) {
        return stack != null && stack.is(ItemTags.SWORDS);
    }

    /** Draws the view model for the main hand; false leaves the hand to vanilla. */
    public boolean renderFirstPerson(AbstractClientPlayer player, InteractionHand hand, ItemStack item, PoseStack stack,
                                     SubmitNodeCollector collector, int light, float equipProgress) {
        if (!isEnabled() || !firstPerson.get() || hand != InteractionHand.MAIN_HAND || !isKnife(item)) return false;
        KnifeSkin current = KnifeSkin.byKey(skin.get());
        KnifeModel model = current == null ? null : KnifeModels.get(current.model());
        if (model == null) return false;

        if (!animator.isBoundTo(current.key())) animator.bind(model, current.key(), current.variants());
        if (heldChanged(player)) animator.restart();
        animator.update(deltaSeconds(), new KnifeAnimator.Motion(player.isSprinting(), player.onGround(), player.moveDist));
        float retire = isKnife(player.getMainHandItem()) ? 0.0f : equipProgress;
        renderer.firstPerson(player, current, model, animator, stack, collector, light, retire);
        return true;
    }

    /** Draws the resting knife in the third person hand of your own player. */
    public boolean renderThirdPerson(ItemStack item, HumanoidArm arm, PoseStack stack, SubmitNodeCollector collector, int light) {
        if (!isEnabled() || !thirdPerson.get() || !isKnife(item)) return false;
        KnifeSkin current = KnifeSkin.byKey(skin.get());
        KnifeModel model = current == null ? null : KnifeModels.get(current.model());
        if (model == null) return false;
        renderer.thirdPerson(current, model, arm, stack, collector, light);
        return true;
    }

    /** A click with the main hand holding a knife; the secondary click plays the third attack. */
    public void onAttack(boolean primary) {
        Minecraft mc = Minecraft.getInstance();
        if (isEnabled() && firstPerson.get() && mc.player != null && isKnife(mc.player.getMainHandItem())) {
            animator.onAttack(primary);
        }
    }

    private boolean heldChanged(AbstractClientPlayer player) {
        Item item = player.getMainHandItem().getItem();
        int slot = player.getInventory().getSelectedSlot();
        boolean changed = item != heldItem || slot != heldSlot;
        heldItem = item;
        heldSlot = slot;
        return changed;
    }

    private float deltaSeconds() {
        long now = System.nanoTime();
        float delta = lastNanos == 0L ? 0.0f : (now - lastNanos) / 1.0E9f;
        lastNanos = now;
        return Minecraft.getInstance().isPaused() ? 0.0f : Math.max(0.0f, Math.min(delta, MAX_DELTA_SECONDS));
    }
}
