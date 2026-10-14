package dev.nostalgiacraft.auditor.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.UUID;

public record SealData(ResourceLocation item, int tier, UUID owner, String ownerName, double rate, long issuedAt, boolean excellent) {
    public static final Codec<SealData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(SealData::item),
            Codec.INT.fieldOf("tier").forGetter(SealData::tier),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(SealData::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(SealData::ownerName),
            Codec.DOUBLE.fieldOf("rate").forGetter(SealData::rate),
            Codec.LONG.fieldOf("issued_at").forGetter(SealData::issuedAt),
            Codec.BOOL.optionalFieldOf("excellent", false).forGetter(SealData::excellent)
    ).apply(instance, SealData::new));
    public static final StreamCodec<ByteBuf, SealData> STREAM_CODEC = StreamCodec.of(SealData::write, SealData::read);

    public SealData(ResourceLocation item, int tier, UUID owner, String ownerName, double rate, long issuedAt) {
        this(item, tier, owner, ownerName, rate, issuedAt, false);
    }

    private static void write(ByteBuf buffer, SealData data) {
        ResourceLocation.STREAM_CODEC.encode(buffer, data.item());
        ByteBufCodecs.VAR_INT.encode(buffer, data.tier());
        UUIDUtil.STREAM_CODEC.encode(buffer, data.owner());
        ByteBufCodecs.STRING_UTF8.encode(buffer, data.ownerName());
        ByteBufCodecs.DOUBLE.encode(buffer, data.rate());
        ByteBufCodecs.VAR_LONG.encode(buffer, data.issuedAt());
        ByteBufCodecs.BOOL.encode(buffer, data.excellent());
    }

    private static SealData read(ByteBuf buffer) {
        return new SealData(ResourceLocation.STREAM_CODEC.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                UUIDUtil.STREAM_CODEC.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.VAR_LONG.decode(buffer), ByteBufCodecs.BOOL.decode(buffer));
    }

    public SealTier sealTier() {
        return SealTier.byIndex(tier);
    }

    public Item auditedItem() {
        return BuiltInRegistries.ITEM.get(item);
    }
}
