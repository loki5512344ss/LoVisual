package dev.loki.lovisual.features.module.modules.combat.pvp;

import net.minecraft.client.Minecraft;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;

@ModuleInfo(
        id = "tpssync",
        displayName = "TPSSync",
        category = ModuleCategory.COMBAT
)
public class TPSSync extends Module {

    private static final String SETTING_START_TPS = "start_tps";
    private final BooleanValue enabled =
            new BooleanValue("tpssync_enabled", true);
    private final NumberValue<Float> startTps =
            num("tpssync_start_tps", SETTING_START_TPS, 19.0f, 1.0f, 20.0f);

    /** сколько серверных тиков прошло за клиентский */
    public float getServerTickDelta() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || !enabled.get()) return 1.0f;
        if (mc == null || mc.level == null) return 1.0f;

        float tps = NetworkStatsUtil.getTps(mc);
        if (tps >= startTps.get()) return 1.0f;

        return Math.max(0.0f, tps / 20.0f);
    }
}
