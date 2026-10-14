package dev.nostalgiacraft.auditor.registry;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.AuditorBlock;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.PlaqueBlock;
import dev.nostalgiacraft.auditor.block.PlaqueBlockEntity;
import dev.nostalgiacraft.auditor.block.TrophyBlock;
import dev.nostalgiacraft.auditor.block.TrophyBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final int BRONZE_LIGHT = 10;
    private static final int GOLD_LIGHT = 15;
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MonitoringCenterAuditor.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,
            MonitoringCenterAuditor.MODID);

    public static final DeferredBlock<AuditorBlock> AUDITOR = BLOCKS.registerBlock("auditor", AuditorBlock::new, machine());
    public static final DeferredItem<BlockItem> AUDITOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(AUDITOR);
    public static final DeferredBlock<PlaqueBlock> PLAQUE = BLOCKS.registerBlock("plaque", PlaqueBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> PLAQUE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(PLAQUE);
    public static final DeferredBlock<TrophyBlock> BRONZE_TROPHY = trophy("bronze_trophy", MapColor.COLOR_ORANGE, BRONZE_LIGHT);
    public static final DeferredItem<BlockItem> BRONZE_TROPHY_ITEM = ModItems.ITEMS.registerSimpleBlockItem(BRONZE_TROPHY,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<TrophyBlock> GOLD_TROPHY = trophy("gold_trophy", MapColor.GOLD, GOLD_LIGHT);
    public static final DeferredItem<BlockItem> GOLD_TROPHY_ITEM = ModItems.ITEMS.registerSimpleBlockItem(GOLD_TROPHY,
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AuditorBlockEntity>> AUDITOR_ENTITY = BLOCK_ENTITIES.register(
            "auditor", () -> BlockEntityType.Builder.of(AuditorBlockEntity::new, AUDITOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyBlockEntity>> TROPHY_ENTITY = BLOCK_ENTITIES.register(
            "trophy", () -> BlockEntityType.Builder.of(TrophyBlockEntity::new, BRONZE_TROPHY.get(), GOLD_TROPHY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaqueBlockEntity>> PLAQUE_ENTITY = BLOCK_ENTITIES.register(
            "plaque", () -> BlockEntityType.Builder.of(PlaqueBlockEntity::new, PLAQUE.get()).build(null));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModBlocks::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUDITOR_ENTITY.get(), AuditorBlockEntity::itemHandlerFor);
    }

    private static DeferredBlock<TrophyBlock> trophy(String name, MapColor color, int light) {
        return BLOCKS.registerBlock(name, TrophyBlock::new, BlockBehaviour.Properties.of().mapColor(color).strength(1f, 6f)
                .sound(SoundType.METAL).noOcclusion().lightLevel(state -> light));
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }
}
