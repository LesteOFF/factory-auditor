package dev.nostalgiacraft.auditor.block;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistributionTest {
    private static final class Bin implements Distribution.Outlet {
        private final int capacity;
        private int stored;

        Bin(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int offer(int count, boolean simulate) {
            int accepted = Math.min(count, capacity - stored);
            if (!simulate) {
                stored += accepted;
            }
            return accepted;
        }
    }

    @Test
    void splitSharesEqually() {
        Bin left = new Bin(100);
        Bin right = new Bin(100);
        assertEquals(10, Distribution.split(10, List.of(left, right), 0, false));
        assertEquals(5, left.stored);
        assertEquals(5, right.stored);
    }

    @Test
    void splitSendsTheIndivisibleRestStartingAtTheGivenOutlet() {
        Bin left = new Bin(100);
        Bin right = new Bin(100);
        Distribution.split(3, List.of(left, right), 1, false);
        assertEquals(1, left.stored);
        assertEquals(2, right.stored);
    }

    @Test
    void singleItemsAlternateWhenTheStartRotates() {
        Bin left = new Bin(100);
        Bin right = new Bin(100);
        List<Distribution.Outlet> outlets = List.of(left, right);
        for (int item = 0; item < 128; item++) {
            Distribution.split(1, outlets, item % 2, false);
        }
        assertEquals(64, left.stored);
        assertEquals(64, right.stored);
    }

    @Test
    void splitRedistributesWhatAnOutletRefuses() {
        Bin small = new Bin(2);
        Bin large = new Bin(100);
        assertEquals(10, Distribution.split(10, List.of(small, large), 0, false));
        assertEquals(2, small.stored);
        assertEquals(8, large.stored);
    }

    @Test
    void splitReportsOnlyWhatWasTaken() {
        assertEquals(3, Distribution.split(10, List.of(new Bin(1), new Bin(2)), 0, false));
    }

    @Test
    void simulationStoresNothing() {
        Bin bin = new Bin(100);
        assertEquals(10, Distribution.split(10, List.of(bin), 0, true));
        assertEquals(0, bin.stored);
    }

    @Test
    void inOrderFillsTheFirstOutletBeforeTheNext() {
        Bin first = new Bin(6);
        Bin second = new Bin(100);
        assertEquals(10, Distribution.inOrder(10, List.of(first, second), false));
        assertEquals(6, first.stored);
        assertEquals(4, second.stored);
    }
}
