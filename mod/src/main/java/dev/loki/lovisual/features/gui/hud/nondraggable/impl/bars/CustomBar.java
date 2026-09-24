package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Set;

import static dev.loki.lovisual.features.theme.Theme.theme;
import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel.*;

@HudElementRegister(order = 40)
public final class CustomBar extends AbstractHudElement {

    public static final CustomBar INSTANCE = new CustomBar();

    private final BarSettings settings = new BarSettings();

    private CustomBar() {
        super("vanilla_bar", "Bar", true);
    }

    public static CustomBar get() {
        return INSTANCE;
    }

    public static boolean shouldUseLocator(Minecraft mc, CustomBar bm) {
        if (mc == null || bm == null) return false;
        return switch (bm.getHotbarBarMode()) {
            case LOCATOR -> true;
            case LEVEL -> false;
            case AUTO -> mc.getConnection() != null
                    && mc.getConnection().getWaypointManager().hasWaypoints();
        };
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        settings.defineSettings(defs);
    }

    @Override
    protected void onLoaded() {
        settings.registerBinds(this);
    }

    @Override
    public void onTick() {
        settings.handleToggles(this);
    }

    public boolean isHudBarEnabled() {
        return isEnabled();
    }

    public float getHudOverlayAlphaFactor() {
        return getHudOverlayAlpha() / 255f;
    }

    public int getHudOverlayAlpha() {
        int v = settings.overlayAlpha.get();
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    public boolean isHotbarXpBarEnabled() {
        return settings.xpBar.get();
    }

    public boolean isHotbarXpBarShowLevel() {
        return settings.xpBarShowLevel.get();
    }

    public CustomBarModel.BarMode getHotbarBarMode() {
        return settings.barMode.get();
    }

    public int getHotbarXpBarBgRgb() {
        return settings.xpBarBgColor.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarFillStartRgb() {
        return settings.xpBarFillStart.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarFillEndRgb() {
        return settings.xpBarFillEnd.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarTextRgb() {
        return settings.xpBarTextColor.getArgb() & 0x00FFFFFF;
    }

    public CustomBarModel.XpBarStyle getHotbarXpBarStyle() {
        return settings.xpBarStyle.get();
    }

    public float getBarWidth() {
        return XP_BAR_WIDTH;
    }

    public float getBarHeight() {
        return XP_BAR_HEIGHT;
    }

    public float getXpTextScale() {
        return (float) XP_TEXT_SCALE;
    }

    public int getXpTextArgb() {
        if (settings.isCustomXpStyleEnabled()) {
            return settings.customTextColor.getArgb();
        }
        return HudRenderUtil.mixColor(theme().textPrimary(), theme().accent(), 0.32f);
    }

    public boolean isCustomMode() {
        return settings.isCustomMode();
    }

    // Custom color accessors consumed by the XP bar renderer.
    public int customBgArgb() {
        return settings.customBgColor.getArgb();
    }

    public int customFillArgb() {
        return settings.customFillColor.getArgb();
    }

    public int customFillGradientArgb() {
        return settings.customFillGradientColor.getArgb();
    }

    public boolean isLocatorSearchEnabled() {
        return settings.locatorModeEnabled() && settings.locatorSearchEnabled.get();
    }

    public Set<String> getLocatorSearchPlayers() {
        return settings.locatorSearchPlayers.get();
    }

    public int getLocatorDimmedColorRgb() {
        return settings.locatorDimmedColor.getArgb() & 0x00FFFFFF;
    }

    public int getLocatorSearchColorRgb() {
        return settings.locatorSearchColor.getArgb() & 0x00FFFFFF;
    }

    public boolean isLocatorDimmedScaleOnly() {
        return settings.locatorDimmedScaleOnly.get();
    }

    public boolean isLocatorMapFilterEnabled() {
        return settings.locatorMapFilterEnabled.get();
    }

    public String getLocatorMapServer() {
        return settings.locatorMapServer.get();
    }

    public String getLocatorMapUrl() {
        return settings.locatorMapUrl.get();
    }

    public boolean isLocatorLabelsEnabled() {
        return settings.locatorModeEnabled() && settings.locatorLabels.get();
    }

    public boolean isLocatorLabelDistanceEnabled() {
        return isLocatorLabelsEnabled() && settings.locatorLabelDistance.get();
    }

    public float getLocatorLabelAngle() {
        return settings.locatorLabelAngle.get();
    }

    public CustomBarModel.LocatorLabelMode getLocatorLabelMode() {
        return settings.locatorLabelMode.get();
    }
}
