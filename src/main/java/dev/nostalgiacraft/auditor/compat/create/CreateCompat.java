package dev.nostalgiacraft.auditor.compat.create;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreateCompat {
    private static final DeferredRegister<DisplaySource> SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE,
            MonitoringCenterAuditor.MODID);
    private static final DeferredHolder<DisplaySource, AuditorDisplaySource> AUDITOR_SOURCE = SOURCES.register("auditor",
            AuditorDisplaySource::new);

    private CreateCompat() {
    }

    public static void register() {
        IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        SOURCES.register(modBus);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(CreateCompat::link));
    }

    private static void link() {
        DisplaySource.BY_BLOCK.add(ModBlocks.AUDITOR.get(), AUDITOR_SOURCE.get());
    }
}
