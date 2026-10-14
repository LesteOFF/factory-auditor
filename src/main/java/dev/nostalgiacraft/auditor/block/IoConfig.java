package dev.nostalgiacraft.auditor.block;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public record IoConfig(int bits) {
    public static final IoConfig NONE = new IoConfig(0);
    private static final int BITS_PER_SIDE = 5;
    private static final int ROLE_MASK = 0b11;

    public enum Role {
        OFF,
        INPUT,
        OUTPUT
    }

    public Role role(Direction side, Direction front, TransferMode mode) {
        if (side == front) {
            return Role.OFF;
        }
        Role stored = storedRole(side);
        return stored == Role.OUTPUT && mode == TransferMode.CONSUME ? Role.OFF : stored;
    }

    public IoConfig cycle(Direction side, Direction front, TransferMode mode, boolean backwards) {
        if (side == front) {
            return this;
        }
        Role[] roles = mode == TransferMode.CONSUME ? new Role[]{Role.OFF, Role.INPUT} : Role.values();
        int current = Math.max(0, List.of(roles).indexOf(role(side, front, mode)));
        int step = backwards ? roles.length - 1 : 1;
        return withSlot(side, roles[(current + step) % roles.length].ordinal());
    }

    public List<Direction> sides(Role role, Direction front, TransferMode mode) {
        List<Direction> sides = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (role(side, front, mode) == role) {
                sides.add(side);
            }
        }
        return sides;
    }

    public static List<Direction> relativeSides(Direction front) {
        if (front.getAxis() == Direction.Axis.Y) {
            return List.of(Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH, front.getOpposite());
        }
        return List.of(Direction.UP, front.getClockWise(), front.getCounterClockWise(), Direction.DOWN, front.getOpposite());
    }

    private Role storedRole(Direction side) {
        int raw = slot(side) & ROLE_MASK;
        return raw < Role.values().length ? Role.values()[raw] : Role.OFF;
    }

    private int slot(Direction side) {
        return bits >>> side.ordinal() * BITS_PER_SIDE & (1 << BITS_PER_SIDE) - 1;
    }

    private IoConfig withSlot(Direction side, int slot) {
        int shift = side.ordinal() * BITS_PER_SIDE;
        return new IoConfig(bits & ~((1 << BITS_PER_SIDE) - 1 << shift) | slot << shift);
    }
}
