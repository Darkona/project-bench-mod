package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import java.util.List;

final class BenchCost {
    private static final Component RECORD = Component.translatable("gui.projectbench.project_bench.record");

    private final ProjectBenchMenu menu;
    @Nullable
    private Item shownCost;
    private int shownCount = -1;
    private int shownMissing = -1;
    private ItemStack ghost = ItemStack.EMPTY;
    private int count;
    private List<Component> recordTip = List.of(RECORD, Component.translatable("gui.projectbench.project_bench.record_help").withStyle(ChatFormatting.GRAY));

    BenchCost(ProjectBenchMenu menu) {
        this.menu = menu;
    }

    ItemStack ghost() {
        return ghost;
    }

    List<Component> recordTip() {
        return recordTip;
    }

    List<Component> slotTip() {
        return ghost.isEmpty() ? List.of() : List.of(Component.translatable("gui.projectbench.project_bench.cost"),
                Component.translatable("gui.projectbench.project_bench.cost_help", count, ghost.getHoverName()).withStyle(ChatFormatting.GRAY));
    }

    void tick() {
        Config.Settings s = Config.get();
        Item cost = Config.costItem(s);
        int missing = menu.missingCost();
        if (cost == shownCost && s.recordCostCount() == shownCount && missing == shownMissing) {
            return;
        }
        shownCost = cost;
        shownCount = s.recordCostCount();
        shownMissing = missing;
        count = shownCount;
        if (cost == Items.AIR) {
            ghost = ItemStack.EMPTY;
            recordTip = List.of(RECORD, Component.translatable("gui.projectbench.project_bench.record_help").withStyle(ChatFormatting.GRAY));
            return;
        }
        ghost = new ItemStack(cost, count);
        Component help = Component.translatable("gui.projectbench.project_bench.record_help_cost", count, cost.getDescription()).withStyle(ChatFormatting.GRAY);
        recordTip = missing > 0
                ? List.of(RECORD, help, Component.translatable("gui.projectbench.project_bench.record_missing", missing, cost.getDescription()).withStyle(ChatFormatting.RED))
                : List.of(RECORD, help);
    }
}
