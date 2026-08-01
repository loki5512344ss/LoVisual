/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.block;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.BedwarsESP;
import dev.loki.lovisual.features.module.modules.visuals.BlockESP;
import dev.loki.lovisual.util.block.bed.BedBlockUtil;

public enum BlockObservationHub {
    ;

    public static void requestSodiumRebuild(Minecraft client, int requestedRadius) {
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            return;
        }

        try {
            Class<?> cls = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            Object renderer = cls.getMethod("instanceNullable").invoke(null);
            if (renderer == null) {
                return;
            }

            int renderRadius = (client.options.getEffectiveRenderDistance() + 1) * 16;
            int radius = Math.max(1, Math.min(requestedRadius, renderRadius));
            BlockPos center = client.player.blockPosition();
            cls.getMethod(
                    "scheduleRebuildForBlockArea",
                    int.class, int.class, int.class, int.class, int.class, int.class, boolean.class
            ).invoke(
                    renderer,
                    center.getX() - radius,
                    client.level.getMinY(),
                    center.getZ() - radius,
                    center.getX() + radius,
                    client.level.getMaxY(),
                    center.getZ() + radius,
                    true
            );
        } catch (Throwable ignored) {
            // Sodium отсутствует или изменился API — молча пропускаем
        }
    }

    public static void observeSodiumBlock(int x, int y, int z, BlockState state) {
        if (state == null || state.isAir()) {
            return;
        }

        BlockEspSodiumCandidateCollector.observeSodiumBlock(x, y, z, state);

        if (BedBlockUtil.isBed(state)) {
            BedwarsESP bedwarsEsp = Modules.get(BedwarsESP.class);
            if (bedwarsEsp != null && bedwarsEsp.isEnabled()) {
                bedwarsEsp.acceptSodiumBlockState(x, y, z, state);
            }
        }
    }

    public static void observeSodiumRenderedBlock(BlockPos pos, BlockState state) {
        if (pos == null || state == null || state.isAir()) {
            return;
        }

        BlockEspSodiumCandidateCollector.observeSodiumRenderedBlock(pos.getX(), pos.getY(), pos.getZ(), state);

        if (BedBlockUtil.isBed(state)) {
            BedwarsESP bedwarsEsp = Modules.get(BedwarsESP.class);
            if (bedwarsEsp != null && bedwarsEsp.isEnabled()) {
                bedwarsEsp.acceptSodiumRenderedBlockState(pos, state);
            }
        }
    }

    public static void observeWorldUpdate(BlockPos pos, BlockState state) {
        if (pos == null || state == null) {
            return;
        }

        BlockESP blockEsp = Modules.get(BlockESP.class);
        if (blockEsp != null && blockEsp.isEnabled()) {
            blockEsp.acceptWorldBlockUpdate(pos, state);
        }

        BedwarsESP bedwarsEsp = Modules.get(BedwarsESP.class);
        if (bedwarsEsp != null && bedwarsEsp.isEnabled()) {
            bedwarsEsp.acceptWorldBlockUpdate(pos, state);
        }
    }
}
