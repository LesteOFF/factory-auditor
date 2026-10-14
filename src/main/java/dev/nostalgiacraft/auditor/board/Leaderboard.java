package dev.nostalgiacraft.auditor.board;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class Leaderboard extends SavedData {
    private final Standings standings = new Standings();

    public static Leaderboard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Leaderboard::new, Leaderboard::load),
                MonitoringCenterAuditor.MODID);
    }

    public Standings standings() {
        return standings;
    }

    public Standings.Outcome record(UUID owner, String name, String item, int tier, double rate) {
        setDirty();
        return standings.record(owner, name, item, tier, rate);
    }

    public void reachMilestone(UUID owner, int seals) {
        standings.reachMilestone(owner, seals);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag best = new CompoundTag();
        standings.best.forEach((item, rates) -> {
            CompoundTag perOwner = new CompoundTag();
            rates.forEach((owner, rate) -> perOwner.putDouble(owner.toString(), rate));
            best.put(item, perOwner);
        });
        CompoundTag holders = new CompoundTag();
        standings.holders.forEach((item, owner) -> holders.putUUID(item, owner));
        tag.put("holders", holders);
        CompoundTag tiers = new CompoundTag();
        standings.tiers.forEach((owner, items) -> {
            CompoundTag perItem = new CompoundTag();
            items.forEach(perItem::putInt);
            tiers.put(owner.toString(), perItem);
        });
        CompoundTag seals = new CompoundTag();
        standings.seals.forEach((owner, count) -> seals.putInt(owner.toString(), count));
        CompoundTag names = new CompoundTag();
        standings.names.forEach((owner, name) -> names.putString(owner.toString(), name));
        CompoundTag milestones = new CompoundTag();
        standings.milestones.forEach((owner, count) -> milestones.putInt(owner.toString(), count));
        tag.put("best", best);
        tag.put("tiers", tiers);
        tag.put("seals", seals);
        tag.put("names", names);
        tag.put("milestones", milestones);
        return tag;
    }

    private static Leaderboard load(CompoundTag tag, HolderLookup.Provider registries) {
        Leaderboard board = new Leaderboard();
        Standings standings = board.standings;
        CompoundTag best = tag.getCompound("best");
        CompoundTag holders = tag.getCompound("holders");
        for (String item : best.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(item);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            CompoundTag perOwner = best.getCompound(item);
            Map<UUID, Double> rates = new HashMap<>();
            perOwner.getAllKeys().forEach(owner -> rates.put(UUID.fromString(owner), perOwner.getDouble(owner)));
            standings.best.put(item, rates);
            if (holders.hasUUID(item) && rates.containsKey(holders.getUUID(item))) {
                standings.holders.put(item, holders.getUUID(item));
            } else {
                standings.restoreHolder(item);
            }
        }
        CompoundTag tiers = tag.getCompound("tiers");
        for (String owner : tiers.getAllKeys()) {
            CompoundTag perItem = tiers.getCompound(owner);
            Map<String, Integer> items = new TreeMap<>();
            perItem.getAllKeys().forEach(item -> items.put(item, perItem.getInt(item)));
            standings.tiers.put(UUID.fromString(owner), items);
        }
        CompoundTag seals = tag.getCompound("seals");
        seals.getAllKeys().forEach(owner -> standings.seals.put(UUID.fromString(owner), seals.getInt(owner)));
        CompoundTag names = tag.getCompound("names");
        names.getAllKeys().forEach(owner -> standings.names.put(UUID.fromString(owner), names.getString(owner)));
        CompoundTag milestones = tag.getCompound("milestones");
        milestones.getAllKeys().forEach(owner -> standings.milestones.put(UUID.fromString(owner), milestones.getInt(owner)));
        return board;
    }
}
