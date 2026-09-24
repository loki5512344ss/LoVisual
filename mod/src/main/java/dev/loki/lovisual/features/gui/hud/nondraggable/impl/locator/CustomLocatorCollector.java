package dev.loki.lovisual.features.gui.hud.nondraggable.impl.locator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.locator.panel.CustomLocatorResolver;

public final class CustomLocatorCollector {

    private CustomLocatorCollector() {
    }

    public static List<CustomBarModel.LocatorCandidate> collect(Minecraft mc, CustomBar bm, TrackedWaypoint.Camera yawProvider,

                                                         PartialTickSupplier tickProgress, Set<UUID> mapPlayers,
                                                         ClientWaypointManager handler) {
        List<CustomBarModel.LocatorCandidate> candidates = new ArrayList<>();

        handler.forEachWaypoint(mc.player, waypoint -> {
            if (isSourcePlayer(mc.player, waypoint)) return;
            UUID wpUuid = waypoint.id().left().orElse(null);
            if (wpUuid != null && !mapPlayers.isEmpty() && mapPlayers.contains(wpUuid)) return;

            double yaw = waypoint.yawAngleToCamera(mc.level, yawProvider, tickProgress);
            if (yaw <= -CustomBarModel.LOCATOR_ANGLE_RANGE || yaw >= CustomBarModel.LOCATOR_ANGLE_RANGE) return;

            float dist = Mth.sqrt((float) waypoint.distanceSquared(mc.player));
            String name = CustomLocatorResolver.getWaypointName(mc, waypoint, wpUuid);
            String key = locatorLabelKey(waypoint, wpUuid);

            candidates.add(new CustomBarModel.LocatorCandidate(
                    waypoint,
                    wpUuid,
                    (float) yaw,
                    dist,
                    name,
                    key
            ));
        });

        return candidates;
    }

    public static boolean isSourcePlayer(LocalPlayer player, TrackedWaypoint waypoint) {
        if (player == null || waypoint == null) return false;
        return waypoint.id().left()
                .map(uuid -> uuid.equals(player.getUUID()))
                .orElse(false);
    }

    public static String locatorLabelKey(TrackedWaypoint waypoint, UUID uuid) {
        if (uuid != null) return uuid.toString();
        return waypoint.id().right().orElse(null);
    }

    public static final class Pass {

        public final GuiGraphicsExtractor ctx;
        public final Minecraft mc;
        public final CustomBar bm;
        public final PartialTickSupplier tickProgress;
        public final float barY;
        public final int startX;
        public final boolean drawArrows;
        public final boolean drawHeads;
        public final boolean labelsEnabled;
        public final CustomBarModel.LocatorLabelMode labelMode;
        public final float labelAngle;
        public final float dt;
        public final Set<String> seenLabels;
        public final List<CustomBarModel.LocatorLabel> labels;
        public final List<CustomBarModel.LocatorHead> heads;

        public Pass(GuiGraphicsExtractor ctx, Minecraft mc, CustomBar bm, PartialTickSupplier tickProgress,

             float barY, int startX, boolean drawArrows, boolean drawHeads,
             boolean labelsEnabled, CustomBarModel.LocatorLabelMode labelMode, float labelAngle, float dt,
             Set<String> seenLabels, List<CustomBarModel.LocatorLabel> labels, List<CustomBarModel.LocatorHead> heads) {
            this.ctx = ctx;
            this.mc = mc;
            this.bm = bm;
            this.tickProgress = tickProgress;
            this.barY = barY;
            this.startX = startX;
            this.drawArrows = drawArrows;
            this.drawHeads = drawHeads;
            this.labelsEnabled = labelsEnabled;
            this.labelMode = labelMode;
            this.labelAngle = labelAngle;
            this.dt = dt;
            this.seenLabels = seenLabels;
            this.labels = labels;
            this.heads = heads;
        }
    }
}
