package dev.nostalgiacraft.auditor.board;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public final class Standings {
    final Map<String, Map<UUID, Double>> best = new TreeMap<>();
    final Map<String, UUID> holders = new TreeMap<>();
    final Map<UUID, Map<String, Integer>> tiers = new TreeMap<>();
    final Map<UUID, Integer> seals = new TreeMap<>();
    final Map<UUID, String> names = new TreeMap<>();
    final Map<UUID, Integer> milestones = new TreeMap<>();

    public record Outcome(boolean newHolder, @Nullable UUID previousHolder, int seals) {
    }

    public record Entry(UUID owner, String name, double rate) {
    }

    public record Personal(String item, double rate) {
    }

    public Outcome record(UUID owner, String name, String item, int tier, double rate) {
        UUID previous = holder(item);
        double record = previous == null ? Double.NEGATIVE_INFINITY : best.get(item).get(previous);
        names.put(owner, name);
        best.computeIfAbsent(item, key -> new HashMap<>()).merge(owner, rate, Math::max);
        tiers.computeIfAbsent(owner, key -> new TreeMap<>()).merge(item, tier, Math::max);
        int count = seals.merge(owner, 1, Integer::sum);
        boolean taken = !owner.equals(previous) && rate > record;
        if (taken) {
            holders.put(item, owner);
        }
        return new Outcome(taken, previous, count);
    }

    @Nullable
    public UUID holder(String item) {
        return holders.get(item);
    }

    void restoreHolder(String item) {
        best.getOrDefault(item, Map.of()).entrySet().stream()
                .max(Map.Entry.<UUID, Double>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .ifPresent(entry -> holders.put(item, entry.getKey()));
    }

    public List<Entry> top(String item, int limit) {
        UUID holder = holder(item);
        return best.getOrDefault(item, Map.of()).entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed()
                        .thenComparing(entry -> !entry.getKey().equals(holder))
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> new Entry(entry.getKey(), name(entry.getKey()), entry.getValue()))
                .toList();
    }

    public Optional<Personal> best(UUID owner) {
        Personal fastest = null;
        for (Map.Entry<String, Map<UUID, Double>> item : best.entrySet()) {
            Double rate = item.getValue().get(owner);
            if (rate != null && (fastest == null || rate > fastest.rate())) {
                fastest = new Personal(item.getKey(), rate);
            }
        }
        return Optional.ofNullable(fastest);
    }

    public int seals(UUID owner) {
        return seals.getOrDefault(owner, 0);
    }

    public int bestTier(UUID owner) {
        return tiers.getOrDefault(owner, Map.of()).values().stream().mapToInt(Integer::intValue).max().orElse(-1);
    }

    public int bestTier(UUID owner, String item) {
        return tiers.getOrDefault(owner, Map.of()).getOrDefault(item, -1);
    }

    public int records(UUID owner) {
        return (int) holders.values().stream().filter(owner::equals).count();
    }

    public Title title(UUID owner) {
        return Title.of(seals(owner), records(owner));
    }

    public String name(UUID owner) {
        return names.getOrDefault(owner, owner.toString().substring(0, 8));
    }

    public int lastMilestone(UUID owner) {
        return milestones.getOrDefault(owner, 0);
    }

    public void reachMilestone(UUID owner, int seals) {
        milestones.merge(owner, seals, Math::max);
    }
}
