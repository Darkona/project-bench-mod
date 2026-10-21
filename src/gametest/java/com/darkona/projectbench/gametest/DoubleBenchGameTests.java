package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.block.BenchBlock;
import com.darkona.projectbench.block.bench.BenchSlabBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.network.RecordRecipe;
import com.darkona.projectbench.registry.ModItems;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.darkona.projectbench.gametest.ColumnAndBesideGameTests.recorded;
import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoubleBenchGameTests {
    private static final float SOUTH = 0;
    private static final float WEST = 90;

    private DoubleBenchGameTests() {
    }

    private static void place(GameTestHelper helper, Item item, BlockPos clicked, Direction face, float yaw, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(yaw);
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockPos at = helper.absolutePos(clicked);
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), face, at, false)));
    }

    private static void place(GameTestHelper helper, Item item, BlockPos at) {
        place(helper, item, at, Direction.UP, SOUTH, false);
    }

    private static ChestType type(GameTestHelper helper, BlockPos at) {
        BlockState state = helper.getBlockState(at);
        helper.assertTrue(state.getBlock() instanceof ProjectBenchBlock, "a bench at " + at);
        return state.getValue(ProjectBenchBlock.TYPE);
    }

    @Nullable
    private static ProjectBenchBlock.Pair pair(GameTestHelper helper, BlockPos at) {
        return ProjectBenchBlock.pair(helper.getBlockState(at), helper.getLevel(), helper.absolutePos(at));
    }

    private static void assertPair(GameTestHelper helper, BlockPos left, BlockPos right, String what) {
        helper.assertTrue(type(helper, left) == ChestType.RIGHT && type(helper, right) == ChestType.LEFT,
                what + ": the half on the player's left is RIGHT, the other LEFT, not " + type(helper, left) + "/" + type(helper, right));
        ProjectBenchBlockEntity first = helper.getBlockEntity(left);
        ProjectBenchBlockEntity second = helper.getBlockEntity(right);
        for (BlockPos at : new BlockPos[]{left, right}) {
            ProjectBenchBlock.Pair pair = pair(helper, at);
            helper.assertTrue(pair != null && pair.first() == first && pair.second() == second, what + ": both halves see the pair, left first");
        }
    }

    private static void assertAlone(GameTestHelper helper, BlockPos at, String what) {
        helper.assertTrue(type(helper, at) == ChestType.SINGLE && pair(helper, at) == null, what + ": " + at + " is on its own");
    }

    private static int dropped(GameTestHelper helper, Item item) {
        int n = 0;
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aSlabJoinsASlabOrABenchBesideIt(GameTestHelper helper) {
        Item slab = ModItems.SLAB_PROJECT_BENCH.get();
        Item bench = ModItems.PROJECT_BENCH.get();
        BlockPos a = new BlockPos(1, 1, 0);
        place(helper, slab, a);
        place(helper, slab, a.east());
        assertPair(helper, a.east(), a, "slab and slab");

        BlockPos b = new BlockPos(1, 1, 2);
        place(helper, bench, b);
        place(helper, slab, b.east());
        assertPair(helper, b.east(), b, "bench and slab");
        helper.assertTrue(((ProjectBenchBlockEntity) helper.getBlockEntity(b)).externalPanels().isEmpty(),
                "the slab beside the bench is its other half, not an inventory beside it");

        BlockPos c = new BlockPos(1, 1, 4);
        place(helper, bench, c);
        place(helper, bench, c.east());
        assertAlone(helper, c, "two benches never join");
        assertAlone(helper, c.east(), "two benches never join");
        helper.assertTrue(((ProjectBenchBlockEntity) helper.getBlockEntity(c)).externalPanels().size() == 1,
                "a bench that is not a partner is an inventory beside the bench");

        BlockPos d = new BlockPos(1, 3, 0);
        place(helper, slab, d);
        place(helper, slab, d.east(), Direction.UP, WEST, false);
        helper.assertTrue(helper.getBlockState(d.east()).getValue(BenchBlock.FACING) == Direction.EAST, "placed looking west, it faces east");
        assertAlone(helper, d, "facing different ways");
        assertAlone(helper, d.east(), "facing different ways");

        BlockPos e = new BlockPos(1, 3, 2);
        place(helper, slab, e);
        place(helper, slab, e.above());
        assertAlone(helper, e, "one on top of the other");
        assertAlone(helper, e.above(), "one on top of the other");

        BlockPos f = new BlockPos(1, 3, 4);
        place(helper, slab, f);
        place(helper, slab, f.east(), Direction.UP, SOUTH, true);
        assertAlone(helper, f.east(), "placed sneaking without clicking the other");

        BlockPos g = new BlockPos(3, 3, 4);
        place(helper, slab, g.west(), Direction.EAST, WEST, true);
        helper.assertTrue(helper.getBlockState(g).getValue(BenchBlock.FACING) == Direction.NORTH, "sneaking onto a side, it takes its partner's facing");
        assertPair(helper, g, g.west(), "sneaking onto the side of a single slab");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void threeInARowMakeOnePair(GameTestHelper helper) {
        Item slab = ModItems.SLAB_PROJECT_BENCH.get();
        Item bench = ModItems.PROJECT_BENCH.get();
        BlockPos a = new BlockPos(0, 1, 0);
        place(helper, slab, a);
        place(helper, slab, a.east());
        place(helper, slab, a.east(2));
        assertPair(helper, a.east(), a, "slab, slab, slab");
        assertAlone(helper, a.east(2), "the third slab");
        helper.destroyBlock(a.east());
        assertAlone(helper, a, "the middle one broken");
        assertAlone(helper, a.east(2), "the middle one broken");

        BlockPos b = new BlockPos(0, 1, 2);
        place(helper, bench, b);
        place(helper, slab, b.east());
        place(helper, bench, b.east(2));
        assertPair(helper, b.east(), b, "bench, slab, bench");
        assertAlone(helper, b.east(2), "the second bench");

        BlockPos c = new BlockPos(0, 1, 4);
        place(helper, bench, c);
        place(helper, bench, c.east());
        place(helper, slab, c.east(2));
        assertAlone(helper, c, "bench, bench, slab: the first bench");
        assertPair(helper, c.east(2), c.east(), "bench, bench, slab");

        BlockPos d = new BlockPos(0, 3, 0);
        place(helper, slab, d);
        place(helper, slab, d.east(2));
        place(helper, slab, d.east());
        assertPair(helper, d.east(2), d.east(), "a slab put between two: it joins one");
        assertAlone(helper, d, "the other stays on its own");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void slabSwitchedOff(GameTestHelper helper) {
        BlockPos bench = new BlockPos(1, 1, 1);
        BlockPos slab = bench.east();
        place(helper, ModItems.PROJECT_BENCH.get(), bench);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), slab);
        assertPair(helper, slab, bench, "switched on");
        BlockPos lone = new BlockPos(1, 1, 3);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), lone);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            Config.override(Config.Settings.builder().slab(false).slabShare(true).build());
            helper.assertFalse(Feature.SLAB_BENCH.enabled(), "the slab's feature is off");
            helper.assertTrue(pair(helper, bench) == null && pair(helper, slab) == null, "a pair counts for nothing: each bench is on its own");
            helper.assertTrue(CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(slab),
                    new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(slab)), Direction.NORTH, helper.absolutePos(slab), false)).isCanceled(),
                    "a slab switched off does not open");
            helper.assertFalse(CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(bench),
                    new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(bench)), Direction.NORTH, helper.absolutePos(bench), false)).isCanceled(),
                    "the bench beside it opens its own window");
            BlockPos more = new BlockPos(1, 1, 4);
            place(helper, ModItems.SLAB_PROJECT_BENCH.get(), more);
            helper.assertTrue(helper.getBlockState(more).isAir(), "a slab switched off is not placed");
            place(helper, ModItems.PROJECT_BENCH.get(), lone.east());
            helper.assertTrue(type(helper, lone.east()) == ChestType.SINGLE && type(helper, lone) == ChestType.SINGLE,
                    "a bench placed beside a slab switched off does not join it");
        } finally {
            Config.clearOverride();
        }
        helper.assertTrue(pair(helper, bench) != null, "back on, the pair counts again");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void breakingOneHalfDropsOnlyItsOwn(GameTestHelper helper) {
        BlockPos bench = new BlockPos(1, 1, 1);
        BlockPos slab = bench.east();
        place(helper, ModItems.PROJECT_BENCH.get(), bench);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), slab);
        assertPair(helper, slab, bench, "bench and slab");
        ProjectBenchBlockEntity left = helper.getBlockEntity(slab);
        ProjectBenchBlockEntity right = helper.getBlockEntity(bench);
        left.storage().setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        left.matrix().setItem(4, new ItemStack(Items.STICK));
        right.storage().setItem(5, new ItemStack(Items.DIRT, 7));

        helper.getLevel().destroyBlock(helper.absolutePos(slab), true);
        assertAlone(helper, bench, "the bench left behind");
        helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 3 && dropped(helper, Items.STICK) == 1 && dropped(helper, ModItems.SLAB_PROJECT_BENCH.get()) == 1,
                "the slab drops itself and what it held");
        helper.assertTrue(dropped(helper, Items.DIRT) == 0 && GameTests.count(right.storage(), Items.DIRT) == 7, "the bench keeps its own");

        helper.getLevel().destroyBlock(helper.absolutePos(bench), true);
        helper.assertTrue(dropped(helper, Items.DIRT) == 7 && dropped(helper, ModItems.PROJECT_BENCH.get()) == 1
                && dropped(helper, Items.COBBLESTONE) == 3 && dropped(helper, Items.STICK) == 1, "then the bench drops its own, nothing twice");
        helper.succeed();
    }

    static ProjectBenchBlock.Pair joinedAt(GameTestHelper helper, BlockPos at) {
        place(helper, ModItems.PROJECT_BENCH.get(), at);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), at.east());
        assertPair(helper, at.east(), at, "bench and slab");
        return new ProjectBenchBlock.Pair(helper.getBlockEntity(at.east()), helper.getBlockEntity(at));
    }

    private static void layOutSticks(ProjectBenchBlockEntity bench) {
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
    }

    private static void refillByItem(GameTestHelper helper, ProjectBenchBlockEntity bench) {
        for (int i = 0; i < AutoMode.values().length && bench.autoMode() != AutoMode.ITEM; ++i) {
            bench.cycleAutoMode();
        }
        helper.assertTrue(bench.autoMode() == AutoMode.ITEM, "the A on ITEM");
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aCraftOnEitherHalfRefillsFromBoth(GameTestHelper helper) {
        ProjectBenchBlock.Pair pair = joinedAt(helper, new BlockPos(1, 1, 1));
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        refillByItem(helper, first);
        refillByItem(helper, second);

        first.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        layOutSticks(second);
        second.craft(player);
        helper.assertTrue(second.matrix().getItem(1).getCount() == 1 && second.matrix().getItem(4).getCount() == 1
                && count(first.storage(), Items.OAK_PLANKS) == 3, "the second half refilled from the first's inventory");

        first.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 1));
        second.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        layOutSticks(first);
        first.craft(player);
        helper.assertTrue(first.matrix().getItem(1).getCount() == 1 && first.matrix().getItem(4).getCount() == 1
                && count(first.storage(), Items.OAK_PLANKS) == 0 && count(second.storage(), Items.OAK_PLANKS) == 4,
                "the first half spent its own inventory first, then the second's");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void shareStorageOffKeepsEachHalfToItsOwn(GameTestHelper helper) {
        ProjectBenchBlock.Pair pair = joinedAt(helper, new BlockPos(1, 1, 1));
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        refillByItem(helper, second);
        first.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        try {
            Config.override(Config.Settings.builder().slab(true).slabShare(false).build());
            helper.assertTrue(pair(helper, new BlockPos(1, 1, 1)) != null, "still a double bench");
            layOutSticks(second);
            second.craft(player);
            helper.assertTrue(second.matrix().isEmpty() && count(first.storage(), Items.OAK_PLANKS) == 5,
                    "not sharing the stock: the matrix pays and the other half's inventory is left alone");
        } finally {
            Config.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void fillTakesFromBothAndClearGoesHomeFirst(GameTestHelper helper) {
        ProjectBenchBlock.Pair pair = joinedAt(helper, new BlockPos(1, 1, 1));
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        List<ItemStack> sticks = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            sticks.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        second.blueprintSlot().setItem(0, recorded(sticks, new ItemStack(Items.STICK, 4)));
        first.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        second.storage().setItem(3, new ItemStack(Items.OAK_PLANKS, 6));
        second.fillMatrix();
        helper.assertTrue(second.matrix().getItem(1).getCount() == 8 && second.matrix().getItem(4).getCount() == 8,
                "16 planks from both inventories over two positions: " + second.matrix().getItem(1) + " " + second.matrix().getItem(4));
        helper.assertTrue(count(first.storage(), Items.OAK_PLANKS) + count(second.storage(), Items.OAK_PLANKS) == 0, "all of them moved");

        second.clearMatrix();
        helper.assertTrue(second.matrix().isEmpty() && count(second.storage(), Items.OAK_PLANKS) == 16 && count(first.storage(), Items.OAK_PLANKS) == 0,
                "cleared into its own inventory first");

        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            second.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        second.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        second.clearMatrix();
        helper.assertTrue(second.matrix().isEmpty() && count(first.storage(), Items.IRON_INGOT) == 5, "its own full, into the other half's");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aDoubleChestBeforeBothCountsOnce(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        helper.setBlock(at.north(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(at.north().east(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        ChestBlockEntity near = helper.getBlockEntity(at.north());
        ChestBlockEntity far = helper.getBlockEntity(at.north().east());
        near.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        far.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        first.setShareMode(ShareMode.ALL);
        helper.assertTrue(first.external().count(new ItemStack(Items.COBBLESTONE)) == 8 && second.external().count(new ItemStack(Items.COBBLESTONE)) == 8,
                "each half alone sees the whole double chest");
        for (ProjectBenchBlockEntity half : new ProjectBenchBlockEntity[]{first, second}) {
            int counted = half.stock().count(new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(counted == 8, "the double bench counts the double chest once, not " + counted);
        }
        int taken = second.stock().take(new ItemStack(Items.COBBLESTONE), 20);
        helper.assertTrue(taken == 8 && count(near, Items.COBBLESTONE) + count(far, Items.COBBLESTONE) == 0, "it takes what there is, no more: " + taken);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void aBenchSwitchedOffIsNotBeside(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        place(helper, ModItems.PROJECT_BENCH.get(), at);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), at.south(), Direction.UP, WEST, false);
        assertAlone(helper, at, "a slab behind the bench");
        ProjectBenchBlockEntity bench = helper.getBlockEntity(at);
        ProjectBenchBlockEntity slab = helper.getBlockEntity(at.south());
        slab.storage().setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        bench.setShareMode(ShareMode.ALL);
        helper.assertTrue(bench.externalPanels().size() == 1 && bench.stock().count(new ItemStack(Items.COBBLESTONE)) == 4, "switched on, the slab is beside it");
        try {
            Config.override(Config.Settings.builder().slab(false).slabShare(true).build());
            helper.assertTrue(bench.externalPanels().isEmpty(), "switched off, the slab is no tab");
            helper.assertTrue(bench.stock().count(new ItemStack(Items.COBBLESTONE)) == 0 && bench.stock().take(new ItemStack(Items.COBBLESTONE), 4) == 0,
                    "and its inventory is no stock");
        } finally {
            Config.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shareSwitchesBothHalves(GameTestHelper helper) {
        ProjectBenchBlock.Pair pair = joinedAt(helper, new BlockPos(1, 1, 1));
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        helper.assertTrue(first.shareMode() == second.shareMode(), "one button for both");
        ShareMode before = first.shareMode();
        second.cycleShareMode();
        helper.assertTrue(first.shareMode() == before.next() && second.shareMode() == before.next(), "cycled on the second, both changed");
        first.cycleShareMode();
        helper.assertTrue(first.shareMode() == before.next().next() && second.shareMode() == before.next().next(), "cycled on the first, both follow");
        helper.succeed();
    }

    private static String legs(GameTestHelper helper, BlockPos at) {
        BlockState state = helper.getBlockState(at);
        helper.assertTrue(state.getBlock() instanceof BenchSlabBlock, "a slab at " + at);
        StringBuilder sides = new StringBuilder();
        for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            sides.append(state.getValue(BenchSlabBlock.legs(side)) ? side.getName().toUpperCase().charAt(0) : '-');
        }
        return sides.toString();
    }

    private static void assertLegs(GameTestHelper helper, BlockPos at, String expected, String what) {
        String actual = legs(helper, at);
        helper.assertTrue(actual.equals(expected), what + ": legs " + expected + ", not " + actual);
    }

    private static double bottom(GameTestHelper helper, BlockPos at) {
        return helper.getBlockState(at).getShape(helper.getLevel(), helper.absolutePos(at)).min(Direction.Axis.Y);
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aSideAgainstASolidFaceHasNoLegs(GameTestHelper helper) {
        Item slab = ModItems.SLAB_PROJECT_BENCH.get();
        BlockPos at = new BlockPos(2, 1, 2);
        place(helper, slab, at);
        assertLegs(helper, at, "NSWE", "on its own");
        helper.setBlock(at.south(), Blocks.STONE);
        assertLegs(helper, at, "N-WE", "a wall behind");
        helper.setBlock(at.east(), Blocks.FURNACE);
        assertLegs(helper, at, "N-W-", "in a corner, so one leg, north-west");
        helper.assertTrue(bottom(helper, at) == 0, "in a corner it still stands on its one leg");
        helper.setBlock(at.south(), Blocks.AIR);
        helper.setBlock(at.east(), Blocks.AIR);
        assertLegs(helper, at, "NSWE", "the walls taken away");

        helper.setBlock(at.west().below(), Blocks.STONE);
        helper.setBlock(at.west(), Blocks.TORCH);
        helper.setBlock(at.east().below(), Blocks.DIRT);
        helper.setBlock(at.east(), Blocks.POPPY);
        assertLegs(helper, at, "NSWE", "a torch and a flower beside it");
        helper.assertTrue(helper.getBlockState(at.west()).is(Blocks.TORCH) && helper.getBlockState(at.east()).is(Blocks.POPPY), "the torch and the flower stay");

        BlockPos placed = new BlockPos(2, 3, 2);
        helper.setBlock(placed.south(), Blocks.STONE);
        helper.setBlock(placed.west(), Blocks.CHEST);
        place(helper, slab, placed);
        assertLegs(helper, placed, "N--E", "placed in a corner");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aPairHasNoLegsInsideAndAPileKeepsThem(GameTestHelper helper) {
        Item slab = ModItems.SLAB_PROJECT_BENCH.get();
        Item bench = ModItems.PROJECT_BENCH.get();
        BlockPos a = new BlockPos(1, 1, 2);
        place(helper, slab, a);
        place(helper, slab, a.east());
        assertPair(helper, a.east(), a, "slab and slab");
        assertLegs(helper, a, "NSW-", "the west half");
        assertLegs(helper, a.east(), "NS-E", "the east half");
        helper.destroyBlock(a.east());
        assertLegs(helper, a, "NSWE", "its partner broken");

        BlockPos b = new BlockPos(1, 3, 2);
        place(helper, slab, b);
        place(helper, slab, b.above());
        assertLegs(helper, b, "NSWE", "the slab underneath");
        assertLegs(helper, b.above(), "NSWE", "a slab on a slab stands on the board below");
        helper.assertTrue(bottom(helper, b.above()) == 0, "a slab on a slab keeps its legs");

        BlockPos c = new BlockPos(3, 3, 2);
        place(helper, bench, c);
        place(helper, slab, c.above());
        assertLegs(helper, c.above(), "NSWE", "a slab on a bench");
        helper.setBlock(c.above().south(), Blocks.STONE);
        assertLegs(helper, c.above(), "N-WE", "a slab on a bench, with a wall behind");
        helper.succeed();
    }

    private static Player playerAt(GameTestHelper helper, BlockPos at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(at)));
        return player;
    }

    private static ProjectBenchMenu station(ProjectBenchBlockEntity half, Player player) {
        ProjectBenchMenu menu = half.opening().menu(0, player.getInventory());
        if (!menu.station()) {
            throw new GameTestAssertException("a double bench opens the Project Station");
        }
        return menu;
    }

    private static void makeActive(GameTestHelper helper, ProjectBenchMenu menu, Player player, int which) {
        if (menu.active() != which) {
            helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_ACTIVE), "the window takes the active button");
        }
        helper.assertTrue(menu.active() == which, "matrix " + (which + 1) + " active");
    }

    @GameTest(template = GameTests.EMPTY)
    public static void eitherHalfOpensTheStationLeftFirst(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player player = playerAt(helper, at);
        for (ProjectBenchBlockEntity half : new ProjectBenchBlockEntity[]{pair.first(), pair.second()}) {
            ProjectBenchMenu menu = station(half, player);
            helper.assertTrue(menu.benchOf(0) == pair.first() && menu.benchOf(1) == pair.second(), "the half on the left is matrix 1");
            helper.assertTrue(menu.active() == 0, "matrix 1 is active to begin with");
            helper.assertTrue(menu.getSlot(menu.matrixStart(1)).container == pair.second().matrix()
                    && menu.getSlot(menu.storageStart(1)).container == pair.second().storage(), "the second half's slots come last");
            helper.assertTrue(menu.slots.size() == ProjectBenchMenu.ARCHIVE_START + ProjectBenchMenu.OUTSIDE_VISIBLE + ProjectBenchMenu.BENCH_SLOTS + 2
                    + ProjectBenchMenu.PROCESSOR_SLOTS, "one more bench's slots, a cost slot per half, then a processor tab's: " + menu.slots.size());
            helper.assertTrue(menu.stillValid(player), "the window stays open");
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theViewersRecordOnTheActiveHalfsBlueprint(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        player.containerMenu = menu;
        pair.first().blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
        pair.second().blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
        pair.second().storage().setItem(0, new ItemStack(Items.STICK));
        pair.second().costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        makeActive(helper, menu, player, 1);
        ResourceLocation torch = ResourceLocation.withDefaultNamespace("torch");
        RecordRecipe.receive(player, new RecordRecipe(torch, true));
        Blueprint bp = pair.second().blueprint();
        helper.assertTrue(bp != null && bp.recipe().equals(torch), "matrix 2's blueprint holds the torch: " + bp);
        helper.assertTrue(pair.first().blueprint() == null && pair.second().costSlot().isEmpty(), "matrix 1's stays blank, matrix 2's dye paid");
        helper.assertTrue(pair.second().matrix().getItem(3).is(Items.STICK) && pair.first().matrix().isEmpty(), "the stick goes to matrix 2");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void eachHalfPaysFromItsOwnCostSlot(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        Slot left = menu.getSlot(menu.costIndex(0));
        Slot right = menu.getSlot(menu.costIndex(1));
        helper.assertTrue(left.container == pair.first().costSlot() && right.container == pair.second().costSlot()
                && menu.costIndex(1) == menu.slots.size() - 1 - ProjectBenchMenu.PROCESSOR_SLOTS, "a cost slot per half, the last two before a processor tab's");
        helper.assertTrue(left.x == ProjectBenchMenu.halfX(0, ProjectBenchMenu.COST_X, 16) && right.x == ProjectBenchMenu.halfX(1, ProjectBenchMenu.COST_X, 16)
                && left.x + right.x + 16 == ProjectBenchMenu.STATION_WIDTH, "in mirror, each beside its own blueprint");
        for (ProjectBenchBlockEntity half : new ProjectBenchBlockEntity[]{pair.first(), pair.second()}) {
            half.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
            layOutSticks(half);
        }
        pair.first().costSlot().setItem(0, new ItemStack(Items.WHITE_DYE, 2));
        makeActive(helper, menu, player, 1);
        helper.assertTrue(!menu.canRecord() && menu.missingCost() == 1, "matrix 2 is active and its cost slot is empty: 'record' is off");
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD);
        helper.assertTrue(pair.second().blueprint() == null && pair.first().costSlot().getItem(0).getCount() == 2,
                "the other half's dye does not pay for it");
        pair.second().costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD) && pair.second().blueprint() != null
                && pair.second().costSlot().isEmpty() && pair.first().costSlot().getItem(0).getCount() == 2, "its own dye does");
        helper.assertTrue(pair.first().blueprint() == null, "and matrix 1's blueprint stays blank");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickTopsUpTheActiveMatrix(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        pair.first().matrix().setItem(0, new ItemStack(Items.OAK_PLANKS, 60));
        pair.second().matrix().setItem(0, new ItemStack(Items.OAK_PLANKS, 60));
        makeActive(helper, menu, player, 1);
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(pair.second().matrix().getItem(0).getCount() == 64 && pair.first().matrix().getItem(0).getCount() == 60,
                "matrix 2 topped up, matrix 1 untouched: " + pair.second().matrix().getItem(0) + " " + pair.first().matrix().getItem(0));
        helper.assertTrue(count(pair.second().storage(), Items.OAK_PLANKS) == 6 && count(pair.first().storage(), Items.OAK_PLANKS) == 0,
                "the rest in the active half's inventory");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aBenchAloneOpensItsOwnWindow(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        place(helper, ModItems.SLAB_PROJECT_BENCH.get(), at);
        ProjectBenchBlockEntity slab = helper.getBlockEntity(at);
        ProjectBenchBlockEntity.Opening opening = slab.opening();
        ProjectBenchMenu menu = opening.menu(0, playerAt(helper, at).getInventory());
        helper.assertTrue(opening.second() == null && !menu.station() && menu.active() == 0, "a slab on its own is a bench");
        helper.assertTrue(menu.slots.size() == ProjectBenchMenu.ARCHIVE_START + ProjectBenchMenu.OUTSIDE_VISIBLE + 1 + ProjectBenchMenu.PROCESSOR_SLOTS
                && menu.matrixStart(1) == ProjectBenchMenu.MATRIX_START, "with the bench's slots, as always");
        helper.assertFalse(menu.clickMenuButton(playerAt(helper, at), ProjectBenchMenu.BUTTON_ACTIVE), "no active button");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theActiveMatrixIsTheDoubleBenchs(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player one = playerAt(helper, at);
        Player two = playerAt(helper, at);
        ProjectBenchMenu mine = station(pair.first(), one);
        ProjectBenchMenu theirs = station(pair.second(), two);
        helper.assertTrue(mine.clickMenuButton(one, ProjectBenchMenu.BUTTON_ACTIVE), "the window takes the button");
        helper.assertTrue(mine.active() == 1 && theirs.active() == 1 && pair.first().activeSecond(), "both windows see matrix 2 active");
        helper.assertTrue(theirs.activeBench() == pair.second(), "and work on the second half");
        theirs.clickMenuButton(two, ProjectBenchMenu.BUTTON_ACTIVE);
        helper.assertTrue(mine.active() == 0 && theirs.active() == 0, "the other player switches it back for both");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickFromThePlayerGoesToTheActiveHalf(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        List<ItemStack> sticks = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            sticks.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        second.blueprintSlot().setItem(0, recorded(sticks, new ItemStack(Items.STICK, 4)));
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(first, player);
        makeActive(helper, menu, player, 1);
        int hotbar = ProjectBenchMenu.PLAYER_END - 9;
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 6));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(second.matrix().getItem(1).getCount() == 3 && second.matrix().getItem(4).getCount() == 3 && first.matrix().isEmpty(),
                "the planks went to the active matrix's ghosts: " + second.matrix().getItem(1) + " " + second.matrix().getItem(4));
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(second.storage(), Items.COBBLESTONE) == 10 && count(first.storage(), Items.COBBLESTONE) == 0, "cobblestone to the active half's storage");
        makeActive(helper, menu, player, 0);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 5));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(first.storage(), Items.DIRT) == 5 && count(second.storage(), Items.DIRT) == 0, "dirt to the first half's, now active");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theInactiveMatrixCraftsAndRefillsByItsOwnA(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        refillByItem(helper, second);
        first.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        layOutSticks(second);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(first, player);
        makeActive(helper, menu, player, 0);
        helper.assertTrue(menu.getSlot(menu.resultIndex(1)).getItem().is(Items.STICK), "the second matrix makes sticks");
        menu.clicked(menu.resultIndex(1), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 4, "crafted from the inactive half's result");
        helper.assertTrue(second.matrix().getItem(1).getCount() == 1 && second.matrix().getItem(4).getCount() == 1
                && count(first.storage(), Items.OAK_PLANKS) == 2, "and its A refilled it from the common stock");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void eachMatrixHasItsOwnBalanceAndClearAndFillWorksOnTheActive(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(first, player);
        first.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        second.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        second.matrix().setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        makeActive(helper, menu, player, 0);
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.balanceButton(1)), "the window takes the second Balance");
        helper.assertTrue(second.matrix().getItem(0).getCount() + second.matrix().getItem(1).getCount() == 5
                && Math.abs(second.matrix().getItem(0).getCount() - second.matrix().getItem(1).getCount()) <= 1
                && first.matrix().getItem(0).getCount() == 2, "the second Balance evens out the second matrix, active or not");
        menu.clickMenuButton(player, ProjectBenchMenu.clearButton(1));
        helper.assertTrue(second.matrix().isEmpty() && count(second.storage(), Items.IRON_INGOT) == 5 && first.matrix().getItem(0).getCount() == 2,
                "the second Clear empties the second matrix into its storage");
        List<ItemStack> layout = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            layout.add(i == 4 ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY);
        }
        second.blueprintSlot().setItem(0, recorded(layout, new ItemStack(Items.IRON_NUGGET, 9)));
        makeActive(helper, menu, player, 1);
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_FILL);
        helper.assertTrue(second.matrix().getItem(4).is(Items.IRON_INGOT) && first.matrix().getItem(0).getCount() == 2, "Fill lays the active matrix out");
        menu.clickMenuButton(player, ProjectBenchMenu.clearButton(0));
        helper.assertTrue(first.matrix().isEmpty() && second.matrix().getItem(4).is(Items.IRON_INGOT), "with matrix 2 active, the first Clear empties matrix 1");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void inventoryModsSeeOnlyTheInternalInventories(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        helper.setBlock(at.north(), Blocks.BARREL);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        helper.assertTrue(menu.outsideSlots() > 0, "the barrel's tab is there");
        int seen = 0;
        for (Slot slot : menu.slots) {
            boolean sortable = !(slot.container instanceof Inventory) && !slot.isFake() && slot.isActive() && slot.x >= 0 && slot.y >= 0
                    && !(slot.container instanceof CraftingContainer) && !(slot.container instanceof ResultContainer);
            boolean storage = slot.container == pair.first().storage() || slot.container == pair.second().storage();
            helper.assertTrue(sortable == storage, "slot " + slot.index + " (" + slot.getClass().getSimpleName() + ") is "
                    + (storage ? "internal inventory, left out" : "not internal inventory, sorted"));
            seen += sortable ? 1 : 0;
        }
        helper.assertTrue(seen == 2 * ProjectBenchBlockEntity.STORAGE, "both internal inventories, not " + seen + " slots");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theStationHasATabForEveryInventoryBeside(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        BlockPos left = at.east();
        for (BlockPos half : new BlockPos[]{left, at}) {
            helper.setBlock(half.north(), Blocks.BARREL);
            helper.setBlock(half.south(), Blocks.BARREL);
            helper.setBlock(half.below(), Blocks.BARREL);
        }
        helper.setBlock(left.east(), Blocks.BARREL);
        helper.setBlock(at.west(), Blocks.BARREL);
        int tabs = pair.second().opening().outside().size();
        helper.assertTrue(tabs == 8, "eight inventories, eight tabs, not " + tabs);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_TAB + 7);
        helper.assertTrue(menu.selectedTab() == 7, "the eighth tab opens");
        ItemStack put = new ItemStack(Items.COBBLESTONE, 3);
        player.getInventory().setItem(0, put);
        pair.first().setShareMode(ShareMode.OPEN);
        for (ProjectBenchBlockEntity half : new ProjectBenchBlockEntity[]{pair.first(), pair.second()}) {
            for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
                half.storage().setItem(i, new ItemStack(Items.DIRT, 64));
            }
        }
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        int inBarrels = 0;
        for (BlockPos barrel : new BlockPos[]{left.north(), left.south(), left.below(), left.east(), at.north(), at.south(), at.below(), at.west()}) {
            inBarrels += count((Container) helper.getBlockEntity(barrel), Items.COBBLESTONE);
        }
        helper.assertTrue(inBarrels == 3, "shift-click sends to the second half's tab: " + inBarrels);

        for (BlockPos half : new BlockPos[]{left, at}) {
            helper.setBlock(half.north(), Blocks.AIR);
        }
        helper.setBlock(at.north(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(left.north(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        tabs = pair.first().opening().outside().size();
        helper.assertTrue(tabs == 7, "a double chest in front of both is one tab: " + tabs);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void breakingAHalfClosesTheStation(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = joinedAt(helper, at);
        Player player = playerAt(helper, at);
        ProjectBenchMenu menu = station(pair.first(), player);
        helper.assertTrue(menu.stillValid(player), "open while both stand");
        helper.destroyBlock(at);
        helper.assertFalse(menu.stillValid(player), "the second half broken, the window closes");
        helper.succeed();
    }
}
