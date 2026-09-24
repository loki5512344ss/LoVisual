package dev.loki.lovisual.features.module.modules.visuals.nametags;

import dev.loki.lovisual.features.module.modules.visuals.nametags.parts.NameTagOpponentRenderer;
import dev.loki.lovisual.features.module.modules.visuals.nametags.render.NameTagLabelBuilder;
import dev.loki.lovisual.features.module.modules.visuals.nametags.render.NameTagRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectView;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Per-frame nametag collection for {@link NameTags}: walks nearby players, applies the
 * distance cutoffs and builds the {@code renderQueue} consumed by the foreground pass.
 * Lives in the {@code impl/} subfolder but the {@code nametags} package so it can read
 * the module's package-private caches and settings.
 */
final class NameTagsCollector {

    private static final int INFO_COLOR_DEFAULT = 0xFFFF8A8A;
    private static final float NAMEPLATE_EXTRA_LIFT = 8f;
    private static final ItemStack[] EMPTY_SLOTS = new ItemStack[0];

    private NameTagsCollector() {
    }

    static void collect(NameTags m, TextRenderer textRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.isEnabled() || mc.level == null || mc.player == null) return;

        NameTagRenderer.ensureFontCache(m, textRenderer);
        StatusEffectView.sync(mc);
        TotemPopCounter.tick();
        float dt = AnimationUtility.deltaTime();

        m.renderQueue.clear();
        m.playerBuffer.clear();
        m.seenIds.clear();

        boolean wantNameplate = m.toggles.get("Show nameplate");
        boolean wantSlots = m.toggles.get("Show armor row");
        boolean wantEffects = m.toggles.get("Show effects");
        boolean wantOpponent = m.opponentAppleCooldowns.get() && NameTagOpponentRenderer.isOpponentAppleTrackingEnabled();
        if (!wantNameplate && !wantSlots && !wantEffects && !wantOpponent) return;

        Cutoff cutoff = computeCutoff(m, wantNameplate, wantSlots, wantEffects, wantOpponent);
        Vec3 cameraPos = cameraPos(mc);
        gatherPlayers(m, mc, cameraPos);

