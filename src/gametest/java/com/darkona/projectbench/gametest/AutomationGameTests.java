package com.darkona.projectbench.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import dev.darkona.jdlib.api.world.RelativeSide;
import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.foundation.BenchBlock;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutomationGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);

    private AutomationGameTests() {
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper, BlockPos at, Direction facing) {
        helper.setBlock(at, Content.Blocks.PROJECT_BENCH.get().defaultBlockState().setValue(BenchBlock.FACING, facing));
        return helper.getBlockEntity(at);
    }

    @Nullable
    private static IItemHandler maybe(GameTestHelper helper, BlockPos at, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), side);
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos at, Direction side) {
        IItemHandler h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), side);
        helper.assertTrue(h != null, "an item handler at " + at + " on " + side);
        return h;
    }

    private static ItemStack push(IItemHandler handler, ItemStack stack) {
        ItemStack rest = stack;
        int before;
        do {
            before = rest.getCount();
            rest = ItemHandlerHelper.insertItem(handler, rest, false);
        } while (!rest.isEmpty() && rest.getCount() < before);
        return rest;
    }

    private static ItemStack pullOne(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack out = handler.extractItem(i, 1, false);
            if (!out.isEmpty()) {
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    private static BlockState hopperFacing(Direction facing) {
        return Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing);
    }

    private static void recordSticks(GameTestHelper helper, ProjectBenchBlockEntity bench) {
        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        helper.assertTrue(bench.record(), "the sticks are recorded");
        bench.matrix().clearContent();
    }

    private static int matrixCount(ProjectBenchBlockEntity bench, Item item) {
        return count(bench.matrix(), item);
    }

    @GameTest(template = GameTests.EMPTY, timeoutTicks = 300)
    public static void aHopperBehindFillsTheMatrixAndOneBelowEmptiesIt(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        helper.setBlock(BENCH.south(), hopperFacing(Direction.NORTH));
        helper.setBlock(BENCH.below(), Blocks.HOPPER);
        HopperBlockEntity behind = helper.getBlockEntity(BENCH.south());
        HopperBlockEntity below = helper.getBlockEntity(BENCH.below());
        behind.setItem(0, new ItemStack(Items.STONE, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(behind.isEmpty(), "the hopper behind emptied into the bench");
            helper.assertTrue(count(below, Items.STONE) == 3, "and the hopper below, with the inventory empty, pulled it all out of the matrix");
            helper.assertTrue(matrixCount(bench, Items.STONE) == 0 && count(bench.storage(), Items.STONE) == 0, "nothing left in the bench");
        });
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theBackFillsTheMatrixInOrder(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        IItemHandler back = handler(helper, BENCH, Direction.SOUTH);
        helper.assertTrue(back.getSlots() == ProjectBenchBlockEntity.MATRIX + ProjectBenchBlockEntity.STORAGE, "the matrix, then the inventory: " + back.getSlots());

        helper.assertTrue(push(back, new ItemStack(Items.STONE, 70)).isEmpty(), "no blueprint: everything goes in");
        helper.assertTrue(bench.matrix().getItem(0).is(Items.STONE) && bench.matrix().getItem(0).getCount() == 64
                && bench.matrix().getItem(1).getCount() == 6, "the first empty slot to a full stack, then the next: " + bench.matrix().getItem(1));
        helper.assertTrue(push(back, new ItemStack(Items.DIRT, 2)).isEmpty() && bench.matrix().getItem(2).is(Items.DIRT), "another item, the next empty slot");
        helper.assertTrue(push(back, new ItemStack(Items.STONE, 3)).isEmpty() && bench.matrix().getItem(1).getCount() == 9
                && bench.matrix().getItem(3).isEmpty(), "stone tops up the stone slot before any empty one");
        helper.assertTrue(count(bench.storage(), Items.STONE) == 0 && count(bench.storage(), Items.DIRT) == 0, "the back never fills the inventory");

        bench.matrix().clearContent();
        recordSticks(helper, bench);
        ItemStack stone = push(back, new ItemStack(Items.STONE, 5));
        helper.assertTrue(stone.getCount() == 5 && bench.matrix().isEmpty(), "a recorded blueprint: what no ghost wants stays out");
        helper.assertTrue(push(back, new ItemStack(Items.OAK_PLANKS, 7)).isEmpty(), "the planks go in");
        int one = bench.matrix().getItem(1).getCount();
        int four = bench.matrix().getItem(4).getCount();
        helper.assertTrue(one + four == 7 && Math.abs(one - four) <= 1 && matrixCount(bench, Items.OAK_PLANKS) == 7,
                "onto the two ghosts, spread: " + one + " and " + four);
        helper.assertTrue(bench.result().getItem(0).is(Items.STICK), "and the bench works the recipe out: " + bench.result().getItem(0));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theBackTakesFromTheMatrixFirstAndTheBottomFromTheInventory(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.storage().setItem(3, new ItemStack(Items.DIRT, 2));
        IItemHandler back = handler(helper, BENCH, Direction.SOUTH);
        IItemHandler down = handler(helper, BENCH, Direction.DOWN);
        helper.assertTrue(pullOne(back).is(Items.OAK_PLANKS), "the back takes from the matrix first");
        helper.assertFalse(bench.result().getItem(0).is(Items.STICK), "and the recipe follows: no sticks");
        helper.assertTrue(pullOne(down).is(Items.DIRT) && pullOne(down).is(Items.DIRT), "the bottom takes from the inventory first");
        helper.assertTrue(pullOne(down).is(Items.OAK_PLANKS), "and then from the matrix");
        helper.assertTrue(pullOne(down).isEmpty() && pullOne(back).isEmpty(), "until both are empty");

        bench.storage().setItem(0, new ItemStack(Items.DIRT));
        helper.assertTrue(pullOne(back).is(Items.DIRT), "the back takes from the inventory once the matrix is empty");
        IItemHandler left = handler(helper, BENCH, Direction.EAST);
        helper.assertTrue(left.getSlots() == ProjectBenchBlockEntity.STORAGE + ProjectBenchBlockEntity.MATRIX
                && push(left, new ItemStack(Items.COBBLESTONE, 5)).isEmpty() && count(bench.storage(), Items.COBBLESTONE) == 5
                && matrixCount(bench, Items.COBBLESTONE) == 0, "the sides fill the inventory, never the matrix");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void nothingTakesTheResult(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(bench.result().getItem(0).is(Items.STICK), "sticks on offer");
        for (Direction side : Direction.values()) {
            IItemHandler h = handler(helper, BENCH, side);
            helper.assertTrue(h.getSlots() == 2 || h.getSlots() == ProjectBenchBlockEntity.STORAGE + ProjectBenchBlockEntity.MATRIX,
                    side + " has no slot for the result: " + h.getSlots());
            for (int i = 0; i < h.getSlots(); ++i) {
                helper.assertFalse(h.getStackInSlot(i).is(Items.STICK), side + " shows no sticks");
            }
        }
        for (Direction side : Direction.values()) {
            IItemHandler h = handler(helper, BENCH, side);
            for (ItemStack out = pullOne(h); !out.isEmpty(); out = pullOne(h)) {
                helper.assertFalse(out.is(Items.STICK), side + " took a stick");
            }
        }
        helper.assertTrue(bench.matrix().isEmpty(), "the planks came out instead");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theFrontTakesAndGivesBlueprintsAndTheCost(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        IItemHandler front = handler(helper, BENCH, Direction.NORTH);
        helper.assertTrue(front.getSlots() == 2, "the blueprint slot and the cost slot: " + front.getSlots());
        helper.assertTrue(push(front, new ItemStack(Items.DIRT)).getCount() == 1, "dirt does not go in");
        helper.assertTrue(push(front, new ItemStack(Content.Items.BLUEPRINT.get(), 3)).getCount() == 2
                && bench.blueprintSlot().getItem(0).getCount() == 1, "one blueprint goes in");
        helper.assertTrue(push(front, new ItemStack(Items.WHITE_DYE, 5)).isEmpty() && bench.costSlot().getItem(0).getCount() == 5,
                "the dye goes into the cost slot");
        helper.assertTrue(bench.storage().isEmpty() && bench.matrix().isEmpty(), "and nowhere else");
        ItemStack out = front.extractItem(0, 64, false);
        helper.assertTrue(out.is(Content.Items.BLUEPRINT.get()) && bench.blueprintSlot().isEmpty(), "the blueprint comes out the front");
        out = front.extractItem(1, 64, false);
        helper.assertTrue(out.is(Items.WHITE_DYE) && out.getCount() == 5 && bench.costSlot().isEmpty(), "and so does the dye");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void blueprintsOffTheFrontIsAnInventorySide(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.NORTH);
        IItemHandler blueprintSide = handler(helper, BENCH, Direction.NORTH);
        try {
            Config.CONFIG.override(Config.Settings.builder().blueprint(false).build());
            helper.assertTrue(blueprintSide.insertItem(0, new ItemStack(Content.Items.BLUEPRINT.get()), false).getCount() == 1,
                    "the blueprint view a pipe kept goes quiet");
            IItemHandler front = handler(helper, BENCH, Direction.NORTH);
            helper.assertTrue(front.getSlots() == ProjectBenchBlockEntity.STORAGE + ProjectBenchBlockEntity.MATRIX, "the front is an inventory side now");
            helper.assertTrue(push(front, new ItemStack(Items.DIRT, 4)).isEmpty() && count(bench.storage(), Items.DIRT) == 4, "and fills the inventory");
            helper.assertTrue(pullOne(front).is(Items.DIRT), "and empties it");
        } finally {
            Config.CONFIG.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void eachHalfOfTheStationHasItsOwnSides(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        ProjectBenchBlock.Pair pair = DoubleBenchGameTests.joinedAt(helper, at);
        ProjectBenchBlockEntity second = pair.second();
        ProjectBenchBlockEntity first = pair.first();
        helper.assertTrue(maybe(helper, at, Direction.EAST) == null && maybe(helper, at.east(), Direction.WEST) == null,
                "the side where the halves touch does not exist");
        helper.assertTrue(push(handler(helper, at, Direction.SOUTH), new ItemStack(Items.STONE, 2)).isEmpty()
                && matrixCount(second, Items.STONE) == 2 && matrixCount(first, Items.STONE) == 0, "the back of a half fills its own matrix");
        helper.assertTrue(push(handler(helper, at.east(), Direction.SOUTH), new ItemStack(Items.DIRT, 3)).isEmpty()
                && matrixCount(first, Items.DIRT) == 3 && matrixCount(second, Items.DIRT) == 0, "and the other's back its own");
        helper.assertTrue(push(handler(helper, at, Direction.WEST), new ItemStack(Items.COBBLESTONE, 4)).isEmpty()
                && count(second.storage(), Items.COBBLESTONE) == 4 && count(first.storage(), Items.COBBLESTONE) == 0, "an outer side fills its half's inventory");
        helper.assertTrue(push(handler(helper, at.east(), Direction.EAST), new ItemStack(Items.GRAVEL, 5)).isEmpty()
                && count(first.storage(), Items.GRAVEL) == 5 && count(second.storage(), Items.GRAVEL) == 0, "and the other outer side its own");
        helper.assertTrue(push(handler(helper, at, Direction.NORTH), new ItemStack(Items.WHITE_DYE, 2)).isEmpty()
                && second.costSlot().getItem(0).getCount() == 2 && first.costSlot().isEmpty(), "each half has its blueprint front, its cost slot included");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void turningTheBenchTurnsItsSides(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH, Direction.EAST);
        helper.assertTrue(handler(helper, BENCH, Direction.EAST).getSlots() == 2, "facing east, the east is the blueprint front");
        helper.assertTrue(push(handler(helper, BENCH, Direction.WEST), new ItemStack(Items.STONE)).isEmpty()
                && matrixCount(bench, Items.STONE) == 1, "the west is the back: the matrix");
        helper.assertTrue(push(handler(helper, BENCH, Direction.NORTH), new ItemStack(Items.DIRT)).isEmpty()
                && count(bench.storage(), Items.DIRT) == 1, "the north is a side: the inventory");
        helper.assertTrue(RelativeSide.of(Direction.EAST, Direction.SOUTH) == RelativeSide.LEFT, "the south is its left");

        BlockCapabilityCache<IItemHandler, Direction> cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, helper.getLevel(),
                helper.absolutePos(BENCH), Direction.EAST);
        IItemHandler before = cache.getCapability();
        if (before == null || before.getSlots() != 2) {
            helper.fail("a pipe on the east holds the blueprint view");
            return;
        }
        helper.setBlock(BENCH, helper.getBlockState(BENCH).setValue(BenchBlock.FACING, Direction.SOUTH));
        helper.assertTrue(helper.getBlockEntity(BENCH) == bench, "turned in place, the same bench");
        IItemHandler after = cache.getCapability();
        helper.assertTrue(after != null && after.getSlots() == ProjectBenchBlockEntity.STORAGE + ProjectBenchBlockEntity.MATRIX,
                "turned to face south, the pipe on the east asks again and gets a side: " + (after == null ? 0 : after.getSlots()));
        helper.assertTrue(before.insertItem(0, new ItemStack(Content.Items.BLUEPRINT.get()), false).getCount() == 1, "the old view went quiet");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theArchiveTakesBlueprintsAndGivesThemBelow(GameTestHelper helper) {
        BlockPos at = BENCH;
        helper.setBlock(at, Content.Blocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(at);
        IItemHandler side = handler(helper, at, Direction.WEST);
        helper.assertTrue(side.getSlots() == BlueprintArchiveBlockEntity.SLOTS, "the archive's 36 slots");
        helper.assertTrue(side.insertItem(0, new ItemStack(Items.DIRT, 3), false).getCount() == 3 && archive.blueprints().isEmpty(), "dirt does not go in");
        helper.assertTrue(side.insertItem(0, new ItemStack(Content.Items.BLUEPRINT.get(), 5), false).isEmpty(), "blueprints do");
        helper.assertTrue(side.extractItem(0, 64, false).isEmpty(), "a side takes nothing out");
        ItemStack back = handler(helper, at, Direction.DOWN).extractItem(0, 64, false);
        helper.assertTrue(back.is(Content.Items.BLUEPRINT.get()) && back.getCount() == 5 && archive.blueprints().isEmpty(), "the bottom does");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void theArchivesSidesTurnWithIt(GameTestHelper helper) {
        BlockPos at = BENCH;
        helper.setBlock(at, Content.Blocks.BLUEPRINT_ARCHIVE.get().defaultBlockState().setValue(BenchBlock.FACING, Direction.EAST));
        try {
            Config.CONFIG.override(Config.Settings.builder().archiveInsert(1 << RelativeSide.BACK.ordinal()).archiveExtract(1 << RelativeSide.FRONT.ordinal()).build());
            ItemStack blueprint = new ItemStack(Content.Items.BLUEPRINT.get(), 2);
            helper.assertTrue(handler(helper, at, Direction.EAST).insertItem(0, blueprint, false).getCount() == 2, "facing east, the east is the front: no way in");
            helper.assertTrue(handler(helper, at, Direction.WEST).insertItem(0, blueprint, false).isEmpty(), "the west is the back: in");
            helper.assertTrue(handler(helper, at, Direction.WEST).extractItem(0, 64, false).isEmpty(), "but not out");
            helper.assertTrue(handler(helper, at, Direction.DOWN).extractItem(0, 64, false).isEmpty(), "nor out of the bottom");
            helper.assertTrue(handler(helper, at, Direction.EAST).extractItem(0, 64, false).getCount() == 2, "out of the front");
        } finally {
            Config.CONFIG.clearOverride();
        }
        helper.succeed();
    }
}
