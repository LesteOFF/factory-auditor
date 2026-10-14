package dev.nostalgiacraft.auditor.gametest;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.award.AwardedFactory;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@PrefixGameTestTemplate(false)
public final class IntegrationGameTests {
    private static final List<String> REQUIRED_MODS = List.of("create", "mekanism", "oritech");
    private static final BlockPos AUDITOR = Scenario.CENTER;
    private static final BlockPos CONVEYOR = AUDITOR.above();
    private static final BlockPos SOURCE = CONVEYOR.above();
    private static final BlockPos DESTINATION = AUDITOR.east();
    private static final int ITEMS = 64;
    private static final int BATCH = 8;
    private static final int REFUSED = 16;
    private static final int SETTLE_TICKS = 100;
    private static final int TIMEOUT = 800;
    private static final int ORITECH_EXTRACT = 2;
    private static final int CHARGE = 100_000;
    private static final int CHARGE_ATTEMPTS = 1000;
    private static final int SWAP_CHECK_TICKS = 380;

    private IntegrationGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        if (REQUIRED_MODS.stream().allMatch(ModList.get()::isLoaded)) {
            event.register(IntegrationGameTests.class);
        }
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void createChuteDeliversEveryItemThroughTheAuditor(GameTestHelper helper) {
        fedConveyorDeliversEverything(helper, "create:chute");
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void createChuteFacingAFullDestination(GameTestHelper helper) {
        fullDestinationIsRefused(helper, "create:chute");
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void mekanismTransporterDeliversEveryItemThroughTheAuditor(GameTestHelper helper) {
        fedConveyorDeliversEverything(helper, "mekanism:basic_logistical_transporter");
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void mekanismTransporterFacingAFullDestination(GameTestHelper helper) {
        fullDestinationIsRefused(helper, "mekanism:basic_logistical_transporter");
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void mekanismTransporterCarriesItemsAwayFromAnOutputFace(GameTestHelper helper) {
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        place(helper, DESTINATION, "mekanism:basic_logistical_transporter");
        Container destination = AuditorScene.chest(helper, DESTINATION.east());
        int[] sent = {0};
        helper.onEachTick(() -> {
            if (sent[0] < REFUSED) {
                sent[0] += AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, REFUSED - sent[0]));
            }
        });
        helper.succeedWhen(() -> {
            AuditorScene.assertEquals("destination", REFUSED, AuditorScene.count(destination));
            AuditorScene.assertEquals("auditor total", REFUSED, auditor.total());
        });
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = TIMEOUT)
    public static void oritechPipeDeliversEveryItemThroughTheAuditor(GameTestHelper helper) {
        place(helper, CONVEYOR, "oritech:item_pipe");
        Container source = AuditorScene.chest(helper, SOURCE);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, ITEMS));
        Container destination = AuditorScene.chest(helper, DESTINATION);
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        helper.runAfterDelay(1, () -> extractFrom(helper, CONVEYOR, Direction.UP));
        helper.succeedWhen(() -> assertDelivered(destination, auditor));
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT,
            batch = "acceleration_on")
    public static void aPlaqueRefundsEnergyToAWorkingMachineButNeverToABattery(GameTestHelper helper) {
        AuditorConfig.ACCELERATION.set(true);
        AwardGameTests.plaque(helper, Scenario.CENTER, Direction.DOWN, Items.IRON_INGOT, SealTier.STAR, 1200);
        BlockPos machine = Scenario.CENTER.east();
        BlockPos battery = Scenario.CENTER.west();
        place(helper, machine, "oritech:pulverizer_block");
        place(helper, battery, "mekanism:basic_energy_cube");
        charge(helper, machine);
        charge(helper, battery);
        helper.onEachTick(() -> feedMachine(helper, machine));
        long[] beforeSwap = {-1};
        helper.onEachTick(() -> {
            long refunded = AwardedFactory.refundedAt(helper.getLevel(), helper.absolutePos(machine));
            if (beforeSwap[0] < 0 && refunded > 0) {
                beforeSwap[0] = refunded;
                place(helper, machine, "oritech:small_storage_block");
                charge(helper, machine);
            }
        });
        helper.runAfterDelay(SWAP_CHECK_TICKS, () -> {
            if (beforeSwap[0] <= 0) {
                throw new GameTestAssertException("the working machine got no energy back");
            }
            AuditorScene.assertEquals("energy refunded after the machine became a battery", beforeSwap[0],
                    AwardedFactory.refundedAt(helper.getLevel(), helper.absolutePos(machine)));
            AuditorScene.assertEquals("energy refunded to the cube", 0, AwardedFactory.refundedAt(helper.getLevel(), helper.absolutePos(battery)));
            helper.succeed();
        });
    }

    private static void charge(GameTestHelper helper, BlockPos pos) {
        IEnergyStorage storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        if (storage == null) {
            throw new GameTestAssertException("no energy storage at " + pos);
        }
        int accepted;
        int attempts = 0;
        do {
            accepted = storage.receiveEnergy(CHARGE, false);
        } while (accepted > 0 && ++attempts < CHARGE_ATTEMPTS);
    }

