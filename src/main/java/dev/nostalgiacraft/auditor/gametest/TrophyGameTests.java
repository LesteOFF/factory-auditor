package dev.nostalgiacraft.auditor.gametest;

import com.mojang.authlib.GameProfile;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.award.TrophyKind;
import dev.nostalgiacraft.auditor.block.TrophyBlockEntity;
import dev.nostalgiacraft.auditor.board.SealCeremony;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MonitoringCenterAuditor.MODID)
@PrefixGameTestTemplate(false)
public final class TrophyGameTests {
    private static final int BRONZE_LIGHT = 10;
    private static final int GOLD_LIGHT = 15;
    private static final double RATE = 321;

    private TrophyGameTests() {
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void aTrophyInspiresOnceUntilItsCooldownEnds(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "Mock"));
        helper.setBlock(Scenario.CENTER, ModBlocks.GOLD_TROPHY.get());
        TrophyBlockEntity trophy = helper.getBlockEntity(Scenario.CENTER);
        trophy.claim(player.getUUID());
        if (!trophy.inspire(player, TrophyKind.GOLD) || !player.hasEffect(MobEffects.DIG_SPEED) || !player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            throw new GameTestAssertException("the gold trophy did not give haste and speed");
        }
        if (trophy.inspire(player, TrophyKind.GOLD)) {
            throw new GameTestAssertException("the trophy inspired again before its cooldown");
        }
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void anotherTeamCannotUseTheTrophy(GameTestHelper helper) {
        ServerPlayer stranger = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "Stranger"));
        helper.setBlock(Scenario.CENTER, ModBlocks.BRONZE_TROPHY.get());
        TrophyBlockEntity trophy = helper.getBlockEntity(Scenario.CENTER);
        trophy.claim(UUID.randomUUID());
        if (trophy.inspire(stranger, TrophyKind.BRONZE) || stranger.hasEffect(MobEffects.DIG_SPEED)) {
            throw new GameTestAssertException("a stranger used the trophy");
        }
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void trophiesLightTheirSurroundings(GameTestHelper helper) {
        AuditorScene.assertEquals("bronze light", BRONZE_LIGHT, ModBlocks.BRONZE_TROPHY.get().defaultBlockState().getLightEmission());
        AuditorScene.assertEquals("gold light", GOLD_LIGHT, ModBlocks.GOLD_TROPHY.get().defaultBlockState().getLightEmission());
        helper.succeed();
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void aTrophyShowsTheBestRecordOfItsOwner(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        helper.setBlock(Scenario.CENTER, ModBlocks.GOLD_TROPHY.get());
        TrophyBlockEntity trophy = helper.getBlockEntity(Scenario.CENTER);
        trophy.claim(owner);
        helper.runAfterDelay(1, () -> SealCeremony.celebrate(helper.getLevel().getServer(),
                new SealData(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), SealTier.COPPER.ordinal(), owner, "Mock", RATE, 0)));
        helper.succeedWhen(() -> {
            AuditorScene.assertEquals("record rate on the hologram", RATE, trophy.recordRate());
            if (!trophy.recordItem().equals("minecraft:iron_ingot")) {
                throw new GameTestAssertException("hologram item was " + trophy.recordItem());
            }
        });
    }
}
