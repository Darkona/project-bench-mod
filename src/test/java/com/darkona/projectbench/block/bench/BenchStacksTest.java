package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.blueprint.Blueprint;
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
import java.util.function.Predicate;

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

    private static int[] counts(SimpleContainer c) {
        int[] out = new int[c.getContainerSize()];
        for (int i = 0; i < out.length; ++i) {
            out[i] = c.getItem(i).getCount();
        }
        return out;
    }

    @Test
    void balanceSpreadsEachKindOverItsSlots() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 7), new ItemStack(Items.OAK_PLANKS, 4),
                new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.OAK_PLANKS, 1), new ItemStack(Items.IRON_INGOT, 1));
        assertTrue(BenchStacks.balance(m));
        assertEquals(List.of(3, 3, 3, 2, 3, 0, 0, 0, 0), toList(counts(m)), "9 iron 3-3-3, 5 planks 3-2, the remainder from the first");
        assertFalse(BenchStacks.balance(m), "balanced already: nothing changes");
    }

    @Test
    void balanceKeepsComponentsApart() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 10), named(new ItemStack(Items.IRON_INGOT, 1), "odd"),
                new ItemStack(Items.IRON_INGOT, 0), new ItemStack(Items.IRON_INGOT, 2));
        BenchStacks.balance(m);
        assertEquals(6, m.getItem(0).getCount());
        assertEquals(1, m.getItem(1).getCount(), "a named ingot is another kind");
        assertTrue(m.getItem(2).isEmpty(), "an empty slot stays empty");
        assertEquals(6, m.getItem(3).getCount());
    }

    @Test
    void consumeRefillsFromTheFirstEqualStack() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 1));
        SimpleContainer s = new SimpleContainer(18);
        s.setItem(3, named(new ItemStack(Items.IRON_INGOT, 5), "odd"));
        s.setItem(4, new ItemStack(Items.IRON_INGOT, 2));
        s.setItem(7, new ItemStack(Items.IRON_INGOT, 2));
        BenchStacks.consume(m, Stock.of(s), 0, null);
        assertEquals(1, m.getItem(0).getCount(), "the matrix slot keeps its last item");
        assertEquals(1, s.getItem(4).getCount(), "the first equal stack pays for it");
        assertEquals(2, s.getItem(7).getCount());
        assertEquals(5, s.getItem(3).getCount(), "a stack with other components is not equal");
        BenchStacks.consume(m, Stock.of(s), 0, null);
        BenchStacks.consume(m, Stock.of(s), 0, null);
        assertTrue(s.getItem(4).isEmpty());
        assertEquals(1, s.getItem(7).getCount(), "then the next one");
        BenchStacks.consume(m, Stock.of(s), 0, null);
        assertEquals(1, m.getItem(0).getCount());
        BenchStacks.consume(m, Stock.of(s), 0, null);
        assertTrue(m.getItem(0).isEmpty(), "with nothing equal left the matrix slot empties: the recipe breaks");
    }

    @Test
    void withoutKeepLayoutTheMatrixPays() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 1));
        BenchStacks.consume(m, null, 0, null);
        assertTrue(m.getItem(0).isEmpty(), "no internal inventory to pay from: the matrix slot empties");
    }

    @Test
    void consumeTakesFromTheMatrixWhileItHasMore() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 3));
        SimpleContainer s = container(18, new ItemStack(Items.IRON_INGOT, 3));
        BenchStacks.consume(m, Stock.of(s), 0, null);
        assertEquals(2, m.getItem(0).getCount());
        assertEquals(3, s.getItem(0).getCount());
    }

    @Test
    void insertMergesBeforeTakingEmptySlots() {
        SimpleContainer s = new SimpleContainer(18);
        s.setItem(5, new ItemStack(Items.BUCKET, 15));
        ItemStack rest = BenchStacks.insert(s, new ItemStack(Items.BUCKET, 3));
        assertTrue(rest.isEmpty());
        assertEquals(16, s.getItem(5).getCount(), "onto the stack there, up to its size");
        assertEquals(2, s.getItem(0).getCount(), "the rest in the first empty slot");
    }

    @Test
    void shiftClickPutsItemsOnTheGhostsThatTakeThem() {
        ItemStack worn = new ItemStack(Items.SHEARS);
        worn.setDamageValue(30);
        List<ItemStack> pattern = new ArrayList<>(List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.SHEARS),
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        Blueprint bp = new Blueprint(pattern, ResourceLocation.withDefaultNamespace("test"), new ItemStack(Items.STICK));
        SimpleContainer m = container(9, new ItemStack(Items.OAK_PLANKS, 4), ItemStack.EMPTY, ItemStack.EMPTY, named(new ItemStack(Items.OAK_PLANKS), "odd"));
        assertTrue(BenchStacks.anyGhostAccepts(bp, new ItemStack(Items.OAK_PLANKS)));
        assertFalse(BenchStacks.anyGhostAccepts(bp, new ItemStack(Items.DIRT)), "no ghost takes dirt");
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 10);
        assertEquals(10, BenchStacks.intoGhosts(m, bp, planks));
        assertTrue(planks.isEmpty());
        assertEquals(7, m.getItem(0).getCount(), "onto the emptiest first: both end at seven");
        assertEquals(7, m.getItem(1).getCount());
        assertEquals("odd", m.getItem(3).getHoverName().getString(), "a slot holding planks of other components is left alone");
        assertEquals(1, m.getItem(3).getCount());
        assertEquals(0, BenchStacks.intoGhosts(m, bp, new ItemStack(Items.DIRT, 5)), "dirt goes nowhere");
        assertEquals(1, BenchStacks.intoGhosts(m, bp, worn), "a worn tool on the new tool's ghost");
        assertTrue(m.getItem(2).is(Items.SHEARS) && m.getItem(2).getDamageValue() == 30);
        ItemStack lots = new ItemStack(Items.OAK_PLANKS, 64);
        assertEquals(64, BenchStacks.intoGhosts(m, bp, lots));
        assertEquals(39, m.getItem(0).getCount(), "split evenly");
        assertEquals(39, m.getItem(1).getCount());
        lots = new ItemStack(Items.OAK_PLANKS, 64);
        assertEquals(50, BenchStacks.intoGhosts(m, bp, lots), "up to full stacks");
        assertEquals(14, lots.getCount(), "the rest stays where it was");
        assertEquals(64, m.getItem(0).getCount());
        assertFalse(BenchStacks.missing(m, bp, 1), "no ghost left to draw there");
    }

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>(a.length);
        for (int v : a) {
            out.add(v);
        }
        return out;
    }

    @Test
    void clearPutsTheMatrixInTheStockAndLeavesWhatDoesNotFit() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.OAK_PLANKS, 3));
        SimpleContainer full = container(1, new ItemStack(Items.DIRT, 64));
        assertFalse(BenchStacks.clear(m, Stock.of(full)), "no room: nothing moves");
        assertEquals(List.of(5, 3, 0, 0, 0, 0, 0, 0, 0), toList(counts(m)));
        SimpleContainer room = container(2, new ItemStack(Items.DIRT, 64));
        assertTrue(BenchStacks.clear(m, Stock.of(room)));
        assertTrue(m.getItem(0).isEmpty() && room.getItem(1).is(Items.IRON_INGOT) && room.getItem(1).getCount() == 5, "the iron took the free slot");
        assertEquals(3, m.getItem(1).getCount(), "the planks had none left and stay");
    }

    @Test
    void clearFallsThroughTheChain() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 5));
        SimpleContainer own = container(1, new ItemStack(Items.DIRT, 64));
        SimpleContainer beside = container(3);
        assertTrue(BenchStacks.clear(m, Stock.chain(Stock.of(own), Stock.of(beside))));
        assertTrue(m.isEmpty() && beside.getItem(0).getCount() == 5, "the full inventory sends it on to the one beside");
    }

    @Test
    void clearEndsInThePocketsWhenTheStockAndTheChestAreFull() {
        SimpleContainer own = container(1, new ItemStack(Items.DIRT, 64));
        SimpleContainer beside = container(1, new ItemStack(Items.DIRT, 64));
        SimpleContainer pockets = container(3, new ItemStack(Items.IRON_INGOT, 60));
        Stock pocketStock = new Stock() {
            public int count(ItemStack like) { return 0; }
            public int take(ItemStack like, int amount) { return 0; }
            public ItemStack find(Predicate<ItemStack> test) { return ItemStack.EMPTY; }
            public ItemStack insert(ItemStack stack) { return BenchStacks.insert(pockets, pockets.getContainerSize(), stack); }
        };
        Stock all = Stock.chain(Stock.chain(Stock.of(own), Stock.of(beside)), pocketStock);
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 10));
        assertTrue(BenchStacks.clear(m, all));
        assertTrue(m.isEmpty(), "the pockets took it");
        assertEquals(64, pockets.getItem(0).getCount(), "merged into the stack first");
        assertEquals(6, pockets.getItem(1).getCount(), "then an empty slot");
        assertEquals(0, pockets.getItem(2).getCount());
    }

    @Test
    void clearWithEverythingFullMovesNothing() {
        SimpleContainer m = container(9, new ItemStack(Items.IRON_INGOT, 5));
        SimpleContainer pockets = container(1, new ItemStack(Items.DIRT, 64));
        Stock pocketStock = new Stock() {
            public int count(ItemStack like) { return 0; }
            public int take(ItemStack like, int amount) { return 0; }
            public ItemStack find(Predicate<ItemStack> test) { return ItemStack.EMPTY; }
            public ItemStack insert(ItemStack stack) { return BenchStacks.insert(pockets, pockets.getContainerSize(), stack); }
        };
        Stock all = Stock.chain(Stock.of(container(1, new ItemStack(Items.DIRT, 64))), pocketStock);
        assertFalse(BenchStacks.clear(m, all));
        assertEquals(5, m.getItem(0).getCount());
    }

    private static Blueprint ironAndPlanks() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            grid.add(i < 3 ? new ItemStack(Items.IRON_INGOT) : i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        return new Blueprint(grid, ResourceLocation.withDefaultNamespace("test"), new ItemStack(Items.STICK));
    }

    @Test
    void fillSpreadsEachKindOverItsPositionsWithWhatTheStockHasOfIt() {
        SimpleContainer m = new SimpleContainer(9);
        SimpleContainer stock = container(4, new ItemStack(Items.IRON_INGOT, 10), new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.OAK_PLANKS, 36));
        assertTrue(BenchStacks.fill(m, ironAndPlanks(), Stock.of(stock)));
        assertEquals(List.of(4, 3, 3, 0, 64, 0, 0, 0, 0), toList(counts(m)), "10 iron over three slots is 4-3-3; the planks take a full stack, whatever the iron is");
        assertEquals(0, Stock.of(stock).count(new ItemStack(Items.IRON_INGOT)));
        assertEquals(36, Stock.of(stock).count(new ItemStack(Items.OAK_PLANKS)), "what is left stays in the stock");
    }

    @Test
    void fillSendsWhatTheGhostsDoNotTakeBackAndKeepsWhatTheyDo() {
        SimpleContainer m = container(9, new ItemStack(Items.DIRT, 4), new ItemStack(Items.IRON_INGOT, 2));
        SimpleContainer stock = container(4, new ItemStack(Items.IRON_INGOT, 4));
        BenchStacks.fill(m, ironAndPlanks(), Stock.of(stock));
        assertFalse(m.getItem(0).is(Items.DIRT), "the dirt left the first position");
        assertTrue(stock.getItem(1).is(Items.DIRT) || stock.getItem(2).is(Items.DIRT) || stock.getItem(3).is(Items.DIRT), "and went to the stock");
        assertTrue(m.getItem(0).is(Items.IRON_INGOT) && m.getItem(1).is(Items.IRON_INGOT) && m.getItem(2).is(Items.IRON_INGOT), "iron on its three positions");
    }

    @Test
    void fillWithoutOneOfTheKindsStillPlacesWhatThereIs() {
        SimpleContainer m = new SimpleContainer(9);
        SimpleContainer stock = container(2, new ItemStack(Items.IRON_INGOT, 2));
        BenchStacks.fill(m, ironAndPlanks(), Stock.of(stock));
        assertEquals(List.of(1, 1, 0, 0, 0, 0, 0, 0, 0), toList(counts(m)), "two iron for three positions: one each on the first two; no planks to place");
    }

    @Test
    void consumeTakesTheSameItemBeforeAnySubstitute() {
        SimpleContainer m = container(9, new ItemStack(Items.OAK_PLANKS, 1));
        SimpleContainer s = container(3, new ItemStack(Items.BIRCH_PLANKS, 4), new ItemStack(Items.OAK_PLANKS, 2));
        BenchStacks.consume(m, Stock.of(s), 0, planks -> planks.is(net.minecraft.tags.ItemTags.PLANKS));
        assertTrue(m.getItem(0).is(Items.OAK_PLANKS) && s.getItem(1).getCount() == 1 && s.getItem(0).getCount() == 4, "the same wood came first");
    }

    @Test
    void consumeFallsBackToAnItemTheRecipeAccepts() {
        SimpleContainer m = container(9, new ItemStack(Items.OAK_PLANKS, 1));
        SimpleContainer s = container(3, new ItemStack(Items.COBBLESTONE, 4), new ItemStack(Items.BIRCH_PLANKS, 2));
        BenchStacks.consume(m, Stock.of(s), 0, planks -> planks.is(Items.BIRCH_PLANKS));
        assertTrue(m.getItem(0).is(Items.BIRCH_PLANKS) && m.getItem(0).getCount() == 1 && s.getItem(1).getCount() == 1 && s.getItem(0).getCount() == 4,
                "birch planks in the slot, one fewer in the stock, the cobblestone untouched");
        SimpleContainer none = container(1, new ItemStack(Items.COBBLESTONE, 4));
        SimpleContainer m2 = container(9, new ItemStack(Items.OAK_PLANKS, 1));
        BenchStacks.consume(m2, Stock.of(none), 0, planks -> planks.is(Items.BIRCH_PLANKS));
        assertTrue(m2.getItem(0).isEmpty(), "nothing the recipe accepts: the matrix pays");
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