        Flags want = new Flags(wantNameplate, wantSlots, wantEffects);
        for (Player player : m.playerBuffer) {
            NameTagModel.RenderEntry entry = buildEntry(m, player, cameraPos, textRenderer, dt, tickDelta, want, cutoff);
            if (entry != null) m.renderQueue.add(entry);
        }
        NameTagCache.pruneCaches(m);
    }

    private record Cutoff(double maxCutoff, boolean hasUnlimited) {
    }

    private record Flags(boolean nameplate, boolean slots, boolean effects) {
    }

    private record Equipment(ItemStack[] slots, List<List<NameTagModel.EnchantLine>> lines, int maxLines) {
    }

    private static Cutoff computeCutoff(NameTags m, boolean wantNameplate, boolean wantSlots, boolean wantEffects, boolean wantOpponent) {
        boolean hasUnlimited = false;
        double maxCutoff = 0.0;
        if (wantNameplate) {
            if (m.distanceToggles.get("nameplate")) maxCutoff = Math.max(maxCutoff, m.plateDistance.get());
            else hasUnlimited = true;
        }
        if (wantSlots) {
            if (m.distanceToggles.get("slots")) maxCutoff = Math.max(maxCutoff, m.slotsDistance.get());
            else hasUnlimited = true;
        }
        if (wantEffects) {
            if (m.distanceToggles.get("effects")) maxCutoff = Math.max(maxCutoff, m.effectsDistance.get());
            else hasUnlimited = true;
        }
        if (wantOpponent) {
            double oppCutoff = NameTags.OPPONENT_DISTANCE;
            if (oppCutoff > 0) maxCutoff = Math.max(maxCutoff, oppCutoff);
            else hasUnlimited = true;
        }
        return new Cutoff(maxCutoff, hasUnlimited);
    }

    private static Vec3 cameraPos(Minecraft mc) {
        if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            return mc.gameRenderer.mainCamera().position();
        }
        return mc.player != null ? mc.player.position() : null;
    }

    private static void gatherPlayers(NameTags m, Minecraft mc, Vec3 cameraPos) {
        for (Player player : mc.level.players()) {
            if (!m.shouldRender(player)) continue;
            m.playerBuffer.add(player);
            m.seenIds.add(player.getUUID());
        }
        if (cameraPos != null && m.playerBuffer.size() > 1) {
            double camX = cameraPos.x;
            double camY = cameraPos.y;
            double camZ = cameraPos.z;
            m.playerBuffer.sort(Comparator.comparingDouble((Player p) -> p.distanceToSqr(camX, camY, camZ)).reversed());
        }
    }

    private static NameTagModel.RenderEntry buildEntry(NameTags m, Player player, Vec3 cameraPos, TextRenderer textRenderer,
            float dt, float tickDelta, Flags want, Cutoff cutoff) {
        Vec3 pos = NameTagProjection.lerpedPos(player, tickDelta);
        double dist = (cameraPos == null) ? 0.0 : cameraPos.distanceTo(pos);
        if (!cutoff.hasUnlimited() && cutoff.maxCutoff() > 0.0 && dist > cutoff.maxCutoff()) return null;

        NameTagProjection.Anchor anchor = NameTagProjection.resolveAnchor(player, pos, tickDelta);
        if (anchor == null) return null;

        float distOffset = NameTagLayoutEngine.computeDistanceOffset(dist);
        int sectionLift = Math.round(NAMEPLATE_EXTRA_LIFT) + NameTagLayoutEngine.computeEspNameSectionLift(player);
        int nameY = NameTagLayoutEngine.computeNameY(anchor.rect(), anchor.anchorY(), distOffset) - sectionLift;
        Flags flags = applyDistanceCutoffs(m, want, dist);

        TotemPopSnapshot snapshot = TotemPopCounter.snapshot(player.getUUID());
        float totemPopAnimation = NameTagCache.updateTotemPopAnimation(m, player, snapshot, dt);
        int totemPopCount = NameTagCache.resolveTotemPopCount(m, player, snapshot, totemPopAnimation);

        NameTagModel.LabelInfo labelInfo = flags.nameplate() ? NameTagLabelBuilder.buildLabelInfo(m, player) : null;
        int infoColor = labelInfo != null ? labelInfo.infoColor() : INFO_COLOR_DEFAULT;
        NameTagModel.NameplateLayout nameplateLayout =
                buildLayout(m, player, textRenderer, labelInfo, flags, dist, anchor.centerX(), nameY, totemPopCount, totemPopAnimation);

        Equipment equipment = resolveEquipment(m, player, flags);
        List<MobEffectInstance> effects = flags.effects() ? NameTagCache.resolveEffects(m, player) : List.of();
        int itemsY = NameTagLayoutEngine.computeItemsY(anchor.rect(), nameY, distOffset) - sectionLift;

        return new NameTagModel.RenderEntry(player, dist, distOffset, anchor.centerX(), nameY, itemsY, anchor.rect(),
                flags.slots(), flags.nameplate(), flags.effects(), labelInfo, infoColor, nameplateLayout,
                equipment.slots(), equipment.lines(), equipment.maxLines(), effects, totemPopCount, totemPopAnimation);
    }

    private static Flags applyDistanceCutoffs(NameTags m, Flags want, double dist) {
        boolean renderSlots = want.slots();
        if (renderSlots && m.distanceToggles.get("slots") && dist > m.slotsDistance.get()) renderSlots = false;
        boolean renderNameplate = want.nameplate();
        if (renderNameplate && m.distanceToggles.get("nameplate") && dist > m.plateDistance.get()) renderNameplate = false;
        boolean renderEffects = want.effects();
        if (renderEffects && m.distanceToggles.get("effects") && dist > m.effectsDistance.get()) renderEffects = false;
        return new Flags(renderNameplate, renderSlots, renderEffects);
    }

    private static NameTagModel.NameplateLayout buildLayout(NameTags m, Player player, TextRenderer textRenderer,
            NameTagModel.LabelInfo labelInfo, Flags flags, double dist, int centerX, int nameY, int popCount, float popAnim) {
        if (!flags.nameplate() || labelInfo == null) return null;
        return NameTagLayoutEngine.buildLayout(m, player,
                m.cachedNameTr != null ? m.cachedNameTr : textRenderer,
                m.cachedHpTr != null ? m.cachedHpTr : textRenderer,
                labelInfo, dist, centerX, nameY, popCount, popAnim);
    }

    private static Equipment resolveEquipment(NameTags m, Player player, Flags flags) {
        if (!flags.slots()) return new Equipment(EMPTY_SLOTS, List.of(), 0);
        ItemStack[] slots = NameTagCache.getCachedEquipmentSlots(m, player);
        NameTagModel.EnchantCache cache = NameTagCache.resolveEnchantCache(m, player, slots);
        return new Equipment(slots, cache.lines, cache.maxLines);
    }
}
