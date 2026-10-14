package dev.nostalgiacraft.auditor;

import dev.nostalgiacraft.auditor.client.AuditorRenderer;
import dev.nostalgiacraft.auditor.client.PlaqueRenderer;
import dev.nostalgiacraft.auditor.client.TrophyRenderer;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.registry.ModItems;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = MonitoringCenterAuditor.MODID, dist = Dist.CLIENT)
public final class MonitoringCenterAuditorClient {
    public MonitoringCenterAuditorClient(IEventBus modBus) {
        modBus.addListener(MonitoringCenterAuditorClient::registerRenderers);
        modBus.addListener(MonitoringCenterAuditorClient::registerItemColors);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            SealData data = SealItem.data(stack);
            return layer == 0 && data != null ? FastColor.ARGB32.opaque(data.sealTier().color()) : -1;
        }, ModItems.SEAL.get());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.AUDITOR_ENTITY.get(), AuditorRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.PLAQUE_ENTITY.get(), PlaqueRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.TROPHY_ENTITY.get(), TrophyRenderer::new);
    }
}
