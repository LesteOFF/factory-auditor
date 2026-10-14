package dev.nostalgiacraft.auditor.gametest;

import dev.nostalgiacraft.auditor.AuditorMetrics;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.monitor.block.SensorBlockEntity;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import dev.nostalgiacraft.monitor.metric.Metric;
import dev.nostalgiacraft.monitor.metric.Metrics;
import dev.nostalgiacraft.monitor.metric.Sample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MonitoringCenterAuditor.MODID)
@PrefixGameTestTemplate(false)
public final class MeasureGameTests {
    private static final BlockPos AUDITOR = Scenario.CENTER;
    private static final BlockPos EAST = AUDITOR.east();
    private static final BlockPos WEST = AUDITOR.west();
    private static final BlockPos SENSOR = AUDITOR.south();
    private static final int STACK = 64;
    private static final int ITEMS = 2 * STACK;
    private static final double RATE_OF_ONE_BATCH_PER_INTERVAL = ITEMS * 1200.0 / 20;
    private static final int CONSUME_PER_TICK = 218;
    private static final int CONSUME_TICKS = 100;
    private static final int IN_FLIGHT = 100_000;

    private MeasureGameTests() {
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void everyMeasuredItemReachesTheChest(GameTestHelper helper) {
        Container chest = AuditorScene.chest(helper, EAST);
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, STACK));
        AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, STACK));
        helper.succeedWhen(() -> {
            AuditorScene.assertEquals("items in chest", ITEMS, AuditorScene.count(chest));
            AuditorScene.assertEquals("total", ITEMS, auditor.total());
            AuditorScene.assertEquals("rate", RATE_OF_ONE_BATCH_PER_INTERVAL, auditor.ratePerMinute());
        });
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void aFullDestinationRefusesTheInsertion(GameTestHelper helper) {
        Container chest = AuditorScene.chest(helper, EAST);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, STACK));
        }
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        int accepted = AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, STACK));
        AuditorScene.assertEquals("accepted", 0, accepted);
        AuditorScene.assertEquals("total", 0, auditor.total());
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void consumeTakesTwoHundredEighteenItemsEveryTick(GameTestHelper helper) {
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.CONSUME);
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (ticks[0]++ < CONSUME_TICKS) {
                int accepted = AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, 64))
                        + AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, 64))
                        + AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, 64))
                        + AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, CONSUME_PER_TICK - 3 * 64));
                AuditorScene.assertEquals("accepted this tick", CONSUME_PER_TICK, accepted);
            }
        });
        helper.runAfterDelay(CONSUME_TICKS + 1, () -> {
            AuditorScene.assertEquals("total", (double) CONSUME_PER_TICK * CONSUME_TICKS, auditor.total());
            AuditorScene.assertEquals("rate", CONSUME_PER_TICK * 1200.0, auditor.ratePerMinute());
            helper.succeed();
        });
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void everyInputSlotTakesAFullStackSoTransportersCanKeepThousandsOfItemsInFlight(GameTestHelper helper) {
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.CONSUME);
        IItemHandler input = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AUDITOR), AuditorScene.INPUT);
        if (input == null || input.getSlots() * STACK < IN_FLIGHT) {
            throw new GameTestAssertException("input must announce room for " + IN_FLIGHT + " items in flight");
        }
        int lastSlot = input.getSlots() - 1;
        AuditorScene.assertEquals("simulated in the last slot", 0, input.insertItem(lastSlot, new ItemStack(Items.COBBLESTONE, STACK), true).getCount());
        AuditorScene.assertEquals("inserted in the last slot", 0, input.insertItem(lastSlot, new ItemStack(Items.COBBLESTONE, STACK), false).getCount());
        AuditorScene.assertEquals("total", STACK, auditor.total());
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void outputFacesOfferAClosedInventoryForCablesToConnect(GameTestHelper helper) {
        AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        IItemHandler output = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AUDITOR), Direction.EAST);
        IItemHandler off = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AUDITOR), Direction.WEST);
        if (output == null || off != null) {
            throw new GameTestAssertException("output face must expose an inventory and an off face must not");
        }
        ItemStack offered = new ItemStack(Items.COBBLESTONE, STACK);
        AuditorScene.assertEquals("accepted through an output face", 0, STACK - output.insertItem(0, offered, false).getCount());
        AuditorScene.assertEquals("extracted from an output face", 0, output.extractItem(0, STACK, false).getCount());
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void splitSharesEquallyBetweenTwoChests(GameTestHelper helper) {
        Container east = AuditorScene.chest(helper, EAST);
        Container west = AuditorScene.chest(helper, WEST);
        AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST, Direction.WEST);
        for (int item = 0; item < ITEMS; item++) {
            AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE));
        }
        AuditorScene.assertEquals("east chest", STACK, AuditorScene.count(east));
        AuditorScene.assertEquals("west chest", STACK, AuditorScene.count(west));
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void centralSensorListsAndReadsTheAuditRate(GameTestHelper helper) {
        AuditorScene.chest(helper, EAST);
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        SensorBlockEntity sensor = new Scenario(helper).sensorFacing(SENSOR, Direction.NORTH);
        AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, STACK));
        helper.succeedWhen(() -> {
            if (auditor.ratePerMinute() <= 0) {
                throw new GameTestAssertException("auditor has not closed an interval yet");
            }
            boolean listed = Metrics.applicable(helper.getLevel(), sensor).stream().map(Metric::id).anyMatch(AuditorMetrics.AUDIT_RATE::equals);
            if (!listed) {
                throw new GameTestAssertException("audit_rate is not offered by the sensor");
            }
            Sample sample = Metrics.get(AuditorMetrics.AUDIT_RATE).reader().read(helper.getLevel(), sensor);
            AuditorScene.assertEquals("sensor reading", auditor.ratePerMinute(), sample == null ? Double.NaN : sample.value());
        });
    }
}
