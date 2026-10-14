package dev.nostalgiacraft.auditor.registry;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.diploma.DiplomaItem;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MonitoringCenterAuditor.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            MonitoringCenterAuditor.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            MonitoringCenterAuditor.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SealData>> SEAL_DATA = COMPONENTS.registerComponentType(
            "seal", builder -> builder.persistent(SealData.CODEC).networkSynchronized(SealData.STREAM_CODEC));
    public static final DeferredItem<SealItem> SEAL = ITEMS.registerItem("seal", SealItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DiplomaItem> JUNIOR_DIPLOMA = diploma("junior_diploma", Rarity.UNCOMMON);
    public static final DeferredItem<DiplomaItem> SENIOR_DIPLOMA = diploma("senior_diploma", Rarity.RARE);
    public static final DeferredItem<DiplomaItem> MASTER_DIPLOMA = diploma("master_diploma", Rarity.EPIC);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.monitoringcenter_auditor"))
            .icon(() -> ModBlocks.AUDITOR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private ModItems() {
    }

    private static DeferredItem<DiplomaItem> diploma(String name, Rarity rarity) {
        return ITEMS.registerItem(name, DiplomaItem::new, new Item.Properties().stacksTo(1).rarity(rarity));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
    }
}
