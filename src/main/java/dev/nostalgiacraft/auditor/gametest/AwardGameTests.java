package dev.nostalgiacraft.auditor.gametest;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.PlaqueBlock;
import dev.nostalgiacraft.auditor.block.PlaqueBlockEntity;
import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MonitoringCenterAuditor.MODID)
@PrefixGameTestTemplate(false)
public final class AwardGameTests {
    private static final BlockPos PLAQUE = Scenario.CENTER;
    private static final BlockPos NEAR_FURNACE = PLAQUE.east();
    private static final BlockPos FAR_FURNACE = PLAQUE.east(7);
    private static final BlockPos EXCELLENT_REACH = PLAQUE.east(6);
    private static final int FURNACE_OUTPUT = 2;
    private static final int ACCELERATED_CHECK = 170;
    private static final int UNACCELERATED_DONE = 215;
    private static final double HIGH_RATE = 12000;

    private AwardGameTests() {
    }

    static PlaqueBlockEntity plaque(GameTestHelper helper, BlockPos pos, Direction wall, Item item, SealTier tier, double rate) {
        helper.setBlock(pos, ModBlocks.PLAQUE.get().defaultBlockState().setValue(PlaqueBlock.FACING, wall));
        PlaqueBlockEntity plaque = helper.getBlockEntity(pos);
        plaque.hang(SealItem.create(new SealData(BuiltInRegistries.ITEM.getKey(item), tier.ordinal(), UUID.randomUUID(), "Tester", rate, 0)));
        return plaque;
    }

    private static Container smelting(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.FURNACE);
        Container furnace = helper.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        return furnace;
    }

    private static int smelted(Container furnace) {
        return furnace.getItem(FURNACE_OUTPUT).getCount();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT, batch = "acceleration_on")
    public static void aFurnaceNextToAPlaqueFinishesBeforeAFarOne(GameTestHelper helper) {
        AuditorConfig.ACCELERATION.set(true);
        plaque(helper, PLAQUE, Direction.DOWN, Items.IRON_INGOT, SealTier.STAR, HIGH_RATE);
        Container near = smelting(helper, NEAR_FURNACE);
        Container far = smelting(helper, FAR_FURNACE);
        helper.runAtTickTime(ACCELERATED_CHECK, () -> {
            AuditorScene.assertEquals("near furnace output", 1, smelted(near));
            AuditorScene.assertEquals("far furnace output", 0, smelted(far));
            helper.succeed();
        });
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT, batch = "acceleration_on")
    public static void anExcellentSealReachesTwoBlocksFurther(GameTestHelper helper) {
        AuditorConfig.ACCELERATION.set(true);
        helper.setBlock(PLAQUE, ModBlocks.PLAQUE.get().defaultBlockState().setValue(PlaqueBlock.FACING, Direction.DOWN));
        PlaqueBlockEntity plaque = helper.getBlockEntity(PLAQUE);
        plaque.hang(SealItem.create(new SealData(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), SealTier.STAR.ordinal(), UUID.randomUUID(),
                "Tester", HIGH_RATE, 0, true)));
        Container beyondPlainRadius = smelting(helper, EXCELLENT_REACH);
        helper.runAtTickTime(ACCELERATED_CHECK, () -> {
            AuditorScene.assertEquals("furnace six blocks away", 1, smelted(beyondPlainRadius));
            helper.succeed();
        });
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT, batch = "acceleration_off")
    public static void withAccelerationOffBothFurnacesFinishTogether(GameTestHelper helper) {
        AuditorConfig.ACCELERATION.set(false);
        plaque(helper, PLAQUE, Direction.DOWN, Items.IRON_INGOT, SealTier.STAR, HIGH_RATE);
        Container near = smelting(helper, NEAR_FURNACE);
        Container far = smelting(helper, FAR_FURNACE);
        helper.runAtTickTime(ACCELERATED_CHECK, () -> {
            AuditorScene.assertEquals("near furnace output", 0, smelted(near));
            AuditorScene.assertEquals("far furnace output", 0, smelted(far));
        });
        helper.runAtTickTime(UNACCELERATED_DONE, () -> {
            AuditorConfig.ACCELERATION.set(true);
            AuditorScene.assertEquals("near furnace output", 1, smelted(near));
            AuditorScene.assertEquals("far furnace output", 1, smelted(far));
            helper.succeed();
        });
    }
}
