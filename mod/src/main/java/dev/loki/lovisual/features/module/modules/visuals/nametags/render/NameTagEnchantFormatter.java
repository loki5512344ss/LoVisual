package dev.loki.lovisual.features.module.modules.visuals.nametags.render;

import dev.loki.lovisual.util.item.enchant.EnchantMeta;
import dev.loki.lovisual.util.item.enchant.EnchantRegistry;
import dev.loki.lovisual.util.item.enchant.EnchantUtil;
import dev.loki.lovisual.util.item.food.IllegalItemUtil;
import dev.loki.lovisual.util.item.enchant.TopEnchantUtil;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NameTagEnchantFormatter {

    private NameTagEnchantFormatter() {
    }

    static List<NameTagModel.EnchantLine> formatEnchantments(NameTags self, ItemStack stack) {
        List<NameTagModel.EnchantLine> parts = new ArrayList<>();

        ItemEnchantments enchComp = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchComp.entrySet()) {
            Holder<Enchantment> ref = entry.getKey();
            int level = entry.getIntValue();
            if (level <= 0) continue;

            ResourceKey<Enchantment> key = ref.unwrapKey().orElse(null);
            if (key == null) continue;

            EnchantMeta meta = EnchantRegistry.REGISTRY.get(key);
            if (meta == null) continue;

            Enchantment value = ref.value();
            boolean singleLevel = value.getMaxLevel() == 1;
            boolean overMax = EnchantUtil.isInvalidLevel(ref, level);
            if (!isEnchantEnabled(self, meta.category(), meta.key())
                    && !(self.illegalEnchantsAlways.get() && overMax)) {
                continue;
            }

            String text = singleLevel && level == 1
                    ? meta.shortName()
                    : meta.shortName() + level;

            int color = overMax
                    ? IllegalItemUtil.illegalColor()
                    : (level == value.getMaxLevel() ? TopEnchantUtil.topColor() : 0xFFFFFFFF);

            parts.add(new NameTagModel.EnchantLine(text, color));
        }

        return parts;
    }

    static boolean isEnchantEnabled(NameTags self, String category, String path) {
        Map<String, Boolean> map = switch (category) {
            case "armor" -> self.enchantArmor.get();
            case "melee" -> self.enchantMelee.get();
            case "tools" -> self.enchantTools.get();
            case "bow" -> self.enchantBow.get();
            case "fishing" -> self.enchantFishing.get();
            case "trident" -> self.enchantTrident.get();
            case "crossbow" -> self.enchantCrossbow.get();
            case "mace" -> self.enchantMace.get();
            default -> self.enchantOther.get();
        };
        return map.getOrDefault(path, false);
    }

    public static List<List<NameTagModel.EnchantLine>> buildEnchantLines(NameTags self, ItemStack[] slots) {
        List<List<NameTagModel.EnchantLine>> result = new ArrayList<>(slots.length);
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                result.add(List.of());
                continue;
            }
            List<NameTagModel.EnchantLine> lines = formatEnchantments(self, stack);
            result.add(lines.isEmpty() ? List.of() : lines);
        }
        return result;
    }

    public static int maxEnchantLines(List<List<NameTagModel.EnchantLine>> lines) {
        int max = 0;
        for (List<NameTagModel.EnchantLine> entry : lines) {
            int size = entry.size();
            if (size > max) max = size;
        }
        return max;
    }
}
