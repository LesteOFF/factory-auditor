package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.block.AuditorStatus;
import dev.nostalgiacraft.auditor.block.IoConfig;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.auditor.network.AuditorPayloads;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class AuditorView {
    static final int GOOD = 0xFF7BD88F;
    static final int BAD = 0xFFFF6B5E;
    static final int CALM = 0xFF9A9A9A;
    static final String[] RELATIVE_NAMES = {"top", "left", "right", "bottom", "back"};
    private static final int GRAPH_PADDING = 3;
    private static final double HEADROOM = 1.4;

    private AuditorView() {
    }

    static void send(AuditorBlockEntity auditor, AuditorSettings settings) {
        PacketDistributor.sendToServer(new AuditorPayloads.ConfigureAuditor(auditor.getBlockPos(), settings));
    }

    static MutableComponent sideName(AuditorBlockEntity auditor, Direction side) {
        List<Direction> sides = IoConfig.relativeSides(auditor.front());
        int index = sides.indexOf(side);
        return index < 0 ? AuditorTexts.screen("side.front") : AuditorTexts.screen("side." + RELATIVE_NAMES[index]);
    }

    static Component facesSummary(AuditorBlockEntity auditor) {
        AuditorSettings settings = auditor.settings();
        String inputs = names(auditor, settings.io().sides(IoConfig.Role.INPUT, auditor.front(), settings.transfer()));
        String outputs = names(auditor, settings.io().sides(IoConfig.Role.OUTPUT, auditor.front(), settings.transfer()));
        return settings.transfer() == TransferMode.CONSUME ? AuditorTexts.screen("summary.consume", inputs)
                : AuditorTexts.screen("summary.pass", inputs, outputs);
    }

    private static String names(AuditorBlockEntity auditor, List<Direction> sides) {
        return sides.isEmpty() ? AuditorTexts.screen("summary.none").getString()
                : sides.stream().map(side -> sideName(auditor, side).getString()).collect(Collectors.joining(", "));
    }

    static Component statusLine(AuditorBlockEntity auditor) {
        AuditorStatus status = auditor.status();
        if (status != AuditorStatus.DISCONNECTED) {
            return AuditorTexts.status(status);
        }
        AuditorSettings settings = auditor.settings();
        String reason = settings.io().sides(IoConfig.Role.INPUT, auditor.front(), settings.transfer()).isEmpty() ? "no_input"
                : settings.transfer() == TransferMode.CONSUME ? "consume"
                : settings.io().sides(IoConfig.Role.OUTPUT, auditor.front(), settings.transfer()).isEmpty() ? "no_output" : "output_alone";
        return AuditorTexts.status(status).append(": ").append(AuditorTexts.screen("reason." + reason));
    }

    static int statusColor(AuditorStatus status) {
        return switch (status) {
            case TRANSFERRING -> GOOD;
            case DISCONNECTED -> BAD;
            case IDLE -> CALM;
        };
    }

    static String rate(double perMinute) {
        return ValueFormat.number(perMinute);
    }

    static void drawGraph(GuiGraphics graphics, Font font, double[] values, int x, int y, int width, int height) {
        double top = Math.max(1, Arrays.stream(values).max().orElse(0) * HEADROOM);
        graphics.blitSprite(AuditorSprites.GRAPH, x, y, width, height);
        int inner = height - 2 * GRAPH_PADDING;
        int bar = Math.max(AuditorSprites.NINE_SLICE_MINIMUM, (width - 2 * GRAPH_PADDING) / 10);
        for (int index = 0; index < values.length; index++) {
            int filled = (int) Math.round(values[index] / top * inner);
            if (filled >= AuditorSprites.NINE_SLICE_MINIMUM) {
                int left = x + GRAPH_PADDING + index * bar;
                graphics.blitSprite(AuditorSprites.BAR, left, y + height - GRAPH_PADDING - filled, bar - 1, filled);
            }
        }
        graphics.drawString(font, ValueFormat.number(top) + " /min", x + GRAPH_PADDING + 2, y + GRAPH_PADDING + 1, CALM, false);
    }
}
