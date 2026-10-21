package com.darkona.projectbench.block.bench;

import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchStacksTest {
    private static SimpleContainer container(int size, ItemStack... stacks) {
        SimpleContainer c = new SimpleContainer(size);
        for (int i = 0; i < stacks.length; ++i) {
            c.setItem(i, stacks[i]);
        }
        return c;
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static Blueprint ironAndPlanks() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            grid.add(i < 3 ? new ItemStack(Items.IRON_INGOT) : i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        return new Blueprint(grid, ResourceLocation.withDefaultNamespace("test"), new ItemStack(Items.STICK));
    }

    private static int pick(SimpleContainer m, @Nullable Blueprint bp, ItemStack stack) {
        int picked = -1;
        for (int i = 0; i < 9; ++i) {
            if (BenchStacks.matrixRoom(m, bp, i, stack) > 0) {
                assertEquals(-1, picked, "only one slot is picked");
                picked = i;
            }
        }
        return picked;
    }

    @Test
    void matrixRoomFollowsTheOrder() {
        SimpleContainer m = new SimpleContainer(9);
        ItemStack stone = new ItemStack(Items.STONE);
        assertEquals(0, pick(m, null, stone), "no blueprint: the first empty slot");
        assertEquals(64, BenchStacks.matrixRoom(m, null, 0, stone), "a full stack of room");
        m.setItem(3, new ItemStack(Items.STONE, 10));
        assertEquals(3, pick(m, null, stone), "a slot with stone comes before an empty one");
        assertEquals(54, BenchStacks.matrixRoom(m, null, 3, stone));
        m.setItem(3, new ItemStack(Items.STONE, 64));
        assertEquals(0, pick(m, null, stone), "full: the first empty slot again");
        assertEquals(-1, pick(m, null, ItemStack.EMPTY), "nothing for nothing");

        Blueprint bp = ironAndPlanks();
        SimpleContainer g = new SimpleContainer(9);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        assertEquals(0, pick(g, bp, iron), "a blueprint: the first ghost that wants iron");
        assertEquals(1, BenchStacks.matrixRoom(g, bp, 0, iron), "one at a time while the others are as empty");
        g.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        assertEquals(1, pick(g, bp, iron), "then the emptiest ghost");
        g.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        g.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        assertEquals(0, pick(g, bp, iron));
        assertEquals(1, BenchStacks.matrixRoom(g, bp, 0, iron), "up to one more than the next emptiest");
        g.setItem(1, new ItemStack(Items.IRON_INGOT, 3));
        assertEquals(3, BenchStacks.matrixRoom(g, bp, 0, iron), "the emptiest catches up with the next one, plus one");
        assertEquals(-1, pick(g, bp, stone), "what no ghost wants stays out, even with empty slots");
        g.setItem(2, new ItemStack(Items.GOLD_INGOT));
        assertEquals(0, pick(g, bp, iron), "a ghost slot holding something else is not a target");
        assertEquals(4, pick(g, bp, new ItemStack(Items.OAK_PLANKS)), "the planks' own ghost");
    }

    @Test
    void topUpFillsOnlyTheSlotsThatHoldTheItem() {
        SimpleContainer m = new SimpleContainer(9);
        assertEquals(0, BenchStacks.matrixRoom(m, null, 0, new ItemStack(Items.STONE), false), "no empty slot without step 3");
        ItemStack stone = new ItemStack(Items.STONE, 20);
        assertEquals(0, BenchStacks.topUp(m, stone), "an empty matrix takes nothing");
        assertEquals(20, stone.getCount());
        m.setItem(2, new ItemStack(Items.STONE, 60));
        m.setItem(5, new ItemStack(Items.STONE, 50));
        m.setItem(7, new ItemStack(Items.DIRT, 1));
        assertEquals(18, BenchStacks.topUp(m, stone), "4 into slot 2, 14 into slot 5");
        assertEquals(64, m.getItem(2).getCount());
        assertEquals(64, m.getItem(5).getCount());
        assertEquals(2, stone.getCount(), "the rest stays with the caller");
        assertEquals(1, m.getItem(7).getCount());
        assertTrue(m.getItem(0).isEmpty() && m.getItem(8).isEmpty(), "no empty slot took any");
    }
}
