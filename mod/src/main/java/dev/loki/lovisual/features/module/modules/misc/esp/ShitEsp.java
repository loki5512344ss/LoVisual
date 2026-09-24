package dev.loki.lovisual.features.module.modules.misc.esp;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

import java.util.Set;

/**
 * Подсветка слотов инвентаря по группам предметов (замена ShitDropper-выброса).
 * Каждая группа — список item id + цвет. Рендер через AbstractContainerScreenMixin.
 */
@ModuleInfo(id = "shitesp", displayName = "ShitESP", category = ModuleCategory.MISC)
public class ShitEsp extends Module {

    private final ItemIdSetValue groupA = itemList("shitEspGroupA", "group_a", TextListSetting.PickerMode.ALL);
    private final RGBAColorValue colorA = color("shitEspColorA", "color_a", "#80FF3B30");

    private final ItemIdSetValue groupB = itemList("shitEspGroupB", "group_b", TextListSetting.PickerMode.ALL);
    private final RGBAColorValue colorB = color("shitEspColorB", "color_b", "#8030FF9A");

    private final ItemIdSetValue groupC = itemList("shitEspGroupC", "group_c", TextListSetting.PickerMode.ALL);
    private final RGBAColorValue colorC = color("shitEspColorC", "color_c", "#80309CFF");

    public Integer colorFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        Set<String> a = groupA.get();
        if (a != null && a.contains(id)) return colorA.getArgb();
        Set<String> b = groupB.get();
        if (b != null && b.contains(id)) return colorB.getArgb();
        Set<String> c = groupC.get();
        if (c != null && c.contains(id)) return colorC.getArgb();
        return null;
    }

    public boolean hasAnyGroup() {
        Set<String> a = groupA.get();
        Set<String> b = groupB.get();
        Set<String> c = groupC.get();
        return (a != null && !a.isEmpty()) || (b != null && !b.isEmpty()) || (c != null && !c.isEmpty());
    }
}
