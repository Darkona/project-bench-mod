package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.BreakMode;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.registry.ModComponents;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import static com.darkona.projectbench.gametest.GameTests.count;

import java.util.List;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InventoryGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final int TOP = 1;
    private static final int UNDER = 4;

    private InventoryGameTests() {
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModBlocks.PROJECT_BENCH.get());
        return helper.getBlockEntity(at);
    }

    private static void fill(ProjectBenchBlockEntity bench) {
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        bench.storage().setItem(17, new ItemStack(Items.COBBLESTONE, 4));
        bench.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE, 3));
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos at, Direction side) {
        IItemHandler h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), side);
        helper.assertTrue(h != null, "an item handler at " + at + " on " + side);
        return h;
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
    public static void breakingDropsEverythingOnce(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        BlockPos at = BENCH.east();
        helper.setBlock(at, ModBlocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(at);
        archive.blueprints().setItem(4, new ItemStack(ModItems.BLUEPRINT.get(), 3));

        helper.getLevel().destroyBlock(helper.absolutePos(BENCH), true);
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 10, "two planks of the matrix and eight of the inventory: " + dropped(helper, Items.OAK_PLANKS));
        helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 4, "four cobblestone");
        helper.assertTrue(dropped(helper, Items.STICK) == 0, "the result is no item of its own");
        helper.assertTrue(dropped(helper, ModItems.BLUEPRINT.get()) == 4, "the bench's blueprint and the archive's three");
        helper.assertTrue(dropped(helper, ModItems.PROJECT_BENCH.get()) == 1 && dropped(helper, ModItems.BLUEPRINT_ARCHIVE.get()) == 1,
                "and each block itself");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void savedAndLoadedAsItWas(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        CompoundTag tag = bench.saveWithFullMetadata(helper.getLevel().registryAccess());
        ProjectBenchBlockEntity loaded = (ProjectBenchBlockEntity) ProjectBenchBlockEntity.loadStatic(bench.getBlockPos(), bench.getBlockState(),
                tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null, "it loads");
        for (int i = 0; i < ProjectBenchBlockEntity.MATRIX; ++i) {
            helper.assertTrue(ItemStack.matches(loaded.matrix().getItem(i), bench.matrix().getItem(i)), "matrix slot " + i);
        }
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            helper.assertTrue(ItemStack.matches(loaded.storage().getItem(i), bench.storage().getItem(i)), "inventory slot " + i);
        }
        helper.assertTrue(ItemStack.matches(loaded.blueprintSlot().getItem(0), bench.blueprintSlot().getItem(0)), "the blueprint");

        BlockPos at = BENCH.east();
        helper.setBlock(at, ModBlocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(at);
        archive.blueprints().setItem(35, new ItemStack(ModItems.BLUEPRINT.get(), 7));
        BlueprintArchiveBlockEntity again = (BlueprintArchiveBlockEntity) BlueprintArchiveBlockEntity.loadStatic(archive.getBlockPos(),
                archive.getBlockState(), archive.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(again != null && again.blueprints().getItem(35).getCount() == 7, "the archive's blueprints too");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void movedWholeDropsNothingAndWorksWhereItLands(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        CompoundTag tag = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        Clearable.tryClear(bench);
        helper.setBlock(BENCH, Blocks.AIR);
        helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "nothing drops where it stood");

        BlockPos to = new BlockPos(3, 1, 3);
        helper.setBlock(to, ModBlocks.PROJECT_BENCH.get().defaultBlockState().rotate(Rotation.CLOCKWISE_90));
        ProjectBenchBlockEntity moved = helper.getBlockEntity(to);
        moved.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(count(moved.matrix(), Items.OAK_PLANKS) == 2 && count(moved.storage(), Items.OAK_PLANKS) == 8
                && count(moved.storage(), Items.COBBLESTONE) == 4 && moved.blueprintSlot().getItem(0).is(ModItems.BLUEPRINT.get())
                && count(moved.costSlot(), Items.WHITE_DYE) == 3, "the same items where it lands, the cost slot's too");
        helper.assertTrue(moved.result().getItem(0).is(Items.STICK), "and its recipe: " + moved.result().getItem(0));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), moved, moved.archives(), List.of());
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.STICK) && count(moved.storage(), Items.OAK_PLANKS) == 6, "it crafts there, paying from its inventory");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void closingTheWindowKeepsEverything(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
        menu.removed(player);
        helper.assertTrue(count(bench.matrix(), Items.OAK_PLANKS) == 2 && count(bench.storage(), Items.OAK_PLANKS) == 8, "the bench keeps its items");
        helper.assertTrue(player.getInventory().isEmpty(), "and the player got nothing");
        helper.succeed();
    }

    private static ItemStack breakWith(GameTestHelper helper, BlockPos at, ItemStack tool) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(at);
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        Block.dropResources(state, level, pos, entity, player, tool);
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(state.getBlock().asItem())) {
                ItemStack found = e.getItem().copy();
                e.discard();
                return found;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack silkPick(GameTestHelper helper) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.enchant(helper.getLevel().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH), 1);
        return pick;
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void silkTouchKeepsWhatTheBenchHolds(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        try {
            Config.override(Config.Settings.builder().benchOnBreak(BreakMode.SILK_TOUCH).archiveOnBreak(BreakMode.DROP_ALL).build());
            ItemStack dropped = breakWith(helper, BENCH, silkPick(helper));
            helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 0 && dropped(helper, Items.COBBLESTONE) == 0, "nothing falls out of it");
            helper.assertTrue(dropped.has(ModComponents.CONTENTS.get()), "the dropped bench holds it");
            ProjectBenchBlockEntity placed = bench(helper, BENCH);
            placed.applyComponentsFromItemStack(dropped);
            helper.assertTrue(count(placed.matrix(), Items.OAK_PLANKS) == 2 && count(placed.storage(), Items.OAK_PLANKS) == 8
                    && count(placed.storage(), Items.COBBLESTONE) == 4 && !placed.blueprintSlot().getItem(0).isEmpty()
                    && placed.costSlot().getItem(0).is(Items.WHITE_DYE) && placed.costSlot().getItem(0).getCount() == 3,
                    "placed again, it has it all back, the cost slot's dye too");

            ProjectBenchBlockEntity second = bench(helper, BENCH);
            fill(second);
            ItemStack plain = breakWith(helper, BENCH, new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(!plain.has(ModComponents.CONTENTS.get()) && dropped(helper, Items.OAK_PLANKS) == 10
                    && dropped(helper, Items.WHITE_DYE) == 3, "without Silk Touch it all falls, the dye too");
        } finally {
            Config.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void alwaysKeepAndDropAll(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        try {
            Config.override(Config.Settings.builder().benchOnBreak(BreakMode.ALWAYS_KEEP).archiveOnBreak(BreakMode.DROP_ALL).build());
            ItemStack kept = breakWith(helper, BENCH, new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(kept.has(ModComponents.CONTENTS.get()) && dropped(helper, Items.OAK_PLANKS) == 0, "ALWAYS_KEEP keeps it with any pickaxe");

            ProjectBenchBlockEntity again = bench(helper, BENCH);
            fill(again);
            ItemStack bare = breakWith(helper, BENCH, ItemStack.EMPTY);
            helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 10 && !bare.has(ModComponents.CONTENTS.get()), "without the right tool it all falls");

            Config.override(Config.Settings.builder().benchOnBreak(BreakMode.DROP_ALL).archiveOnBreak(BreakMode.DROP_ALL).build());
            ProjectBenchBlockEntity last = bench(helper, BENCH);
            fill(last);
            ItemStack all = breakWith(helper, BENCH, silkPick(helper));
            helper.assertTrue(!all.has(ModComponents.CONTENTS.get()) && dropped(helper, Items.OAK_PLANKS) == 20, "DROP_ALL drops it even with Silk Touch");
        } finally {
            Config.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aBenchBrokenUnderItsWindowGivesNothingMore(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
        helper.destroyBlock(BENCH);
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "no sticks out of a broken bench: " + menu.getCarried());
        helper.assertFalse(menu.stillValid(player), "and the window is no longer valid");
        helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 10 && dropped(helper, Items.STICK) == 0, "what it held is on the ground, once");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void switchesStopWhatTheyCover(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        fill(bench);
        helper.setBlock(BENCH.east(), ModBlocks.BLUEPRINT_ARCHIVE.get());
        IItemHandler west = handler(helper, BENCH, Direction.WEST);
        IItemHandler down = handler(helper, BENCH, Direction.DOWN);
        try {
            Config.override(Config.Settings.builder().benchSides(0).build());
            helper.assertTrue(west.insertItem(1, new ItemStack(Items.DIRT), false).getCount() == 1, "every side closed: the west takes nothing");
            helper.assertTrue(down.extractItem(0, 64, false).isEmpty(), "and the bottom gives nothing");
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(BENCH), Direction.DOWN) == null,
                    "a closed side has no handler");

            Config.override(Config.Settings.builder().bench(false).benchOnBreak(BreakMode.DROP_ALL).archiveOnBreak(BreakMode.DROP_ALL).build());
            helper.assertTrue(west.insertItem(1, new ItemStack(Items.DIRT), false).getCount() == 1 && down.extractItem(0, 64, false).isEmpty(),
                    "a bench switched off takes and gives nothing");
            helper.assertTrue(handler(helper, BENCH.east(), Direction.UP).insertItem(0, new ItemStack(ModItems.BLUEPRINT.get()), false).getCount() == 1,
                    "nor does the archive, which needs the bench");

            Config.override(Config.Settings.builder().blueprint(false).archive(false).benchOnBreak(BreakMode.DROP_ALL).archiveOnBreak(BreakMode.DROP_ALL)
                    .build());
            helper.assertTrue(bench.archives().isEmpty(), "an archive switched off is not in the bench's panel");
            helper.assertFalse(bench.record(), "blueprints switched off: nothing is recorded");
            helper.assertTrue(bench.blueprint() == null, "nor shown");
        } finally {
            Config.clearOverride();
        }
        helper.assertTrue(bench.archives().size() == 1 && bench.record(), "back on, the archive is listed and the blueprint records");
        helper.succeed();
    }
}
