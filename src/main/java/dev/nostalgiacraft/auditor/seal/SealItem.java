package dev.nostalgiacraft.auditor.seal;

import dev.nostalgiacraft.auditor.registry.ModItems;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class SealItem extends Item {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SealItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(SealData data) {
        ItemStack stack = new ItemStack(ModItems.SEAL.get());
        stack.set(ModItems.SEAL_DATA.get(), data);
        return stack;
    }

    @Nullable
    public static SealData data(ItemStack stack) {
        return stack.get(ModItems.SEAL_DATA.get());
    }

    public static Component tierName(SealTier tier) {
        return Component.translatable("seal_tier.monitoringcenter_auditor." + tier.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Component getName(ItemStack stack) {
        SealData data = data(stack);
        if (data == null) {
            return super.getName(stack);
        }
        String key = data.excellent() ? "item.monitoringcenter_auditor.seal.excellent" : "item.monitoringcenter_auditor.seal.named";
        return Component.translatable(key, tierName(data.sealTier()), data.auditedItem().getDescription());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        SealData data = data(stack);
        return data != null && data.excellent();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SealData data = data(stack);
        if (data == null) {
            return;
        }
        if (data.excellent()) {
            tooltip.add(Component.translatable("tooltip.monitoringcenter_auditor.seal.excellent").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.monitoringcenter_auditor.seal.owner", data.ownerName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.monitoringcenter_auditor.seal.rate", ValueFormat.number(data.rate()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.monitoringcenter_auditor.seal.date",
                DATE.format(Instant.ofEpochMilli(data.issuedAt()).atZone(ZoneId.systemDefault()))).withStyle(ChatFormatting.DARK_GRAY));
    }
}
