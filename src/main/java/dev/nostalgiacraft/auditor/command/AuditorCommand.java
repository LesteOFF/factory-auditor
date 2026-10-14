package dev.nostalgiacraft.auditor.command;

import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public final class AuditorCommand {
    private static final int SHOWN = 10;

    private AuditorCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("auditor")
                .then(Commands.literal("top")
                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                .executes(context -> top(context.getSource(), ItemArgument.getItem(context, "item").getItem())))));
    }

    private static int top(CommandSourceStack source, Item item) {
        List<Standings.Entry> top = Leaderboard.get(source.getServer()).standings().top(BuiltInRegistries.ITEM.getKey(item).toString(), SHOWN);
        if (top.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.monitoringcenter_auditor.top.empty", item.getDescription()), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.monitoringcenter_auditor.top.header", item.getDescription()), false);
        for (int index = 0; index < top.size(); index++) {
            Standings.Entry entry = top.get(index);
            int rank = index + 1;
            source.sendSuccess(() -> Component.translatable("command.monitoringcenter_auditor.top.line", rank, entry.name(),
                    ValueFormat.number(entry.rate())), false);
        }
        return top.size();
    }
}
