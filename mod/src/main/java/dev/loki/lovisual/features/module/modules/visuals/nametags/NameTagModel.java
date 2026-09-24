package dev.loki.lovisual.features.module.modules.visuals.nametags;

import dev.loki.lovisual.config.values.mode.EnumValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class NameTagModel {

    private NameTagModel() {
    }

    public enum NameplateStyle implements EnumValue.IdProvider {
        LIQUID_GLASS,
        MATTE
    }

    public static final class EffectCache {
        int lastTick = -1;
        List<MobEffectInstance> effects = List.of();
    }

    public static final class EnchantCache {
        int lastTick = -1;
        int signature = 0;
        List<List<EnchantLine>> lines = List.of();
        int maxLines = 0;
    }

    public record ScreenRect(double minX, double minY, double maxX, double maxY) {
    }

    public record NameplateLayout(int nameX, int nameY, int width, int height, float labelScale,
                                  String pingText, String distText, float bgX, float bgY, float bgW, float bgH,
                                  int pingWidth, int pingGap, int nameWidth, int hpWidth, int hpGap,
                                  boolean renderHead, float headX, float headY, float headSize,
                                  float dividerX, float dividerY, float dividerW, float dividerH,
                                  boolean renderTotemBadge, float totemDividerX, float totemDividerY,
                                  float totemDividerW, float totemDividerH, float totemBadgeX, float totemBadgeY,
                                  float totemBadgeW, float totemBadgeH, float totemIconSize) {
    }

    public record EnchantLine(String text, int color) {
    }

    public record RenderEntry(Player player, double dist, float distOffset, int centerX, int nameY, int itemsY,
                              ScreenRect rect, boolean renderSlots, boolean renderNameplate,
                              boolean renderEffects, LabelInfo labelInfo, int infoColor,
                              NameplateLayout nameplateLayout,
                              ItemStack[] slots, List<List<EnchantLine>> enchantLines, int maxEnchantLines,
                              List<MobEffectInstance> effects, int totemPopCount, float totemPopAnimation) {
    }

    public record LabelInfo(String pvpPrefix, Component styledName, String name, int ping, String pingText, String hp,
                            int infoColor) {
    }
}
