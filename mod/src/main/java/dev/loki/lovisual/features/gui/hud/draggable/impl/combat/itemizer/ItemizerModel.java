package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ItemizerModel {

    private ItemizerModel() {}

    static void push(Itemizer host, Itemizer.ItemizerEvent event, ItemStack stack, String label) {
        if (!accepts(host, event)) return;
        Itemizer.Entry entry = host.entries.remove(event);
        if (entry == null) {
            entry = new Itemizer.Entry(event);
        }
        entry.stack = stack.copy();
        entry.label = label != null && !label.isBlank() ? label : event.fallbackLabel;
        entry.life = Math.max(0.1f, host.duration.get().floatValue());
        entry.totalLife = entry.life;
        entry.punch = 1.0f;
        host.entries.put(event, entry);
        trimInternalEntries(host);
    }

    static boolean accepts(Itemizer host, Itemizer.ItemizerEvent event) {
        return switch (event) {
            case AUTO_EAT -> host.showAutoEat.get();
            case AUTO_TOTEM -> host.showAutoTotem.get();
            case ELYTRA_SWAP -> host.showElytraSwap.get();
        };
    }

    static void trimInternalEntries(Itemizer host) {
        while (host.entries.size() > Itemizer.MAX_INTERNAL_ENTRIES) {
            Itemizer.ItemizerEvent first = host.entries.keySet().iterator().next();
            host.entries.remove(first);
        }
    }

    static List<Itemizer.Entry> updateEntries(Itemizer host, float dt) {
        List<Itemizer.ItemizerEvent> remove = new ArrayList<>();
        List<Itemizer.Entry> out = new ArrayList<>();
        for (Map.Entry<Itemizer.ItemizerEvent, Itemizer.Entry> mapEntry : host.entries.entrySet()) {
            Itemizer.Entry entry = mapEntry.getValue();
            entry.life = Math.max(0.0f, entry.life - dt);
            entry.punch = AnimationUtility.approach(entry.punch, 0.0f, dt, 8.0f);
            boolean alive = entry.life > 0.0f;
            entry.anim = AnimationUtility.approach(entry.anim, alive ? 1.0f : 0.0f, dt, 13.0f);
            entry.anim = AnimationUtility.snap(entry.anim, alive ? 1.0f : 0.0f, 0.002f);
            if (!alive && entry.anim <= 0.0f) {
                remove.add(mapEntry.getKey());
                continue;
            }
            if (out.size() < host.maxEntries.get()) {
                out.add(entry);
            }
        }
        for (Itemizer.ItemizerEvent event : remove) {
            host.entries.remove(event);
        }
        return out;
    }

    static List<Itemizer.Entry> previewEntries(Itemizer host) {
        Itemizer.Entry eat = new Itemizer.Entry(Itemizer.ItemizerEvent.AUTO_EAT);
        eat.stack = new ItemStack(Items.COOKED_BEEF);
        eat.label = "Eat";
        eat.life = 1.0f;
        eat.totalLife = 1.0f;
        eat.anim = 1.0f;

        Itemizer.Entry totem = new Itemizer.Entry(Itemizer.ItemizerEvent.AUTO_TOTEM);
        totem.stack = new ItemStack(Items.TOTEM_OF_UNDYING);
        totem.label = "Totem";
        totem.life = 1.0f;
        totem.totalLife = 1.0f;
        totem.anim = 1.0f;

        Itemizer.Entry swap = new Itemizer.Entry(Itemizer.ItemizerEvent.ELYTRA_SWAP);
        swap.stack = new ItemStack(Items.ELYTRA);
        swap.label = "Swap";
        swap.life = 1.0f;
        swap.totalLife = 1.0f;
        swap.anim = 1.0f;

        List<Itemizer.Entry> list = new ArrayList<>();
        list.add(eat);
        list.add(totem);
        list.add(swap);
        return list.subList(0, Math.min(host.maxEntries.get(), list.size()));
    }

    static float baseEntryWidth(Itemizer host) {
        return host.displayMode.get() == Itemizer.ItemizerDisplayMode.COMPACT ? Itemizer.BASE_COMPACT_W : Itemizer.BASE_ICON_CARD;
    }

    static float baseEntryHeight(Itemizer host) {
        return host.displayMode.get() == Itemizer.ItemizerDisplayMode.COMPACT ? Itemizer.BASE_COMPACT_H : Itemizer.BASE_ICON_CARD;
    }
}
