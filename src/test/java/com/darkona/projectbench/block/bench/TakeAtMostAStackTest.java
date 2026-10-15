package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.gametest.FakeInventories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakeAtMostAStackTest {
    @Test
    void aCreativeDrawerGivesAStackAtMost() {
        FakeInventories.Vending vending = new FakeInventories.Vending(Items.IRON_INGOT);
        assertEquals(64, BenchStacks.takeAtMostAStack(vending, 0, Integer.MAX_VALUE).getCount());
        assertEquals(10, BenchStacks.takeAtMostAStack(vending, 0, 10).getCount());
        assertEquals(16, BenchStacks.takeAtMostAStack(new FakeInventories.Vending(Items.ENDER_PEARL), 0, 100).getCount(), "the item's own stack size");
    }

    @Test
    void aDrawerLosesExactlyWhatIsTaken() {
        FakeInventories.Drawer drawer = new FakeInventories.Drawer(Items.IRON_INGOT, 1000);
        assertEquals(64, BenchStacks.takeAtMostAStack(drawer, 0, 500).getCount());
        assertEquals(936, drawer.count());
    }

    @Test
    void anInventoryThatSaysItWouldGiveMoreIsRefused() {
        assertTrue(BenchStacks.takeAtMostAStack(new FakeInventories.Greedy(Items.IRON_INGOT, false), 0, 64).isEmpty());
    }

    @Test
    void anInventoryThatGivesMoreAnywayGetsTheExcessBack() {
        FakeInventories.Greedy liar = new FakeInventories.Greedy(Items.IRON_INGOT, true);
        ItemStack got = BenchStacks.takeAtMostAStack(liar, 0, 64);
        assertEquals(64, got.getCount());
        assertEquals(Integer.MAX_VALUE - 64, liar.received, "the excess goes back in");
    }

    @Test
    void aPlainInventoryIsUnchanged() {
        ItemStackHandler chest = new ItemStackHandler(1);
        chest.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 20));
        assertEquals(20, BenchStacks.takeAtMostAStack(chest, 0, 64).getCount());
        assertTrue(chest.getStackInSlot(0).isEmpty());
        assertTrue(BenchStacks.takeAtMostAStack(chest, 0, 64).isEmpty());
    }

    @Test
    void countsSaturate() {
        assertEquals(Integer.MAX_VALUE, BenchStacks.sum(Integer.MAX_VALUE, 5));
        assertEquals(Integer.MAX_VALUE, Stock.of(new FakeInventories.Vending(Items.IRON_INGOT)).count(new ItemStack(Items.IRON_INGOT)));
        assertEquals(200, Stock.of(new FakeInventories.Vending(Items.IRON_INGOT)).take(new ItemStack(Items.IRON_INGOT), 200));
    }
}