    private static void feedMachine(GameTestHelper helper, BlockPos pos) {
        for (Direction side : Direction.values()) {
            IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
            for (int slot = 0; items != null && slot < items.getSlots(); slot++) {
                if (items.insertItem(slot, new ItemStack(Items.RAW_IRON, 8), false).isEmpty()) {
                    return;
                }
            }
        }
    }

    @GameTest(templateNamespace = MonitoringCenterAuditor.MODID, template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void createListsTheAuditorAsADisplaySource(GameTestHelper helper) {
        Registry<?> sources = BuiltInRegistries.REGISTRY.get(ResourceLocation.parse("create:display_source"));
        if (sources == null || !sources.containsKey(MonitoringCenterAuditor.id("auditor"))) {
            throw new GameTestAssertException("the Auditor is not a Create display source");
        }
        helper.succeed();
    }

    private static void extractFrom(GameTestHelper helper, BlockPos pipe, Direction side) {
        BlockState state = helper.getBlockState(pipe);
        Property<?> property = state.getBlock().getStateDefinition().getProperty(side.getSerializedName());
        if (!(property instanceof IntegerProperty connection) || !connection.getPossibleValues().contains(ORITECH_EXTRACT)) {
            throw new GameTestAssertException("pipe did not connect towards " + side + ": " + state);
        }
        helper.setBlock(pipe, state.setValue(connection, ORITECH_EXTRACT));
    }

    private static void fedConveyorDeliversEverything(GameTestHelper helper, String conveyorId) {
        place(helper, CONVEYOR, conveyorId);
        Container destination = AuditorScene.chest(helper, DESTINATION);
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        int[] sent = {0};
        helper.onEachTick(() -> {
            if (sent[0] < ITEMS) {
                sent[0] += feed(helper, Math.min(BATCH, ITEMS - sent[0]));
            }
        });
        helper.succeedWhen(() -> assertDelivered(destination, auditor));
    }

    private static void fullDestinationIsRefused(GameTestHelper helper, String conveyorId) {
        place(helper, CONVEYOR, conveyorId);
        Container destination = AuditorScene.chest(helper, DESTINATION);
        for (int slot = 0; slot < destination.getContainerSize(); slot++) {
            destination.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.PASS, Direction.EAST);
        int[] sent = {0};
        helper.onEachTick(() -> {
            if (sent[0] < REFUSED) {
                sent[0] += feed(helper, REFUSED - sent[0]);
            }
        });
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            MonitoringCenterAuditor.LOGGER.info("[pack] {} facing a full destination: sent {}, held {}, dropped {}", conveyorId, sent[0],
                    held(helper), dropped(helper));
            AuditorScene.assertEquals("auditor total while full", 0, auditor.total());
            AuditorScene.assertEquals("destination while full", destination.getContainerSize() * 64, AuditorScene.count(destination));
            AuditorScene.assertEquals("items dropped", 0, dropped(helper));
            AuditorScene.assertEquals("items held by the conveyor", sent[0], held(helper));
            destination.clearContent();
        });
        helper.succeedWhen(() -> {
            AuditorScene.assertEquals("destination once freed", REFUSED, AuditorScene.count(destination));
            AuditorScene.assertEquals("auditor total once freed", REFUSED, auditor.total());
        });
    }

    private static void assertDelivered(Container destination, AuditorBlockEntity auditor) {
        AuditorScene.assertEquals("destination", ITEMS, AuditorScene.count(destination));
        AuditorScene.assertEquals("auditor total", ITEMS, auditor.total());
    }

    private static void place(GameTestHelper helper, BlockPos pos, String blockId) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        if (block == Blocks.AIR) {
            throw new GameTestAssertException("missing block " + blockId);
        }
        helper.setBlock(pos, block);
    }

    private static int feed(GameTestHelper helper, int count) {
        IItemHandler conveyor = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CONVEYOR), Direction.UP);
        if (conveyor == null) {
            throw new GameTestAssertException("conveyor exposes no item handler from above");
        }
        ItemStack stack = new ItemStack(Items.COBBLESTONE, count);
        for (int slot = 0; slot < conveyor.getSlots() && !stack.isEmpty(); slot++) {
            stack = conveyor.insertItem(slot, stack, false);
        }
        return count - stack.getCount();
    }

    private static int held(GameTestHelper helper) {
        IItemHandler conveyor = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CONVEYOR), Direction.UP);
        int held = 0;
        for (int slot = 0; conveyor != null && slot < conveyor.getSlots(); slot++) {
            held += conveyor.getStackInSlot(slot).getCount();
        }
        return held;
    }

    private static int dropped(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).stream().mapToInt(item -> ((ItemEntity) item).getItem().getCount()).sum();
    }
}
