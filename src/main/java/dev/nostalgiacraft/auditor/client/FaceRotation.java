package dev.nostalgiacraft.auditor.client;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

final class FaceRotation {
    private FaceRotation() {
    }

    static Quaternionf towards(Direction front) {
        return switch (front) {
            case UP -> Axis.XP.rotationDegrees(-90);
            case DOWN -> Axis.XP.rotationDegrees(90);
            default -> Axis.YP.rotationDegrees(-front.toYRot());
        };
    }
}
