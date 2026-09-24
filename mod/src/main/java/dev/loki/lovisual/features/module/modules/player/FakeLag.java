package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.common.impl.common.FakeLagFlushOn;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.network.control.FakeLagController;

import java.util.EnumSet;
import dev.loki.lovisual.util.network.control.BlinkManager;

/**
 * FakeLag module front-end for the shared BlinkManager-backed controller.
 */
//todo Description
@ModuleInfo(
        id = "fakelag",
        displayName = "FakeLag",
        category = ModuleCategory.PLAYER
)
public final class FakeLag extends Module {
    private final NumberValue<Float> minRange = numCommon(
            "fakeLagMinRange",
            "min_range",
            CommonSettingSchemas.PLAYER_FAKELAG_RANGE_MIN,
            2.0f,
            0.0f,
            10.0f
    );

    private final NumberValue<Float> maxRange = numCommon(
            "fakeLagMaxRange",
            "max_range",
            CommonSettingSchemas.PLAYER_FAKELAG_RANGE_MAX,
            5.0f,
            0.0f,
            10.0f
    );

    private final NumberValue<Integer> minDelay = numCommon(
            "fakeLagMinDelay",
            "delay_min",
            CommonSettingSchemas.PLAYER_FAKELAG_DELAY_MIN,
            300,
            0,
            1000
    );

    private final NumberValue<Integer> maxDelay = numCommon(
            "fakeLagMaxDelay",
            "delay_max",
            CommonSettingSchemas.PLAYER_FAKELAG_DELAY_MAX,
            600,
            0,
            1000
    );

    private final NumberValue<Integer> recoilTime = numCommon(
            "fakeLagRecoilTime",
            "recoil_time",
            CommonSettingSchemas.PLAYER_FAKELAG_RECOIL_TIME,
            250,
            0,
            1000
    );

    private final EnumValue<FakeLagController.Mode> mode = enumCommon(
            "fakeLagMode",
            "mode",
            CommonSettingSchemas.PLAYER_FAKELAG_MODE,
            FakeLagController.Mode.DYNAMIC,
            FakeLagController.Mode.class
    );

    private final BooleanMapValue flushOn = groupCommon(
            "fakeLagFlushOn",
            "flush_on",
            CommonSettingSchemas.PLAYER_FAKELAG_FLUSH_ON
    );

    private FakeLagController.Config appliedConfig;

    @Override
    public void onEnable() {
        syncControllerConfig();
        FakeLagController.INSTANCE.setEnabled(true);
    }

    @Override
    public void onDisable() {
        FakeLagController.INSTANCE.setEnabled(false);
        appliedConfig = null;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            return;
        }
        syncControllerConfig();
    }

    private void syncControllerConfig() {
        FakeLagController.Config config = buildConfig();
        if (!config.equals(appliedConfig) || !config.equals(FakeLagController.INSTANCE.getConfig())) {
            FakeLagController.INSTANCE.configure(config);
            appliedConfig = config;
        }
    }

    private FakeLagController.Config buildConfig() {
        float min = minRange.get();
        float max = maxRange.get();
        int minMs = minDelay.get();
        int maxMs = maxDelay.get();
        return new FakeLagController.Config(
                Math.min(min, max),
                Math.max(min, max),
                Math.min(minMs, maxMs),
                Math.max(minMs, maxMs),
                recoilTime.get(),
                mode.get(),
                selectedFlushOn()
        );
    }

    private EnumSet<FakeLagController.FlushOn> selectedFlushOn() {
        EnumSet<FakeLagController.FlushOn> selected = EnumSet.noneOf(FakeLagController.FlushOn.class);
        if (flushOn.get(FakeLagFlushOn.ENTITY_INTERACT)) {
            selected.add(FakeLagController.FlushOn.ENTITY_INTERACT);
        }
        if (flushOn.get(FakeLagFlushOn.BLOCK_INTERACT)) {
            selected.add(FakeLagController.FlushOn.BLOCK_INTERACT);
        }
        if (flushOn.get(FakeLagFlushOn.ACTION)) {
            selected.add(FakeLagController.FlushOn.ACTION);
        }
        return selected;
    }
}
