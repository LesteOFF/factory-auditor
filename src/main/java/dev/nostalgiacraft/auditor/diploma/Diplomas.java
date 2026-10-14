package dev.nostalgiacraft.auditor.diploma;

import dev.nostalgiacraft.auditor.tab.TabMedals;
import dev.nostalgiacraft.monitor.compat.OptionalEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

public final class Diplomas {
    private static final int RECHECK_TICKS = 600;
    private static final Map<UUID, Optional<Diploma>> SHOWN = new HashMap<>();
    private static BiPredicate<ServerPlayer, Item> equipped = (player, item) -> false;

    private Diplomas() {
    }

    public static void useEquipmentCheck(BiPredicate<ServerPlayer, Item> check) {
        equipped = check;
    }

    public static Set<Diploma> held(ServerPlayer player) {
        EnumSet<Diploma> held = EnumSet.noneOf(Diploma.class);
        for (Diploma diploma : Diploma.values()) {
            if (player.getInventory().hasAnyMatching(stack -> stack.is(diploma.item())) || isEquipped(player, diploma.item())) {
                held.add(diploma);
            }
        }
        return held;
    }

    private static boolean isEquipped(ServerPlayer player, Item item) {
        return OptionalEvents.guarded("curios", false, () -> equipped.test(player, item));
    }

    public static Optional<Diploma> best(ServerPlayer player) {
        return held(player).stream().max(Comparator.naturalOrder());
    }

    public static Optional<Diploma> shown(ServerPlayer player) {
        return SHOWN.computeIfAbsent(player.getUUID(), key -> best(player));
    }

    public static void clear() {
        SHOWN.clear();
    }

    public static void recheck(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer) && player.tickCount % RECHECK_TICKS == 0) {
            refresh(player);
        }
    }

    public static void refresh(ServerPlayer player) {
        Optional<Diploma> best = best(player);
        if (!best.equals(SHOWN.put(player.getUUID(), best))) {
            player.refreshDisplayName();
            player.refreshTabListName();
        }
    }

    public static void forget(UUID player) {
        SHOWN.remove(player);
    }

    public static void decorateName(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            shown(player).ifPresent(diploma -> event.setDisplayname(Component.empty().append(event.getDisplayname())
                    .append(" ").append(TabMedals.glyph(diploma.glyph()))));
        }
    }
}
