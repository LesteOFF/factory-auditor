package dev.nostalgiacraft.auditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntConsumer;

final class FaceButton extends AbstractButton {
    static final int SIZE = 32;
    private static final int BADGE_INSET = 4;
    private static final int TEXT = 0xFFFFFFFF;
    private final ItemStack neighbor;
    private final ResourceLocation frame;
    private final String badge;
    private final IntConsumer pressed;
    private int lastButton;

    FaceButton(int x, int y, Component name, ItemStack neighbor, ResourceLocation frame, String badge, IntConsumer pressed) {
        super(x, y, SIZE, SIZE, name);
        this.neighbor = neighbor;
        this.frame = frame;
        this.badge = badge;
        this.pressed = pressed;
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastButton = button;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress() {
        pressed.accept(lastButton);
        lastButton = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(active && isHoveredOrFocused() ? AuditorSprites.FACE_HOVER : frame, getX(), getY(), width, height);
        if (!neighbor.isEmpty()) {
            graphics.renderItem(neighbor, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        }
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, badge, getX() + width - BADGE_INSET - font.width(badge), getY() + BADGE_INSET, TEXT, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
