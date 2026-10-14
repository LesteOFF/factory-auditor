package dev.nostalgiacraft.auditor.gametest;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.award.MilestonePrizes;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.auditor.diploma.Perks;
import dev.nostalgiacraft.auditor.goal.AuditGoals;
import dev.nostalgiacraft.auditor.rate.CertificationRun;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.data.MonitorNetwork;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MonitoringCenterAuditor.MODID)
@PrefixGameTestTemplate(false)
public final class CertificationGameTests {
    private static final BlockPos AUDITOR = Scenario.CENTER;
    private static final int STACK = 64;
    private static final int STACKS_ABOVE_THE_COBBLESTONE_GOAL = 4;
    private static final int ONE_MINUTE_RUN = CertificationRun.TICKS_PER_MINUTE + 100;

    private CertificationGameTests() {
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = ONE_MINUTE_RUN)
    public static void consumingCertifiesAndLeavesASealInTheTray(GameTestHelper helper) {
        Container chest = AuditorScene.chest(helper, AUDITOR.east());
        AuditorBlockEntity auditor = AuditorScene.place(helper, AUDITOR, TransferMode.CONSUME);
        UUID owner = UUID.randomUUID();
        if (!auditor.startCertification(owner, "Tester", new ItemStack(Items.COBBLESTONE), SealTier.COPPER, new Perks(1, 0, false))) {
            throw new GameTestAssertException("certification did not start in Consume");
        }
        for (int stack = 0; stack < STACKS_ABOVE_THE_COBBLESTONE_GOAL; stack++) {
            AuditorScene.insert(helper, AUDITOR, new ItemStack(Items.COBBLESTONE, STACK));
        }
        helper.succeedWhen(() -> {
            AuditorScene.assertEquals("tray with seal and first milestone prize", 2, auditor.traySize());
            AuditorScene.assertEquals("chest", 0, AuditorScene.count(chest));
            Standings standings = Leaderboard.get(helper.getLevel().getServer()).standings();
            AuditorScene.assertEquals("seals on the leaderboard", 1, standings.seals(owner));
            AuditorScene.assertEquals("certified tier", SealTier.COPPER.ordinal(), standings.bestTier(owner, "minecraft:cobblestone"));
            if (MonitorNetwork.get(helper.getLevel().getServer().overworld()).inbox().peek(owner).isEmpty()) {
                throw new GameTestAssertException("the offline owner got no note in the Central inbox");
            }
        });
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void aSealCarriesTheAuditedItemAndTier(GameTestHelper helper) {
        SealData data = new SealData(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), SealTier.GOLD.ordinal(),
                UUID.randomUUID(), "Tester", 120, 0);
        ItemStack seal = SealItem.create(data);
        SealData read = SealItem.data(seal);
        if (read == null || read.sealTier() != SealTier.GOLD || read.auditedItem() != Items.IRON_INGOT) {
            throw new GameTestAssertException("seal component did not round-trip: " + read);
        }
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void everyMilestonePrizePointsToAValidItem(GameTestHelper helper) {
        if (!MilestonePrizes.problems().isEmpty()) {
            throw new GameTestAssertException("invalid milestone prizes: " + MilestonePrizes.problems());
        }
        AuditorScene.assertEquals("milestone prizes", 5, MilestonePrizes.current().ordered().size());
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void everyAuditGoalPointsToAValidItem(GameTestHelper helper) {
        if (!AuditGoals.problems().isEmpty()) {
            throw new GameTestAssertException("invalid audit goals: " + AuditGoals.problems());
        }
        AuditorScene.assertEquals("cobblestone goal", 240, AuditGoals.goalFor(new ItemStack(Items.COBBLESTONE)).base());
        AuditorScene.assertEquals("iron ingot goal (tag)", 60, AuditGoals.goalFor(new ItemStack(Items.IRON_INGOT)).base());
        AuditorScene.assertEquals("fallback goal", 32, AuditGoals.goalFor(new ItemStack(Items.DIRT)).base());
        helper.succeed();
    }
}
