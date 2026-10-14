package dev.nostalgiacraft.auditor.board;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsTest {
    private static final String IRON = "minecraft:iron_ingot";
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);

    @Test
    void aSealCountsForItsOwner() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 2, 100);
        standings.record(ALICE, "Alice", IRON, 1, 80);
        assertEquals(2, standings.seals(ALICE));
        assertEquals(2, standings.bestTier(ALICE));
        assertEquals(2, standings.bestTier(ALICE, IRON));
        assertEquals(-1, standings.bestTier(BOB, IRON));
    }

    @Test
    void theFirstSealOfAnItemTakesTheRecord() {
        Standings.Outcome outcome = new Standings().record(ALICE, "Alice", IRON, 0, 100);
        assertTrue(outcome.newHolder());
        assertNull(outcome.previousHolder());
    }

    @Test
    void aFasterOwnerTakesTheRecordFromTheHolder() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 0, 100);
        Standings.Outcome outcome = standings.record(BOB, "Bob", IRON, 0, 150);
        assertTrue(outcome.newHolder());
        assertEquals(ALICE, outcome.previousHolder());
        assertEquals(BOB, standings.holder(IRON));
    }

    @Test
    void anEqualRateDoesNotTakeTheRecord() {
        Standings standings = new Standings();
        UUID late = new UUID(0, 0);
        standings.record(ALICE, "Alice", IRON, 0, 150);
        assertFalse(standings.record(late, "Late", IRON, 0, 150).newHolder());
        assertEquals(ALICE, standings.holder(IRON));
        assertEquals("Alice", standings.top(IRON, 1).get(0).name());
    }

    @Test
    void theHolderBeatingItselfIsNotANewHolder() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 0, 100);
        assertFalse(standings.record(ALICE, "Alice", IRON, 1, 400).newHolder());
    }

    @Test
    void aSlowerSealKeepsTheOwnersBestRate() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 0, 100);
        standings.record(ALICE, "Alice", IRON, 0, 50);
        assertEquals(100, standings.top(IRON, 5).get(0).rate(), 1e-9);
    }

    @Test
    void topListsOwnersFromFastestToSlowest() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 0, 100);
        standings.record(BOB, "Bob", IRON, 0, 300);
        List<Standings.Entry> top = standings.top(IRON, 1);
        assertEquals(1, top.size());
        assertEquals("Bob", top.get(0).name());
    }

    @Test
    void theBestEntryOfAnOwnerIsItsFastestCertifiedRate() {
        Standings standings = new Standings();
        standings.record(ALICE, "Alice", IRON, 0, 100);
        standings.record(ALICE, "Alice", "minecraft:gold_ingot", 0, 300);
        assertEquals("minecraft:gold_ingot", standings.best(ALICE).orElseThrow().item());
        assertEquals(300, standings.best(ALICE).orElseThrow().rate(), 1e-9);
        assertTrue(standings.best(BOB).isEmpty());
    }

    @Test
    void titlesGrowWithSealsAndMonopolyNeedsFiveRecords() {
        Standings standings = new Standings();
        assertEquals(Title.NONE, standings.title(ALICE));
        for (int item = 0; item < Title.MONOPOLY_RECORDS; item++) {
            standings.record(ALICE, "Alice", "minecraft:item_" + item, 0, 10);
        }
        assertEquals(Title.MONOPOLIST, standings.title(ALICE));
        standings.record(BOB, "Bob", "minecraft:item_0", 0, 99);
        assertEquals(Title.SUPPLIER, standings.title(ALICE));
    }
}
