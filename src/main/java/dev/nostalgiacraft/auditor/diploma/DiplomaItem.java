package dev.nostalgiacraft.auditor.diploma;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class DiplomaItem extends Item {
    public DiplomaItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".perk").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.monitoringcenter_auditor.diploma.where").withStyle(ChatFormatting.GRAY));
    }
}
