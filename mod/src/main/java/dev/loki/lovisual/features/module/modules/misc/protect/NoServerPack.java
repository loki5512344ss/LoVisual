/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.protect;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Blocks forced server resource packs.
 *
 * <p>Decline/Remove answer every push with a terminal
 * {@code ServerboundResourcePackPacket.Action.DECLINED} before vanilla shows
 * its prompt or starts a download; Remove additionally drops packs that are
 * already applied.</p>
 */
@ModuleInfo(
        id = "noserverpack",
        displayName = "No Server Pack",
        aliases = {"noserverpack", "nopack"},
        category = ModuleCategory.MISC
)
public class NoServerPack extends Module {
    private static final String SETTING_ACTION = "action";

    private final ModeValue action =
            mode("noServerPackAction", SETTING_ACTION, Action.DECLINE.id, Action.REMOVE.id, Action.ASK.id);

    private boolean clearedAppliedPacks;

    public Action action() {
        return Action.byId(action.get());
    }

    /**
     * Packet-hook decision. Never touches the network or GUI from here.
     *
     * @return true when the push must be cancelled and answered with DECLINED
     */
    public boolean shouldDeclinePush() {
        return shouldDeclinePush(isEnabled(), action.get());
    }

    /** Called from the packet hook after a push was answered with DECLINED. */
    public void onPackDeclined(UUID packId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || packId == null) return;
        mc.execute(() -> Notifier.info("Server resource pack declined (" + packId + ")"));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            clearedAppliedPacks = false;
            return;
        }
        if (action() == Action.REMOVE) {
            clearAppliedPacksOnce();
        } else {
            clearedAppliedPacks = false;
        }
    }

    private void clearAppliedPacksOnce() {
        if (clearedAppliedPacks) return;
        clearedAppliedPacks = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getDownloadedPackSource() == null) return;
        mc.getDownloadedPackSource().popAll();
        Notifier.info("Applied server packs removed");
    }

    static boolean shouldDeclinePush(boolean enabled, String mode) {
        return enabled && Action.byId(mode) != Action.ASK;
    }

    public enum Action {
        DECLINE("Decline"),
        REMOVE("Remove"),
        ASK("Ask");

        public final String id;

        Action(String id) {
            this.id = id;
        }

        static Action byId(String id) {
            for (Action value : values()) {
                if (value.id.equals(id)) return value;
            }
            return DECLINE;
        }
    }
}
