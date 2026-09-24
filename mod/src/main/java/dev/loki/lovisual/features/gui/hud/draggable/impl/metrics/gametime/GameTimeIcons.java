package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

final class GameTimeIcons {

    private GameTimeIcons() {
    }

    static String iconString(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    static TextRenderer getWeatherIcons() {
        return Fonts.renderer("WeatherIcons", FontInfo.Type.Regular, TextRenderer.get());
    }

    static String formatTime(long time) {
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) ((time % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }

    static int resolveIcon(GameTime host, long time) {
        if (!host.showWeather.get()) return resolveClearIcon(time);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return resolveClearIcon(time);

        boolean thunder = WorldTweaks.isServerThundering(mc.level);
        boolean rain = WorldTweaks.isServerRaining(mc.level);
        if (!thunder && !rain) return resolveClearIcon(time);

        boolean day = time < 12000L;
        boolean snow = isSnowingAtPlayer();
        if (thunder) {
            return snow ? (day ? GameTime.ICON_THUNDER_SNOW_DAY : GameTime.ICON_THUNDER_SNOW_NIGHT)
                    : (day ? GameTime.ICON_THUNDER_DAY : GameTime.ICON_THUNDER_NIGHT);
        }
        if (snow) return day ? GameTime.ICON_SNOW_DAY : GameTime.ICON_SNOW_NIGHT;
        return day ? GameTime.ICON_RAIN_DAY : GameTime.ICON_RAIN_NIGHT;
    }

    static int resolveClearIcon(long time) {
        if (time >= GameTime.SUNRISE_START || time < GameTime.SUNRISE_END) return GameTime.ICON_SUNRISE;
        if (time >= GameTime.SUNSET_START && time < GameTime.SUNSET_END) return GameTime.ICON_SUNSET;
        return GameTime.ICON_SUN;
    }

    static boolean isSnowingAtPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return false;
        BlockPos pos = mc.player.blockPosition();
        Biome.Precipitation precipitation = mc.level.getBiome(pos).value()
                .getPrecipitationAt(pos, mc.level.getSeaLevel());
        return precipitation == Biome.Precipitation.SNOW;
    }
}
