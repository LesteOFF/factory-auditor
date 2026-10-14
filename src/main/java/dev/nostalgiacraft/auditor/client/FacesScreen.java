package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.block.IoConfig;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.monitor.client.screen.WorkshopScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class FacesScreen extends WorkshopScreen {
    private static final int CELL = FaceButton.SIZE + 4;
    private static final int[][] RELATIVE_CELLS = {{1, 0}, {0, 1}, {2, 1}, {1, 2}, {0, 2}};
    private static final int SWATCH = 12;
    private final Screen parent;
    private final AuditorBlockEntity auditor;
    private AuditorSettings shown;

    public FacesScreen(Screen parent, AuditorBlockEntity auditor) {
        super(AuditorTexts.screen("faces.title"), AuditorTexts.screen("faces.help"));
        this.parent = parent;
        this.auditor = auditor;
        this.shown = auditor.settings();
    }

    private int gridLeft() {
        return layout.x() + 8;
    }

    private int legendLeft() {
        return gridLeft() + 3 * CELL + 24;
    }

    @Override
    protected void init() {
        super.init();
        Direction front = auditor.front();
        List<Direction> sides = IoConfig.relativeSides(front);
        for (int index = 0; index < sides.size(); index++) {
            faceButton(sides.get(index), RELATIVE_CELLS[index][0], RELATIVE_CELLS[index][1]);
        }
        FaceButton frontCell = addRenderableWidget(new FaceButton(gridLeft() + CELL, layout.y() + CELL,
                AuditorTexts.screen("side.front"), new ItemStack(ModBlocks.AUDITOR_ITEM.get()), AuditorSprites.FACE_FRONT,
                AuditorTexts.screen("role_short.front").getString(), button -> {
        }));
        frontCell.active = false;
        frontCell.setTooltip(Tooltip.create(AuditorTexts.screen("faces.front")));
        paragraph(AuditorTexts.screen("io.help"), legendLeft(), layout.y() + 52, layout.contentWidth() - legendLeft() + layout.x());
        int half = (layout.contentWidth() - 6) / 2;
        button(AuditorTexts.screen("clear_io"), layout.x(), layout.footerY(), half, () -> AuditorView.send(auditor,
                auditor.settings().withIo(IoConfig.NONE)));
        button(AuditorTexts.screen("back"), layout.x() + half + 6, layout.footerY(), layout.contentWidth() - half - 6, true, false,
                this::onClose);
    }

    private void faceButton(Direction side, int column, int row) {
        AuditorSettings settings = auditor.settings();
        IoConfig.Role role = settings.io().role(side, auditor.front(), settings.transfer());
        ItemStack neighbor = new ItemStack(auditor.getLevel().getBlockState(auditor.getBlockPos().relative(side)).getBlock().asItem());
        FaceButton cell = addRenderableWidget(new FaceButton(gridLeft() + column * CELL, layout.y() + row * CELL,
                AuditorView.sideName(auditor, side), neighbor, AuditorSprites.face(role),
                AuditorTexts.option("role_short", role).getString(), button -> cycle(side, button == 1)));
        MutableComponent tooltip = AuditorView.sideName(auditor, side).append(": ").append(AuditorTexts.option("role", role));
        if (!neighbor.isEmpty()) {
            tooltip.append("\n").append(AuditorTexts.screen("faces.neighbor", neighbor.getHoverName()));
        }
        cell.setTooltip(Tooltip.create(tooltip));
    }

    private void cycle(Direction side, boolean backwards) {
        AuditorSettings settings = auditor.settings();
        AuditorView.send(auditor, settings.withIo(settings.io().cycle(side, auditor.front(), settings.transfer(), backwards)));
    }

    @Override
    public void tick() {
        super.tick();
        if (auditor.isRemoved()) {
            onClose();
        } else if (!auditor.settings().equals(shown)) {
            shown = auditor.settings();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = legendLeft();
        int y = layout.y();
        legend(graphics, x, y, AuditorSprites.FACE_INPUT, AuditorTexts.screen("role.input"));
        legend(graphics, x, y + 14, AuditorSprites.FACE_OUTPUT, AuditorTexts.screen("role.output"));
        legend(graphics, x, y + 28, AuditorSprites.FACE_OFF, AuditorTexts.screen("role.off"));
    }

    private void legend(GuiGraphics graphics, int x, int y, ResourceLocation swatch, Component name) {
        graphics.blitSprite(swatch, x, y, SWATCH, SWATCH);
        graphics.drawString(font, name, x + SWATCH + 5, y + 1, INK, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
