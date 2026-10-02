package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.modules.visuals.items.held.HoldMyItems;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.Easings;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.EntityApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.LuaTables;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.PoseApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.HandParticles;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.ModelAnimator;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.VanillaApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.LuaSandbox;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.ScriptFiles;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.ScriptSlot;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Скрипты руки (см. {@link ScriptKind}): hand_pose, hand_relative_pose, item_pose, item_model и их аддоны.
 * Порядок за кадр руки: hand_pose, hand_relative_pose, (ваниль), item_pose, item_model. Источник и горячая
 * перезагрузка по mtime раз в секунду: {@link ScriptFiles}. Ошибка скрипта пишется в лог один раз и отключает
 * только этот скрипт до правки файла.
 */
public final class HandScripts {

    private static final Logger LOG = LoggerFactory.getLogger(HandScripts.class);
    private static final long RECHECK_MS = 1000L;

    private final LuaSandbox sandbox = new LuaSandbox();
    private final PoseApi pose = new PoseApi();
    private final VanillaApi vanilla = new VanillaApi();
    private final HandParticles particles = new HandParticles(sandbox, pose);
    private final ScriptKind[] kinds = ScriptKind.values();
    private final ScriptSlot[][] slots = new ScriptSlot[kinds.length][2];
    private final long[][] stamps = new long[kinds.length][2];
    private final float[][] lastState = new float[2][2];
    private long lastCheck;
    private Player player;
    private ItemStack stack = ItemStack.EMPTY;

    public HandScripts() {
        Globals g = sandbox.globals();
        g.set("M", pose.table());
        g.set("P", EntityApi.player(() -> player));
        g.set("I", EntityApi.item(() -> stack));
        g.set("V", vanilla.vanillaTable());
        g.set("C", vanilla.cameraTable());
        g.set("animator", ModelAnimator.INSTANCE.table());
        g.set("particleManager", particles.table());
        LuaTable easings = new LuaTable();
        Easings.all().forEach((name, f) -> LuaTables.put(easings, name, a -> LuaValue.valueOf(f.applyAsDouble(LuaTables.num(a, 2, 0)))));
        g.set("Easings", easings);
        reload();
    }

    public VanillaApi vanilla() {
        return vanilla;
    }

    public HandParticles particles() {
        return particles;
    }

    /** Принудительная перезагрузка всех скриптов на ближайшем кадре. */
    public final void reload() {
        for (long[] row : stamps) java.util.Arrays.fill(row, Long.MIN_VALUE);
        lastCheck = 0L;
        particles.clear();
    }

    /** Применяет hand_pose и hand_relative_pose к матрице руки; true, если был pushPose и нужен popPose. */
    public static boolean applyHandPose(HoldMyItems module, AbstractClientPlayer player, InteractionHand hand,
                                        float swing, float equip, float tickDelta, ItemStack item, PoseStack ms) {
        if (!module.scriptsActive()) return false;
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        boolean rightArm = (player.getMainArm() == HumanoidArm.RIGHT) == mainHand;
        ms.pushPose();
        HandScripts s = module.scripts();
        HandFrame f = new HandFrame(player, item, mainHand, rightArm, swing, equip, tickDelta, ms);
        s.vanilla.begin(mainHand);
        s.run(ScriptKind.HAND, f);
        s.run(ScriptKind.RELATIVE, f);
        s.lastState[mainHand ? 0 : 1][0] = swing;
        s.lastState[mainHand ? 0 : 1][1] = equip;
        return true;
    }

    /** Применяет item_pose и item_model; true, если был pushPose и нужен popPose. */
    public static boolean applyItemPose(HoldMyItems module, LivingEntity entity, ItemStack stack,
                                        ItemDisplayContext ctx, PoseStack ms) {
        if (!ctx.firstPerson() || !(entity instanceof AbstractClientPlayer player)) return false;
        if (!module.isEnabled() || !module.scriptsActive()) return false;
        boolean rightArm = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean mainHand = (player.getMainArm() == HumanoidArm.RIGHT) == rightArm;
        HandScripts s = module.scripts();
        float[] last = s.lastState[mainHand ? 0 : 1];
        HandFrame f = new HandFrame(player, stack, mainHand, rightArm, last[0], last[1], 0.0f, ms);
        ms.pushPose();
        s.run(ScriptKind.ITEM, f);
        ModelAnimator.INSTANCE.clear();
        s.sandbox.globals().set("quadCount", LuaValue.valueOf(ModelAnimator.INSTANCE.lastQuadCount()));
        s.run(ScriptKind.MODEL, f);
        return true;
    }

    private void run(ScriptKind kind, HandFrame f) {
        refresh();
        for (int part = 0; part < 2; part++) {
            ScriptSlot slot = slots[kind.ordinal()][part];
            if (slot == null || !slot.canRun()) continue;
            bind(slot, f);
            try {
                slot.run();
            } finally {
                pose.finish();
            }
            if (!slot.canRun()) LOG.warn("HoldMyItems script {} disabled: {}", kind.file(part), slot.error());
        }
    }

    private void bind(ScriptSlot slot, HandFrame f) {
        this.player = f.player();
        this.stack = f.stack();
        Globals g = sandbox.globals();
        g.set("registry", slot.registryTable());
        g.set("mainHand", LuaValue.valueOf(f.mainHand()));
        g.set("rightArm", LuaValue.valueOf(f.rightArm()));
        g.set("swingProgress", LuaValue.valueOf(f.swingProgress()));
        g.set("equipProgress", LuaValue.valueOf(f.equipProgress()));
        g.set("tickDelta", LuaValue.valueOf(f.tickDelta()));
        g.set("usingItem", LuaValue.valueOf(f.player().isUsingItem()));
        g.set("useDuration", LuaValue.valueOf(f.player().getTicksUsingItem()));
        g.set("time", LuaValue.valueOf(System.nanoTime() / 1.0e9));
        pose.bind(f.pose());
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < RECHECK_MS) return;
        lastCheck = now;
        for (ScriptKind kind : kinds) {
            for (int part = 0; part < 2; part++) {
                String name = kind.file(part);
                long stamp = ScriptFiles.stamp(name, part == 0);
                if (stamp == stamps[kind.ordinal()][part]) continue;
                stamps[kind.ordinal()][part] = stamp;
                String source = stamp < 0 ? null : ScriptFiles.read(name);
                ScriptSlot slot = source == null ? null : new ScriptSlot(name, source, sandbox);
                slots[kind.ordinal()][part] = slot;
                if (slot != null && !slot.canRun()) LOG.warn("HoldMyItems script {}: {}", name, slot.error());
            }
        }
    }
}
