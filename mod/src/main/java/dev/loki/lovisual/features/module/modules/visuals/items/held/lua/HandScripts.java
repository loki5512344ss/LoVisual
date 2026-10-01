package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.Easings;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.EntityApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.LuaTables;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.PoseApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.LuaSandbox;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.ScriptSlot;
import net.fabricmc.loader.api.FabricLoader;
import dev.loki.lovisual.features.module.modules.visuals.items.held.HoldMyItems;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Скрипты руки: {@code hand_pose.lua} (поза всей руки) и {@code item_pose.lua} (поза предмета).
 * Источник: {@code config/lovisual/holdmyitems/<name>.lua} (горячая перезагрузка по mtime раз в
 * секунду), иначе встроенный пример из ресурсов мода. Ошибка скрипта пишется в лог один раз и
 * отключает только этот скрипт до правки файла.
 */
public final class HandScripts {

    public static final String HAND_POSE = "hand_pose";
    public static final String ITEM_POSE = "item_pose";

    private static final Logger LOG = LoggerFactory.getLogger(HandScripts.class);
    private static final long RECHECK_MS = 1000L;

    private final LuaSandbox sandbox = new LuaSandbox();
    private final PoseApi pose = new PoseApi();
    private final long[] stamps = {Long.MIN_VALUE, Long.MIN_VALUE};
    private final ScriptSlot[] slots = new ScriptSlot[2];
    private final float[][] lastState = new float[2][2];
    private long lastCheck;
    private Player player;
    private ItemStack stack = ItemStack.EMPTY;

    public HandScripts() {
        Globals g = sandbox.globals();
        g.set("M", pose.table());
        g.set("P", EntityApi.player(() -> player));
        g.set("I", EntityApi.item(() -> stack));
        LuaTable easings = new LuaTable();
        Easings.all().forEach((name, f) -> LuaTables.put(easings, name, a -> LuaValue.valueOf(f.applyAsDouble(LuaTables.num(a, 2, 0)))));
        g.set("Easings", easings);
    }

    /** Применяет hand_pose к матрице руки; true, если был pushPose и нужен popPose. */
    public static boolean applyHandPose(HoldMyItems module, AbstractClientPlayer player, InteractionHand hand,
                                        float swing, float equip, float tickDelta, ItemStack item, PoseStack ms) {
        if (!module.scriptsActive()) return false;
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        boolean rightArm = (player.getMainArm() == HumanoidArm.RIGHT) == mainHand;
        ms.pushPose();
        module.scripts().runHand(new HandFrame(player, item, mainHand, rightArm, swing, equip, tickDelta, ms));
        return true;
    }

    /** Применяет item_pose к матрице предмета; true, если был pushPose и нужен popPose. */
    public static boolean applyItemPose(HoldMyItems module, LivingEntity entity, ItemStack stack,
                                        ItemDisplayContext ctx, PoseStack ms) {
        if (!ctx.firstPerson() || !(entity instanceof AbstractClientPlayer player)) return false;
        if (!module.isEnabled() || !module.scriptsActive()) return false;
        boolean rightArm = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean mainHand = (player.getMainArm() == HumanoidArm.RIGHT) == rightArm;
        HandScripts scripts = module.scripts();
        float[] last = scripts.lastState(mainHand);
        ms.pushPose();
        scripts.runItem(new HandFrame(player, stack, mainHand, rightArm, last[0], last[1], 0.0f, ms));
        return true;
    }

    public static Path scriptDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lovisual").resolve("holdmyitems");
    }

    /** Принудительная перезагрузка на следующем кадре. */
    public void reload() {
        stamps[0] = Long.MIN_VALUE;
        stamps[1] = Long.MIN_VALUE;
        lastCheck = 0L;
    }

    public void runHand(HandFrame f) {
        run(0, HAND_POSE, f);
        lastState[f.mainHand() ? 0 : 1][0] = f.swingProgress();
        lastState[f.mainHand() ? 0 : 1][1] = f.equipProgress();
    }

    public void runItem(HandFrame f) {
        run(1, ITEM_POSE, f);
    }

    /** Последний известный swing/equip руки: item_pose рендерится вне submitArmWithItem. */
    public float[] lastState(boolean mainHand) {
        return lastState[mainHand ? 0 : 1];
    }

    private void run(int index, String name, HandFrame f) {
        refresh();
        ScriptSlot slot = slots[index];
        if (slot == null || !slot.canRun()) return;
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
        try {
            slot.run();
        } finally {
            pose.finish();
        }
        if (!slot.canRun()) LOG.warn("HoldMyItems script {} disabled: {}", name, slot.error());
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < RECHECK_MS) return;
        lastCheck = now;
        String[] names = {HAND_POSE, ITEM_POSE};
        for (int i = 0; i < names.length; i++) {
            Path file = scriptDir().resolve(names[i] + ".lua");
            long stamp = stampOf(file);
            if (stamp == stamps[i]) continue;
            stamps[i] = stamp;
            String source = read(file, names[i]);
            slots[i] = source == null ? null : new ScriptSlot(names[i], source, sandbox);
            if (slots[i] != null && !slots[i].canRun()) LOG.warn("HoldMyItems script {}: {}", names[i], slots[i].error());
        }
    }

    private static long stampOf(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    private static String read(Path file, String name) {
        try {
            if (Files.isRegularFile(file)) return Files.readString(file, StandardCharsets.UTF_8);
            try (InputStream in = HandScripts.class.getResourceAsStream("/assets/lovisual/holdmyitems/" + name + ".lua")) {
                return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOG.warn("Cannot read HoldMyItems script {}: {}", name, e.getMessage());
            return null;
        }
    }
}
