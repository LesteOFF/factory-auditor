package dev.nostalgiacraft.auditor.goal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class AuditGoals extends SimpleJsonResourceReloadListener {
    private static final String DIRECTORY = "audit_goals";
    private static final Goal BUILT_IN_FALLBACK = new Goal(32);
    private static volatile Snapshot current = new Snapshot(Map.of(), List.of(), BUILT_IN_FALLBACK, List.of());

    public record Goal(double base) {
    }

    private record Snapshot(Map<Item, Goal> byItem, List<Map.Entry<TagKey<Item>, Goal>> byTag, Goal fallback, List<String> problems) {
    }

    public AuditGoals() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, Goal> byItem = new HashMap<>();
        List<Map.Entry<TagKey<Item>, Goal>> byTag = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Goal fallback = BUILT_IN_FALLBACK;
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                GoalEntry entry = GoalEntry.parse(file.getValue().getAsJsonObject());
                Goal goal = new Goal(entry.base());
                if (entry.item().isPresent()) {
                    ResourceLocation id = ResourceLocation.parse(entry.item().get());
                    if (BuiltInRegistries.ITEM.containsKey(id)) {
                        byItem.put(BuiltInRegistries.ITEM.get(id), goal);
                    } else {
                        problems.add(file.getKey() + ": unknown item " + id);
                    }
                } else if (entry.tag().isPresent()) {
                    byTag.add(Map.entry(TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.tag().get())), goal));
                } else {
                    fallback = goal;
                }
            } catch (RuntimeException invalid) {
                problems.add(file.getKey() + ": " + invalid.getMessage());
            }
        }
        byTag.sort(Map.Entry.comparingByKey((left, right) -> left.location().compareTo(right.location())));
        problems.forEach(problem -> MonitoringCenterAuditor.LOGGER.warn("Ignoring audit goal {}", problem));
        current = new Snapshot(Map.copyOf(byItem), List.copyOf(byTag), fallback, List.copyOf(problems));
    }

    public static Goal goalFor(ItemStack stack) {
        Snapshot snapshot = current;
        Goal exact = snapshot.byItem().get(stack.getItem());
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<TagKey<Item>, Goal> tagged : snapshot.byTag()) {
            if (stack.is(tagged.getKey())) {
                return tagged.getValue();
            }
        }
        return snapshot.fallback();
    }

    public static List<String> problems() {
        return current.problems();
    }
}
