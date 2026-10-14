package dev.nostalgiacraft.auditor.rate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemTally {
    public static final int MAX_KINDS = 16;
    private final Map<Item, Long> counts = new LinkedHashMap<>();

    public void add(Item item, long count) {
        if (counts.containsKey(item) || counts.size() < MAX_KINDS) {
            counts.merge(item, count, Long::sum);
        }
    }

    public Item top() {
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Items.AIR);
    }

    public Map<Item, Long> view() {
        return Collections.unmodifiableMap(counts);
    }

    public void clear() {
        counts.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        counts.forEach((item, count) -> tag.putLong(BuiltInRegistries.ITEM.getKey(item).toString(), count));
        return tag;
    }

    public void load(CompoundTag tag) {
        counts.clear();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                add(BuiltInRegistries.ITEM.get(id), tag.getLong(key));
            }
        }
    }
}
