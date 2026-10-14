package dev.nostalgiacraft.auditor.award;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyRefundTest {
    private static final int SECOND = 20;

    @Test
    void nothingIsRefundedBeforeAnyMeasurement() {
        assertEquals(0, new EnergyRefund().due(SECOND, 0.35));
    }

    @Test
    void aMeasuredTickIsRefundedForTheWholeWindow() {
        EnergyRefund refund = new EnergyRefund();
        refund.observe(1000, 800);
        assertEquals(200 * SECOND / 10, refund.due(SECOND, 0.1));
    }

    @Test
    void aWindowWithoutAMeasuredTickRefundsNothing() {
        EnergyRefund refund = new EnergyRefund();
        refund.observe(1000, 800);
        refund.due(SECOND, 0.1);
        assertEquals(0, refund.due(SECOND, 0.1));
    }

    @Test
    void theLatestMeasurementReplacesTheEarlierOne() {
        EnergyRefund refund = new EnergyRefund();
        refund.observe(1000, 800);
        refund.observe(800, 800);
        assertEquals(0, refund.due(SECOND, 0.35));
    }

    @Test
    void energyGainedDuringATickCountsAsNoUse() {
        EnergyRefund refund = new EnergyRefund();
        refund.observe(500, 900);
        assertEquals(0, refund.due(SECOND, 0.35));
    }

    @Test
    void fractionsAccumulateWithoutLoss() {
        EnergyRefund refund = new EnergyRefund();
        int total = 0;
        for (int second = 0; second < 20; second++) {
            refund.observe(3, 0);
            total += refund.due(SECOND, 0.05);
        }
        assertEquals(60, total);
    }
}
