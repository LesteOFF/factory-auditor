package dev.nostalgiacraft.auditor.network;

import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.auditor.board.Title;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public record BoardSnapshot(String item, List<Line> lines, int seals, int title) {
    public static final int SHOWN = 8;
    private static final int NAME_LENGTH = 32;
    public static final StreamCodec<ByteBuf, BoardSnapshot> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardSnapshot::item,
            Line.CODEC.apply(ByteBufCodecs.list(SHOWN)), BoardSnapshot::lines,
            ByteBufCodecs.VAR_INT, BoardSnapshot::seals,
            ByteBufCodecs.VAR_INT, BoardSnapshot::title,
            BoardSnapshot::new);

    public record Line(String name, double rate) {
        public static final StreamCodec<ByteBuf, Line> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(NAME_LENGTH), Line::name, ByteBufCodecs.DOUBLE, Line::rate, Line::new);
    }

    public static BoardSnapshot of(MinecraftServer server, AuditorBlockEntity auditor, UUID viewer) {
        Standings standings = Leaderboard.get(server).standings();
        Item audited = auditor.auditedItem();
        String item = audited == Items.AIR ? "" : BuiltInRegistries.ITEM.getKey(audited).toString();
        List<Line> lines = standings.top(item, SHOWN).stream()
                .map(entry -> new Line(entry.name().substring(0, Math.min(entry.name().length(), NAME_LENGTH)), entry.rate())).toList();
        return new BoardSnapshot(item, lines, standings.seals(viewer), standings.title(viewer).ordinal());
    }

    public Title titleValue() {
        return Title.values()[Math.clamp(title, 0, Title.values().length - 1)];
    }
}
