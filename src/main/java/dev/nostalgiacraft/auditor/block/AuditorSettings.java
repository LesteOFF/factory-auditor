package dev.nostalgiacraft.auditor.block;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public record AuditorSettings(IoConfig io, TransferMode transfer, MeasureMode measure, int interval, int tolerance, int limit) {
    public static final int MIN_INTERVAL = 5;
    public static final int MAX_INTERVAL = 72000;
    public static final int MAX_LIMIT = 4096;
    public static final int DEFAULT_INTERVAL = 20;
    private static final int LEGACY_CONSUME = 2;
    public static final AuditorSettings DEFAULT = new AuditorSettings(IoConfig.NONE, TransferMode.PASS, MeasureMode.INSTANT,
            DEFAULT_INTERVAL, 0, 0);
    public static final StreamCodec<ByteBuf, AuditorSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.map(IoConfig::new, IoConfig::bits), AuditorSettings::io,
            ByteBufCodecs.idMapper(index -> pick(TransferMode.values(), index), Enum::ordinal), AuditorSettings::transfer,
            ByteBufCodecs.idMapper(index -> pick(MeasureMode.values(), index), Enum::ordinal), AuditorSettings::measure,
            ByteBufCodecs.VAR_INT, AuditorSettings::interval,
            ByteBufCodecs.VAR_INT, AuditorSettings::tolerance,
            ByteBufCodecs.VAR_INT, AuditorSettings::limit,
            AuditorSettings::new);

    public AuditorSettings {
        interval = Math.clamp(interval, MIN_INTERVAL, MAX_INTERVAL);
        tolerance = Math.clamp(tolerance, 0, interval);
        limit = Math.clamp(limit, 0, MAX_LIMIT);
    }

    public static boolean validInterval(int value) {
        return value >= MIN_INTERVAL && value <= MAX_INTERVAL;
    }

    public static boolean validTolerance(int value, int interval) {
        return value >= 0 && value <= interval;
    }

    public static boolean validLimit(int value) {
        return value >= 0 && value <= MAX_LIMIT;
    }

    private static TransferMode transferOf(CompoundTag tag) {
        if (tag.contains("transfer", Tag.TAG_STRING)) {
            String name = tag.getString("transfer");
            return Arrays.stream(TransferMode.values()).filter(mode -> mode.name().equals(name)).findFirst().orElse(TransferMode.PASS);
        }
        return tag.getInt("transfer") == LEGACY_CONSUME ? TransferMode.CONSUME : TransferMode.PASS;
    }

    public static <E extends Enum<E>> E pick(E[] values, int ordinal) {
        return values[Math.clamp(ordinal, 0, values.length - 1)];
    }

    public AuditorSettings withIo(IoConfig newIo) {
        return new AuditorSettings(newIo, transfer, measure, interval, tolerance, limit);
    }

    public AuditorSettings withTransfer(TransferMode newTransfer) {
        return new AuditorSettings(io, newTransfer, measure, interval, tolerance, limit);
    }

    public AuditorSettings withMeasure(MeasureMode newMeasure) {
        return new AuditorSettings(io, transfer, newMeasure, interval, tolerance, limit);
    }

    public AuditorSettings withNumbers(int newInterval, int newTolerance, int newLimit) {
        return new AuditorSettings(io, transfer, measure, newInterval, newTolerance, newLimit);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("io", io.bits());
        tag.putString("transfer", transfer.name());
        tag.putInt("measure", measure.ordinal());
        tag.putInt("interval", interval);
        tag.putInt("tolerance", tolerance);
        tag.putInt("limit", limit);
        return tag;
    }

    public static AuditorSettings load(CompoundTag tag) {
        return new AuditorSettings(new IoConfig(tag.getInt("io")), transferOf(tag),
                pick(MeasureMode.values(), tag.getInt("measure")), tag.getInt("interval"), tag.getInt("tolerance"), tag.getInt("limit"));
    }
}
