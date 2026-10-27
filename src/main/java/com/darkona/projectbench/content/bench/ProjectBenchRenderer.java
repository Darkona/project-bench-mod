package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.foundation.BenchBlock;
import com.darkona.projectbench.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import java.util.List;

public final class ProjectBenchRenderer implements BlockEntityRenderer<ProjectBenchBlockEntity> {
    private static final float STEP = 3F / 16F;
    private static final float FLAT = 0.17F;
    private static final float RESULT_SIZE = 0.5F;

    private static final float HOVER = 0.38F;
    private static final Quaternionf[] FACING = new Quaternionf[4];
    private static final Quaternionf FLAT_TURN = Axis.XP.rotationDegrees(-90);

    static {
        for (int i = 0; i < FACING.length; ++i) {
            FACING[i] = Axis.YP.rotationDegrees(-Direction.from2DDataValue(i).toYRot());
        }
    }

    private final ItemRenderer items;
    private final Quaternionf spin = new Quaternionf();
    private final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

    public ProjectBenchRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(ProjectBenchBlockEntity bench, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int overlay) {
        boolean drawItems = ClientConfig.showcaseItems();
        boolean drawResult = ClientConfig.showcaseResult();
        List<ItemStack> shown = bench.display().items();
        Level level = bench.getLevel();
        if (shown == null || level == null || !drawItems && !drawResult) {
            return;
        }
        Direction facing = bench.getBlockState().getValue(BenchBlock.FACING);
        int light = LevelRenderer.getLightColor(level, above.setWithOffset(bench.getBlockPos(), Direction.UP));
        int seed = (int) bench.getBlockPos().asLong();
        pose.pushPose();
        pose.translate(0.5, 1.0, 0.5);
        pose.mulPose(FACING[facing.get2DDataValue()]);
        for (int i = 0; drawItems && i < shown.size(); ++i) {
            if (shown.get(i).isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate((i % 3 - 1) * STEP, 0.02, (i / 3 - 1) * STEP);
            pose.mulPose(FLAT_TURN);
            pose.scale(FLAT, FLAT, FLAT);
            items.renderStatic(shown.get(i), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, seed + i);
            pose.popPose();
        }
        ItemStack made = drawResult ? bench.display().result() : ItemStack.EMPTY;
        if (!made.isEmpty()) {
            float time = level.getGameTime() + partialTick;
            pose.pushPose();
            pose.translate(0, HOVER + Mth.sin(time * 0.1F) * 0.04, 0);
            pose.mulPose(spin.rotationY(time * 2F % 360F * Mth.DEG_TO_RAD));
            pose.scale(RESULT_SIZE, RESULT_SIZE, RESULT_SIZE);
            items.renderStatic(made, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, seed);
            pose.popPose();
        }
        pose.popPose();
    }
}
