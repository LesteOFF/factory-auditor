package dev.nostalgiacraft.auditor.award;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MilestonePrizes extends SimpleJsonResourceReloadListener {
    private static final String DIRECTORY = "diplomas";
    private static volatile Milestones current = Milestones.NONE;
    private static volatile List<String> problems = List.of();

    public MilestonePrizes() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Milestone> milestones = new ArrayList<>();
        List<String> found = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                Milestone milestone = Milestone.parse(file.getValue().getAsJsonObject());
                ResourceLocation item = ResourceLocation.parse(milestone.item());
                if (BuiltInRegistries.ITEM.containsKey(item) && BuiltInRegistries.ITEM.get(item) != Items.AIR) {
                    milestones.add(milestone);
                } else {
                    found.add(file.getKey() + ": unknown item " + milestone.item());
                }
            } catch (RuntimeException invalid) {
                found.add(file.getKey() + ": " + invalid.getMessage());
            }
        }
        found.forEach(problem -> MonitoringCenterAuditor.LOGGER.warn("Ignoring milestone prize {}", problem));
        current = Milestones.of(milestones);
        problems = List.copyOf(found);
    }

    public static Milestones current() {
        return current;
    }

    public static List<String> problems() {
        return problems;
    }

    public static ItemStack prize(Milestone milestone) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(milestone.item())));
    }
}
