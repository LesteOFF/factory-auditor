package dev.nostalgiacraft.auditor.compat.jade;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.AuditorBlock;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class AuditorJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = MonitoringCenterAuditor.id("auditor");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new AuditorReading(), AuditorBlock.class);
    }

    private static final class AuditorReading implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof AuditorBlockEntity auditor) {
                tooltip.add(AuditorTexts.screen("jade.rate", ValueFormat.number(auditor.ratePerMinute())));
                tooltip.add(AuditorTexts.status(auditor.status()));
                tooltip.add(AuditorTexts.option("transfer", auditor.settings().transfer()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
