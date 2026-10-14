package dev.nostalgiacraft.auditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class MedalButton extends AbstractButton {
    static final int SIZE = 22;
    private static final int WHITE = 0xFFFFFFFF;
    private final Component medal;
    private final boolean selected;
    private final Runnable pressed;

    MedalButton(int x, int y, Component medal, Component name, boolean selected, Runnable pressed) {
        super(x, y, SIZE, SIZE, name);
        this.medal = medal;
        this.selected = selected;
        this.pressed = pressed;
    }

    @Override
    public void onPress() {
        pressed.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation slot = selected ? AuditorSprites.MEDAL_SLOT_SELECTED
                : isHoveredOrFocused() ? AuditorSprites.MEDAL_SLOT_HOVER : AuditorSprites.MEDAL_SLOT;
        graphics.blitSprite(slot, getX(), getY(), width, height);
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + (width - 16) / 2f, getY() + (height - 16) / 2f, 0);
        graphics.pose().scale(2, 2, 1);
        graphics.drawString(font, medal, 0, 0, WHITE, false);
        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
