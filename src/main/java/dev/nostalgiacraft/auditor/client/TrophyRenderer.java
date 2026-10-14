package dev.nostalgiacraft.auditor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nostalgiacraft.auditor.block.TrophyBlockEntity;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public final class TrophyRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private static final float HEIGHT = 1.35f;
    private static final float SCALE = 0.022f;
    private static final int LINE = 10;
    private static final int TITLE_COLOR = 0xFFFFD45A;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x60000000;
    private static final int VIEW_DISTANCE = 16;
    private final Font font;

    public TrophyRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(TrophyBlockEntity trophy, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, HEIGHT, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SCALE, -SCALE, SCALE);
        draw(Component.translatable("hologram.monitoringcenter_auditor.title"), -LINE, TITLE_COLOR, pose, buffers, light);
        draw(recordLine(trophy), 0, TEXT_COLOR, pose, buffers, light);
        pose.popPose();
    }

    private static Component recordLine(TrophyBlockEntity trophy) {
        ResourceLocation item = trophy.recordItem().isEmpty() ? null : ResourceLocation.tryParse(trophy.recordItem());
        if (item == null) {
            return Component.translatable("hologram.monitoringcenter_auditor.empty");
        }
        return Component.translatable("hologram.monitoringcenter_auditor.record", BuiltInRegistries.ITEM.get(item).getDescription(),
                ValueFormat.number(trophy.recordRate()));
    }

    private void draw(Component line, int top, int color, PoseStack pose, MultiBufferSource buffers, int light) {
        float left = -font.width(line) / 2f;
        font.drawInBatch(line, left, top, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, BACKGROUND, light);
    }

    @Override
    public AABB getRenderBoundingBox(TrophyBlockEntity trophy) {
        return new AABB(trophy.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 1, 0);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
