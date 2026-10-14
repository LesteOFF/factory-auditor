package dev.nostalgiacraft.auditor.block;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class IoConfigTest {
    private static final Direction FRONT = Direction.NORTH;

    @Test
    void frontNeverBecomesInputOrOutput() {
        IoConfig config = IoConfig.NONE.cycle(FRONT, FRONT, TransferMode.PASS, false);
        assertSame(IoConfig.NONE, config);
        assertEquals(IoConfig.Role.OFF, new IoConfig(1 << FRONT.ordinal() * 5).role(FRONT, FRONT, TransferMode.PASS));
    }

    @Test
    void cyclingGoesOffInputOutputOff() {
        IoConfig input = IoConfig.NONE.cycle(Direction.UP, FRONT, TransferMode.PASS, false);
        IoConfig output = input.cycle(Direction.UP, FRONT, TransferMode.PASS, false);
        IoConfig off = output.cycle(Direction.UP, FRONT, TransferMode.PASS, false);
        assertEquals(IoConfig.Role.INPUT, input.role(Direction.UP, FRONT, TransferMode.PASS));
        assertEquals(IoConfig.Role.OUTPUT, output.role(Direction.UP, FRONT, TransferMode.PASS));
        assertEquals(IoConfig.Role.OFF, off.role(Direction.UP, FRONT, TransferMode.PASS));
    }

    @Test
    void cyclingBackwardsGoesOffOutput() {
        IoConfig output = IoConfig.NONE.cycle(Direction.UP, FRONT, TransferMode.PASS, true);
        assertEquals(IoConfig.Role.OUTPUT, output.role(Direction.UP, FRONT, TransferMode.PASS));
    }

    @Test
    void consumeForbidsOutputs() {
        IoConfig output = IoConfig.NONE.cycle(Direction.UP, FRONT, TransferMode.PASS, true);
        assertEquals(IoConfig.Role.OFF, output.role(Direction.UP, FRONT, TransferMode.CONSUME));
        assertEquals(List.of(), output.sides(IoConfig.Role.OUTPUT, FRONT, TransferMode.CONSUME));
    }

    @Test
    void consumeCyclingSkipsOutput() {
        IoConfig input = IoConfig.NONE.cycle(Direction.UP, FRONT, TransferMode.CONSUME, false);
        IoConfig off = input.cycle(Direction.UP, FRONT, TransferMode.CONSUME, false);
        assertEquals(IoConfig.Role.OFF, off.role(Direction.UP, FRONT, TransferMode.CONSUME));
    }

    @Test
    void garbageBitsDecodeAsOff() {
        IoConfig garbage = new IoConfig(-1);
        assertEquals(IoConfig.Role.OFF, garbage.role(Direction.UP, FRONT, TransferMode.PASS));
    }

    @Test
    void relativeSidesOfAHorizontalFront() {
        assertEquals(List.of(Direction.UP, Direction.EAST, Direction.WEST, Direction.DOWN, Direction.SOUTH),
                IoConfig.relativeSides(Direction.NORTH));
    }
}
