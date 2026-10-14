package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.IoConfig;
import net.minecraft.resources.ResourceLocation;

final class AuditorSprites {
    static final ResourceLocation FACE_INPUT = sprite("face_input");
    static final ResourceLocation FACE_OUTPUT = sprite("face_output");
    static final ResourceLocation FACE_OFF = sprite("face_off");
    static final ResourceLocation FACE_FRONT = sprite("face_front");
    static final ResourceLocation FACE_HOVER = sprite("face_hover");
    static final ResourceLocation MEDAL_SLOT = sprite("medal_slot");
    static final ResourceLocation MEDAL_SLOT_HOVER = sprite("medal_slot_hover");
    static final ResourceLocation MEDAL_SLOT_SELECTED = sprite("medal_slot_selected");
    static final ResourceLocation GRAPH = sprite("graph");
    static final ResourceLocation BAR = sprite("bar");
    static final ResourceLocation PROGRESS = sprite("progress");
    static final ResourceLocation PROGRESS_FILL = sprite("progress_fill");
    static final ResourceLocation PROGRESS_DONE = sprite("progress_done");
    static final ResourceLocation STEP_DONE = sprite("step_done");
    static final ResourceLocation STEP_MISSING = sprite("step_missing");
    static final ResourceLocation FIELD_INVALID = sprite("field_invalid");
    static final int NINE_SLICE_MINIMUM = 4;
    static final int STEP_SIZE = 9;

    private AuditorSprites() {
    }

    private static ResourceLocation sprite(String name) {
        return MonitoringCenterAuditor.id("auditor/" + name);
    }

    static ResourceLocation face(IoConfig.Role role) {
        return switch (role) {
            case INPUT -> FACE_INPUT;
            case OUTPUT -> FACE_OUTPUT;
            case OFF -> FACE_OFF;
        };
    }
}
