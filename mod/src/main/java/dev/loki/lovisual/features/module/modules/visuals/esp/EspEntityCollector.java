package dev.loki.lovisual.features.module.modules.visuals.esp;

import dev.loki.lovisual.features.relations.category.CategoryService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspModel;

public final class EspEntityCollector {

    private EspEntityCollector() {
    }

    static boolean containsEntityId(Set<String> selected, Identifier id) {
        String full = id.toString();
        String path = id.getPath();
        for (String raw : selected) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (full.equals(normalized) || path.equals(normalized)) {
                return true;
            }
            if (!normalized.contains(":") && full.equals("minecraft:" + normalized)) {
                return true;
            }
        }
        return false;
    }

    public static List<EspModel.RenderEntry> collectPlayerEntries(ESP esp, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        List<EspModel.RenderEntry> entries = new ArrayList<>();

        for (Player player : mc.level.players()) {
            if (!shouldRenderPlayer(esp, player)) continue;
            int color = resolvePlayerColor(player);
            double distSq = player.distanceToSqr(camPos);
            entries.add(new EspModel.RenderEntry(player, color, distSq));
        }

        return entries;
    }

    public static List<EspModel.RenderEntry> collectSearchEntityEntries(ESP esp, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        List<EspModel.RenderEntry> entries = new ArrayList<>();
        if (!esp.renderEntities.get()) return entries;

        int color = esp.entityColor.getArgb();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRenderEntity(esp, entity)) continue;
            double distSq = entity.distanceToSqr(camPos);
            entries.add(new EspModel.RenderEntry(entity, color, distSq));
        }

        return entries;
    }

    public static List<EspModel.RenderEntry> collectShaderEntries(ESP esp, Vec3 camPos) {
        List<EspModel.RenderEntry> entries = new ArrayList<>();
        double maxDistSq = ESP.SHADER_MAX_DISTANCE * ESP.SHADER_MAX_DISTANCE;

        for (EspModel.RenderEntry entry : collectPlayerEntries(esp, camPos)) {
            if (entry.distSq() > maxDistSq) {
                continue;
            }
            entries.add(entry);
        }

        for (EspModel.RenderEntry entry : collectShaderChamsEntityEntries(esp, camPos, maxDistSq)) {
            entries.add(entry);
        }

        entries.sort(java.util.Comparator.comparingDouble(EspModel.RenderEntry::distSq).reversed());
        return entries;
    }

    static List<EspModel.RenderEntry> collectShaderChamsEntityEntries(ESP esp, Vec3 camPos, double maxDistSq) {
        Minecraft mc = Minecraft.getInstance();
        List<EspModel.RenderEntry> entries = new ArrayList<>();
        Set<String> selected = esp.shaderChamsEntities.get();
        if (selected == null || selected.isEmpty()) {
            return entries;
        }

        int color = esp.shaderChamsEntityColor.getArgb();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRenderShaderChamsEntity(esp, entity, selected)) {
                continue;
            }
            double distSq = entity.distanceToSqr(camPos);
            if (distSq > maxDistSq) {
                continue;
            }
            entries.add(new EspModel.RenderEntry(entity, color, distSq));
        }
        return entries;
    }

    static boolean shouldRenderPlayer(ESP esp, Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (p == null) return false;
        if (p == mc.player) {
            if (mc.options.getCameraType().isFirstPerson()) return false;
            return esp.renderSelf.get();
        }
        if (esp.skipInvisible.get() && (p.isInvisible() || p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY))) {
            return false;
        }
        if (esp.onlyArmored.get() && !hasAnyArmor(p)) {
            return false;
        }
        if (esp.onlyVisible.get() && !hasLineOfSight(esp, p)) {
            return false;
        }
        return true;
    }

    static boolean hasAnyArmor(Player p) {
        return !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                || !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()
                || !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).isEmpty()
                || !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty();
    }

    static boolean hasLineOfSight(ESP esp, Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Vec3 start = mc.gameRenderer.mainCamera().position();
        Vec3 end = p.getBoundingBox().getCenter();
        HitResult result = mc.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return result.getType() == HitResult.Type.MISS;
    }

    static boolean shouldRenderEntity(ESP esp, Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (e == null) return false;
        if (e instanceof Player) return false;
        if (e == mc.player) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (id == null) return false;
        return esp.searchEntities.get().contains(id.toString());
    }

    static boolean shouldRenderShaderChamsEntity(ESP esp, Entity entity, Set<String> selected) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == null) return false;
        if (entity instanceof Player) return false;
        if (entity == mc.player) return false;

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return false;
        return containsEntityId(selected, id);
    }

    static int resolvePlayerColor(Player p) {
        return CategoryService.getColor(p);
    }

    public static int resolve3DColor(Entity entity, int fallbackColor) {
        if (entity instanceof Player player) {
            return CategoryService.getColor(player);
        }
        return fallbackColor;
    }
}
