package dev.nostalgiacraft.auditor.gametest;

import com.mojang.authlib.GameProfile;
import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.board.SealCeremony;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.gametest.Scenario;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MonitoringCenterAuditor.MODID)
@PrefixGameTestTemplate(false)
public final class BoardGameTests {
    private BoardGameTests() {
    }

    @GameTest(template = Scenario.EMPTY, timeoutTicks = Scenario.TIMEOUT)
    public static void theTabShowsMedalSealCountAndTitle(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "Mock"));
        SealCeremony.celebrate(helper.getLevel().getServer(), new SealData(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT),
                SealTier.GOLD.ordinal(), player.getUUID(), "Mock", 100, 0));
        String shown = player.getTabListDisplayName() == null ? "" : player.getTabListDisplayName().getString();
        if (!shown.contains(String.valueOf(SealTier.GOLD.medal())) || !shown.contains("x1")) {
            throw new GameTestAssertException("tab name lacks medal or seal count: " + shown);
        }
        helper.succeed();
    }
}
