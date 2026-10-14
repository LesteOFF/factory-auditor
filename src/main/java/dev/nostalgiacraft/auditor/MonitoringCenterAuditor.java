package dev.nostalgiacraft.auditor;

import com.mojang.logging.LogUtils;
import dev.nostalgiacraft.auditor.award.AwardedFactory;
import dev.nostalgiacraft.auditor.award.MilestonePrizes;
import dev.nostalgiacraft.auditor.award.Trophies;
import dev.nostalgiacraft.auditor.command.AuditorCommand;
import dev.nostalgiacraft.auditor.compat.Integrations;
import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.diploma.Diplomas;
import dev.nostalgiacraft.auditor.tab.TabMedals;
import dev.nostalgiacraft.auditor.gametest.IntegrationGameTests;
import dev.nostalgiacraft.auditor.goal.AuditGoals;
import dev.nostalgiacraft.auditor.network.AuditorPayloads;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

@Mod(MonitoringCenterAuditor.MODID)
public final class MonitoringCenterAuditor {
    public static final String MODID = "monitoringcenter_auditor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MonitoringCenterAuditor(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, AuditorConfig.SPEC);
        modBus.addListener(AuditorPayloads::register);
        modBus.addListener(IntegrationGameTests::register);
        Integrations.register();
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(AuditorMetrics::register));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new AuditGoals());
            event.addListener(new MilestonePrizes());
        });
        NeoForge.EVENT_BUS.addListener(MonitoringCenterAuditor::onLevelTick);
        NeoForge.EVENT_BUS.addListener(TabMedals::decorate);
        NeoForge.EVENT_BUS.addListener(Diplomas::decorateName);
        NeoForge.EVENT_BUS.addListener(Diplomas::recheck);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Diplomas.refresh(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(AuditorCommand::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            AwardedFactory.clear();
            Trophies.clear();
            Diplomas.clear();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            AuditorPayloads.forget(event.getEntity().getUUID());
            Diplomas.forget(event.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                AwardedFactory.forget(level);
                Trophies.forget(level);
            }
        });
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AwardedFactory.tick(level);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
