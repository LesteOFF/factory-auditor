package dev.nostalgiacraft.auditor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nostalgiacraft.auditor.block.PlaqueBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PlaqueRenderer implements BlockEntityRenderer<PlaqueBlockEntity> {
    private static final float SURFACE = -0.5f + 1.1f / 16;
    private static final float SEAL_SIZE = 0.45f;
    private static final float FLAT = 0.01f;
    private final ItemRenderer items;

    public PlaqueRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(PlaqueBlockEntity plaque, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack seal = plaque.seal();
        if (seal.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(FaceRotation.towards(plaque.wall().getOpposite()));
        pose.translate(0, 0, SURFACE);
        pose.scale(SEAL_SIZE, SEAL_SIZE, FLAT);
        items.renderStatic(seal, ItemDisplayContext.GUI, light, overlay, pose, buffers, plaque.getLevel(), 0);
        pose.popPose();
    }
}
