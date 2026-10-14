package dev.nostalgiacraft.auditor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorDisplayText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class AuditorRenderer implements BlockEntityRenderer<AuditorBlockEntity> {
    private static final float TEXT_SCALE = 0.025f;
    private static final float UNIT_SCALE = 0.8f;
    private static final float FACE_OFFSET = 0.5001f;
    private static final float ICON_SIZE = 0.22f;
    private static final float ICON_HEIGHT = 0.27f;
    private static final float VALUE_TOP = -4f;
    private static final float UNIT_TOP = 7f;
    private static final int VIEW_DISTANCE = 32;
    private static final int WHITE = 0xFFFFFFFF;
    private final Font font;
    private final ItemRenderer items;

    public AuditorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(AuditorBlockEntity auditor, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        AuditorDisplayText text = AuditorDisplayText.of(auditor);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(FaceRotation.towards(auditor.front()));
        pose.translate(0, 0, FACE_OFFSET);
        renderIcon(auditor, pose, buffers);
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        drawCentered(text.value(), VALUE_TOP, pose, buffers);
        pose.translate(0, UNIT_TOP, 0);
        pose.scale(UNIT_SCALE, UNIT_SCALE, UNIT_SCALE);
        drawCentered(text.caption(), 0, pose, buffers);
        pose.popPose();
    }

    private void renderIcon(AuditorBlockEntity auditor, PoseStack pose, MultiBufferSource buffers) {
        ItemStack icon = auditor.topItem();
        if (icon.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0, ICON_HEIGHT, 0);
        pose.scale(ICON_SIZE, ICON_SIZE, 0.001f);
        items.renderStatic(icon, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers,
                auditor.getLevel(), 0);
        pose.popPose();
    }

    private void drawCentered(Component line, float top, PoseStack pose, MultiBufferSource buffers) {
        String text = line.getString();
        font.drawInBatch(text, -font.width(text) / 2f, top, WHITE, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
