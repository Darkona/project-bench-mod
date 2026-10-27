package com.darkona.projectbench.infrastructure.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import com.darkona.projectbench.config.SideMode;
import com.darkona.projectbench.content.bench.AutoBenchBlock;
import com.darkona.projectbench.content.bench.Opening;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.foundation.BenchBlock;
import com.darkona.projectbench.index.BenchBlocks;
import com.darkona.projectbench.index.BenchDataComponents;
import com.darkona.projectbench.index.BenchItems;
import dev.darkona.jdlib.api.world.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.projectbench.infrastructure.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutocraftGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final int AFTER_PULSE = 6;

    private AutocraftGameTests() {
    }

    private static ProjectBenchBlockEntity auto(GameTestHelper helper, boolean ejecting) {
        helper.setBlock(BENCH, (ejecting ? BenchBlocks.EJECTING_AUTO_PROJECT_BENCH : BenchBlocks.AUTO_PROJECT_BENCH).get().defaultBlockState()
                .setValue(BenchBlock.FACING, Direction.NORTH));
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        for (int i = 0; i < AutoMode.values().length && bench.autoMode() != AutoMode.ITEM; ++i) {
            bench.cycleAutoMode();
        }
        helper.assertTrue(bench.automates(), "a new automatic bench automates");
        return bench;
    }

    private static void sticks(ProjectBenchBlockEntity bench, int spare) {
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        if (spare > 0) {
            bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, spare));
        }
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, Blocks.CHEST);
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

    private static int planks(ProjectBenchBlockEntity bench) {
        return count(bench.matrix(), Items.OAK_PLANKS) + count(bench.storage(), Items.OAK_PLANKS);
    }

    private static int thrown(GameTestHelper helper, Item item) {
        int n = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BENCH)).inflate(4))) {
            if (entity.getItem().is(item)) {
                n += entity.getItem().getCount();
            }
        }
        return n;
    }

    private static boolean triggered(GameTestHelper helper) {
        return helper.getBlockState(BENCH).getValue(AutoBenchBlock.TRIGGERED);
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aPulseCraftsOnceIntoTheChestBelow(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 10);
        ChestBlockEntity below = chest(helper, BENCH.below());
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(BENCH.east(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(AFTER_PULSE, () -> {
                    helper.assertTrue(triggered(helper), "the bench is triggered");
                    helper.assertTrue(count(below, Items.STICK) == 4, "one craft, four sticks in the chest below: " + count(below, Items.STICK));
                    helper.assertTrue(bench.matrix().getItem(1).getCount() == 1 && bench.matrix().getItem(4).getCount() == 1
                            && count(bench.storage(), Items.OAK_PLANKS) == 8, "one plank a slot paid, the A refilled the matrix from the inventory");
                    helper.setBlock(BENCH.west(), Blocks.REDSTONE_BLOCK);
                })
                .thenExecuteAfter(AFTER_PULSE, () -> {
                    helper.assertTrue(count(below, Items.STICK) == 4, "a second signal while still powered crafts nothing");
                    helper.setBlock(BENCH.east(), Blocks.AIR);
                    helper.setBlock(BENCH.west(), Blocks.AIR);
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertFalse(triggered(helper), "the signal went off");
                    helper.setBlock(BENCH.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenExecuteAfter(AFTER_PULSE, () -> helper.assertTrue(count(below, Items.STICK) == 8 && planks(bench) == 8,
                        "off and on again: another craft"))
                .thenSucceed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void noRoomBelowNoCraft(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 10);
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(BENCH.east(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(AFTER_PULSE, () -> {
                    helper.assertTrue(planks(bench) == 12 && thrown(helper, Items.STICK) == 0, "nothing below: nothing crafted, nothing thrown");
                    ChestBlockEntity below = chest(helper, BENCH.below());
                    for (int i = 0; i < below.getContainerSize(); ++i) {
                        below.setItem(i, new ItemStack(Items.DIRT, 64));
                    }
                    helper.setBlock(BENCH.east(), Blocks.AIR);
                })
                .thenExecuteAfter(1, () -> helper.setBlock(BENCH.east(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(AFTER_PULSE, () -> helper.assertTrue(planks(bench) == 12 && thrown(helper, Items.STICK) == 0,
                        "a full chest below: nothing crafted"))
                .thenSucceed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void pullingFromBelowCrafts(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 2);
        IItemHandler down = handler(helper, BENCH, Direction.DOWN);
        helper.assertTrue(down.getSlots() == 1 && bench.sideMode(Direction.DOWN) == SideMode.RESULT,
                "the bottom is the result");
        helper.assertTrue(down.insertItem(0, new ItemStack(Items.OAK_PLANKS), false).getCount() == 1, "nothing goes in");
        helper.assertTrue(down.extractItem(0, 4, true).getCount() == 4 && planks(bench) == 4, "simulate: four sticks, nothing spent");
        ItemStack out = down.extractItem(0, 4, false);
        helper.assertTrue(out.is(Items.STICK) && out.getCount() == 4 && planks(bench) == 2, "a pull of four: one craft");

        ItemStack one = down.extractItem(0, 1, false);
        helper.assertTrue(one.getCount() == 1 && planks(bench) == 0 && count(bench.storage(), Items.STICK) == 3,
                "a pull of one (a hopper): one craft, the other three wait in the inventory");
        helper.assertTrue(down.extractItem(0, 64, false).getCount() == 0, "no planks left: the matrix makes nothing, so nothing comes out");
        sticks(bench, 0);
        ItemStack waiting = down.extractItem(0, 64, false);
        helper.assertTrue(waiting.getCount() == 3 && planks(bench) == 2 && count(bench.storage(), Items.STICK) == 0,
                "the three that waited come out first, without a craft");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void remaindersGoInsideThenBelow(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        bench.setShareMode(ShareMode.NONE);
        ChestBlockEntity below = chest(helper, BENCH.below());
        bench.matrix().setItem(0, new ItemStack(Items.HONEY_BOTTLE, 2));
        bench.craftOnPulse();
        helper.assertTrue(count(below, Items.SUGAR) == 3 && count(bench.storage(), Items.GLASS_BOTTLE) == 1 && count(below, Items.GLASS_BOTTLE) == 0,
                "the sugar below, the bottle inside");
        for (int i = 0; i < bench.storage().getContainerSize(); ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        bench.craftOnPulse();
        helper.assertTrue(count(below, Items.SUGAR) == 6 && count(below, Items.GLASS_BOTTLE) == 1, "the inventory full: the bottle goes below too");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theEjectingBenchThrowsOutOfItsBack(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, true);
        sticks(bench, 10);
        ChestBlockEntity below = chest(helper, BENCH.below());
        ChestBlockEntity behind = chest(helper, BENCH.south());
        bench.craftOnPulse();
        helper.assertTrue(count(behind, Items.STICK) == 4 && count(below, Items.STICK) == 0 && thrown(helper, Items.STICK) == 0,
                "a chest behind: the sticks go in");
        behind.clearContent();
        helper.setBlock(BENCH.south(), Blocks.AIR);
        bench.craftOnPulse();
        helper.assertTrue(thrown(helper, Items.STICK) == 4 && count(below, Items.STICK) == 0, "nothing behind: thrown");
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BENCH)).inflate(4))) {
            Vec3 at = entity.position().subtract(Vec3.atCenterOf(helper.absolutePos(BENCH)));
            helper.assertTrue(at.z > 0.5 && entity.getDeltaMovement().z > 0, "out of the back (south), flying away: " + at);
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theButtonOffStopsIt(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 10);
        ChestBlockEntity below = chest(helper, BENCH.below());
        IItemHandler down = handler(helper, BENCH, Direction.DOWN);
        bench.toggleAutocraft();
        helper.assertTrue(!bench.automates() && bench.autocraftButton() == 1, "the button is off");
        helper.assertTrue(down.extractItem(0, 4, false).isEmpty(), "a handler asked for before goes quiet");
        helper.assertTrue(maybe(helper, BENCH, Direction.DOWN) == null, "and the bottom has none now");
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(BENCH.east(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(AFTER_PULSE, () -> {
                    helper.assertTrue(count(below, Items.STICK) == 0 && planks(bench) == 12, "the pulse crafts nothing");
                    bench.toggleAutocraft();
                    helper.assertTrue(bench.automates() && maybe(helper, BENCH, Direction.DOWN) != null, "back on, the bottom opens");
                })
                .thenSucceed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void autocraftOffLeavesAPlainBench(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 10);
        ChestBlockEntity below = chest(helper, BENCH.below());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().autocraft(false).autocraftDefault(true).build());
            helper.assertFalse(bench.automates() || bench.autocraftButton() != 0, "it does not automate and has no button");
            helper.assertTrue(Feature.allows(helper.getBlockState(BENCH).getBlock()), "it still opens and works");
            helper.assertFalse(Feature.allows(BenchItems.AUTO_PROJECT_BENCH.get()) || Feature.allows(BenchItems.EJECTING_AUTO_PROJECT_BENCH.get()),
                    "but no more can be placed");
            bench.craftOnPulse();
            helper.assertTrue(count(below, Items.STICK) == 0 && planks(bench) == 12, "a pulse crafts nothing");
            helper.assertTrue(bench.sideMode(Direction.DOWN) == BenchConfig.DEFAULTS.benchSide(RelativeSide.DOWN),
                    "the sides are a plain bench's");
            bench.craft(player);
            helper.assertTrue(planks(bench) == 10, "crafting by hand works");
            helper.assertTrue(upgrade(helper, upgradeGrid(new ItemStack(BenchItems.PROJECT_BENCH.get()))).isEmpty(), "and the upgrade recipe does not match");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.assertTrue(bench.automates(), "back on, it automates again");
        helper.succeed();
    }

    private static CraftingInput upgradeGrid(ItemStack bench) {
        return CraftingInput.of(3, 3, List.of(new ItemStack(Items.IRON_INGOT), bench, new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.REDSTONE), new ItemStack(Items.CRAFTER), new ItemStack(Items.REDSTONE),
                new ItemStack(Items.PISTON), new ItemStack(Items.LEVER), new ItemStack(Items.PISTON)));
    }

    private static ItemStack upgrade(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(template = GameTests.EMPTY)
    public static void upgradingKeepsWhatTheBenchHolds(GameTestHelper helper) {
        ItemContainerContents held = ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3), ItemStack.EMPTY, new ItemStack(Items.STICK, 9)));
        ItemStack bench = new ItemStack(BenchItems.PROJECT_BENCH.get());
        bench.set(BenchDataComponents.CONTENTS.get(), held);
        ItemContainerContents locks = ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND)));
        bench.set(BenchDataComponents.SLOT_LOCKS.get(), locks);
        ItemStack auto = upgrade(helper, upgradeGrid(bench));
        helper.assertTrue(auto.is(BenchItems.AUTO_PROJECT_BENCH.get()) && held.equals(auto.get(BenchDataComponents.CONTENTS.get())),
                "a bench with contents makes an Auto with the same contents: " + auto);
        helper.assertTrue(locks.equals(auto.get(BenchDataComponents.SLOT_LOCKS.get())), "and the same slot locks");
        ItemStack ejecting = upgrade(helper, CraftingInput.of(2, 1, List.of(new ItemStack(Items.DROPPER), auto)));
        helper.assertTrue(ejecting.is(BenchItems.EJECTING_AUTO_PROJECT_BENCH.get()) && held.equals(ejecting.get(BenchDataComponents.CONTENTS.get())),
                "an Auto and a dropper make the ejecting one, contents and all: " + ejecting);
        ItemStack empty = upgrade(helper, upgradeGrid(new ItemStack(BenchItems.PROJECT_BENCH.get())));
        helper.assertTrue(empty.is(BenchItems.AUTO_PROJECT_BENCH.get()) && !empty.has(BenchDataComponents.CONTENTS.get()), "an empty bench, an empty Auto");

        helper.setBlock(BENCH, Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, ejecting);
        BlockPos abs = helper.absolutePos(BENCH);
        ejecting.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        ProjectBenchBlockEntity placed = helper.getBlockEntity(BENCH);
        helper.assertTrue(count(placed.matrix(), Items.DIAMOND) == 3 && count(placed.matrix(), Items.STICK) == 9,
                "placed, the ejecting bench holds what the first bench held");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void anAutomaticBenchNeverJoinsASlab(GameTestHelper helper) {
        place(helper, BenchItems.AUTO_PROJECT_BENCH.get(), BENCH);
        place(helper, BenchItems.SLAB_PROJECT_BENCH.get(), BENCH.east());
        BlockPos slabFirst = new BlockPos(1, 1, 3);
        place(helper, BenchItems.SLAB_PROJECT_BENCH.get(), slabFirst);
        place(helper, BenchItems.EJECTING_AUTO_PROJECT_BENCH.get(), slabFirst.west());
        for (BlockPos at : new BlockPos[]{BENCH, BENCH.east(), slabFirst, slabFirst.west()}) {
            helper.assertTrue(helper.getBlockState(at).getValue(ProjectBenchBlock.TYPE) == ChestType.SINGLE
                    && ProjectBenchBlock.pair(helper.getBlockState(at), helper.getLevel(), helper.absolutePos(at)) == null,
                    "on its own: " + helper.getBlockState(at));
        }
        ProjectBenchBlockEntity auto = helper.getBlockEntity(BENCH);
        ProjectBenchBlockEntity slab = helper.getBlockEntity(BENCH.east());
        helper.assertTrue(auto.partner() == null && slab.partner() == null && !Opening.of(auto).menu(0, helper.makeMockPlayer(GameType.SURVIVAL).getInventory()).station(),
                "no Project Station");
        helper.assertTrue(auto.automates() && auto.sideMode(Direction.EAST) != SideMode.NONE, "the Auto automates, its side toward the slab open");
        sticks(auto, 0);
        ChestBlockEntity below = chest(helper, BENCH.below());
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(BENCH.west(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(AFTER_PULSE, () -> helper.assertTrue(count(below, Items.STICK) == 4, "a pulse crafts on the Auto alone"))
                .thenSucceed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void limitedCraftingDoesNotStopIt(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = auto(helper, false);
        sticks(bench, 10);
        ChestBlockEntity below = chest(helper, BENCH.below());
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING);
        boolean was = rule.get();
        try {
            rule.set(true, helper.getLevel().getServer());
            bench.craftOnPulse();
        } finally {
            rule.set(was, helper.getLevel().getServer());
        }
        helper.assertTrue(count(below, Items.STICK) == 4, "crafted with limited crafting on");
        helper.succeed();
    }

    private static void place(GameTestHelper helper, Item item, BlockPos at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockPos abs = helper.absolutePos(at);
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
    }
}
