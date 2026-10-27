package com.darkona.projectbench.content.blueprint;

import com.darkona.projectbench.index.BenchDataComponents;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.Feature;
import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlueprintItem extends Item {
    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static Blueprint blueprint(ItemStack stack) {
        return stack.get(BenchDataComponents.BLUEPRINT.get());
    }

    public static boolean blank(ItemStack stack) {
        return Feature.BLUEPRINT.enabled() && stack.getItem() instanceof BlueprintItem && !recorded(stack);
    }

    public static boolean recorded(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem && blueprint(stack) != null;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return blueprint(stack) != null ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Blueprint blueprint = blueprint(stack);
        if (blueprint == null) {
            lines.add(Component.translatable("item.projectbench.blueprint.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("item.projectbench.blueprint.makes", blueprint.result().getCount(),
                blueprint.result().getHoverName()).withStyle(ChatFormatting.GOLD));
        if (!BenchConfig.get().tooltipIngredients()) {
            return;
        }

        int counted = 0;
        for (int i = 0; i < Blueprint.SIZE; ++i) {
            ItemStack ghost = blueprint.ghost(i);
            if (ghost.isEmpty() || (counted & 1 << i) != 0) {
                continue;
            }
            int count = 0;
            for (int j = i; j < Blueprint.SIZE; ++j) {
                if ((counted & 1 << j) == 0 && ItemStack.isSameItemSameComponents(ghost, blueprint.ghost(j))) {
                    counted |= 1 << j;
                    ++count;
                }
            }
            lines.add(Component.translatable("item.projectbench.blueprint.ingredient", count, ghost.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
