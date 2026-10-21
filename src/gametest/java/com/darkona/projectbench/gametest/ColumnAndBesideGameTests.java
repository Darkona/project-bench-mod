package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlock;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.archive.BlueprintArchiveMenu;
import com.darkona.projectbench.block.archive.ColumnPart;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModComponents;
import com.darkona.projectbench.registry.ModItems;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColumnAndBesideGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final BlockPos BASE = new BlockPos(3, 1, 1);

    private ColumnAndBesideGameTests() {
    }

    private static BlueprintArchiveBlockEntity archive(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModBlocks.BLUEPRINT_ARCHIVE.get());
        return helper.getBlockEntity(at);
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModBlocks.PROJECT_BENCH.get());
        return helper.getBlockEntity(at);
    }

    static IItemHandler handler(GameTestHelper helper, BlockPos at, Direction side) {
        IItemHandler h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), side);
        helper.assertTrue(h != null, "an item handler at " + at + " on " + side);
        return h;
    }

    static ItemStack recorded(List<ItemStack> pattern, ItemStack result) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT.get());
        stack.set(ModComponents.BLUEPRINT.get(), new Blueprint(pattern, ResourceLocation.withDefaultNamespace("test"), result));
        return stack;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void stackedArchivesShareOneInventory(GameTestHelper helper) {
        BlueprintArchiveBlockEntity bottom = archive(helper, BASE);
        BlueprintArchiveBlockEntity middle = archive(helper, BASE.above());
        BlueprintArchiveBlockEntity top = archive(helper, BASE.above(2));
        helper.assertTrue(helper.getBlockState(BASE).getValue(BlueprintArchiveBlock.PART) == ColumnPart.BOTTOM
                && helper.getBlockState(BASE.above()).getValue(BlueprintArchiveBlock.PART) == ColumnPart.MIDDLE
                && helper.getBlockState(BASE.above(2)).getValue(BlueprintArchiveBlock.PART) == ColumnPart.TOP, "the parts connect");
        helper.assertTrue(top.column().size() == 3 && top.column().get(0) == bottom && middle.shelf().getContainerSize() == 108, "one column of three");
        bottom.blueprints().setItem(5, new ItemStack(ModItems.BLUEPRINT.get(), 3));
        helper.assertTrue(top.shelf().getItem(5).getCount() == 3, "the top sees what the bottom holds");

        IItemHandler h = handler(helper, BASE.above(2), Direction.UP);
        helper.assertTrue(h.getSlots() == 108, "the handler has the column's 108 slots: " + h.getSlots());
        ItemStack rest = h.insertItem(40, new ItemStack(ModItems.BLUEPRINT.get()), false);
        helper.assertTrue(rest.isEmpty() && middle.blueprints().getItem(4).getCount() == 1, "slot 40 is the middle's fifth");
        helper.assertTrue(h.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "nothing but blueprints goes in, blank or written");

        helper.setBlock(BASE.above(), Blocks.AIR);
        helper.assertTrue(top.column().size() == 1 && bottom.column().size() == 1, "two columns of one");
        helper.assertTrue(helper.getBlockState(BASE).getValue(BlueprintArchiveBlock.PART) == ColumnPart.SINGLE, "and they let go");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aRunTallerThanAColumnIsCutAndRegroupsWhenOneGoes(GameTestHelper helper) {
        BlueprintArchiveBlockEntity first = archive(helper, BASE);
        archive(helper, BASE.above());
        archive(helper, BASE.above(2));
        BlueprintArchiveBlockEntity fourth = archive(helper, BASE.above(3));
        helper.assertTrue(first.column().size() == 3 && fourth.column().size() == 1, "three and one");
        helper.assertTrue(helper.getBlockState(BASE.above(2)).getValue(BlueprintArchiveBlock.PART) == ColumnPart.TOP
                && helper.getBlockState(BASE.above(3)).getValue(BlueprintArchiveBlock.PART) == ColumnPart.SINGLE, "the fourth is its own column");
        fourth.blueprints().setItem(0, new ItemStack(ModItems.BLUEPRINT.get(), 2));
        helper.setBlock(BASE, Blocks.AIR);
        helper.assertTrue(fourth.column().size() == 3 && fourth.column().get(0).getBlockPos().equals(helper.absolutePos(BASE.above())),
                "the three left floating are one column: " + fourth.column().size());
        helper.assertTrue(helper.getBlockState(BASE.above()).getValue(BlueprintArchiveBlock.PART) == ColumnPart.BOTTOM
                && helper.getBlockState(BASE.above(2)).getValue(BlueprintArchiveBlock.PART) == ColumnPart.MIDDLE
                && helper.getBlockState(BASE.above(3)).getValue(BlueprintArchiveBlock.PART) == ColumnPart.TOP, "and connect");
        helper.assertTrue(fourth.shelf().getItem(2 * BlueprintArchiveBlockEntity.SLOTS).getCount() == 2, "each block kept what it held");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void archiveSlotHoldsOneRecordedBlueprintOr64Blank(GameTestHelper helper) {
        BlueprintArchiveBlockEntity archive = archive(helper, BASE);
        IItemHandler h = handler(helper, BASE, Direction.UP);
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            grid.add(i == 0 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        ItemStack written = recorded(grid, new ItemStack(Items.STICK));
        helper.assertTrue(h.insertItem(0, written.copy(), false).isEmpty(), "the first recorded blueprint goes in");
        helper.assertTrue(h.insertItem(0, written.copy(), false).getCount() == 1 && archive.blueprints().getItem(0).getCount() == 1,
                "a second one does not stack onto it");
        helper.assertTrue(h.insertItem(1, new ItemStack(ModItems.BLUEPRINT.get(), 64), false).isEmpty(), "64 blank ones fit in a slot");
        helper.assertTrue(h.insertItem(1, new ItemStack(ModItems.BLUEPRINT.get()), false).getCount() == 1, "and not a 65th");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void benchListsEveryColumnItTouches(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH.above());
        archive(helper, BENCH.above().east());
        archive(helper, BENCH.above().east().above());
        archive(helper, BENCH.above().east().below());
        archive(helper, BENCH.above().west());
        helper.assertTrue(bench.archives().size() == 4, "three of the east column and one west: " + bench.archives().size());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
        helper.assertTrue(menu.archiveSlots() == 4 * BlueprintArchiveBlockEntity.SLOTS, "the panel lists 144");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aColumnIsInReachThroughAnyOfItsBlocks(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        BlueprintArchiveBlockEntity bottom = archive(helper, BENCH.east());
        archive(helper, BENCH.east().above());
        BlueprintArchiveBlockEntity top = archive(helper, BENCH.east().above(2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 nearTheTopOnly = Vec3.atCenterOf(helper.absolutePos(BENCH.east().above(2))).add(0, 7.5, 0);
        Vec3 farAway = nearTheTopOnly.add(0, 20, 0);
        player.setPos(nearTheTopOnly);
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            grid.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        ItemStack sticks = recorded(grid, new ItemStack(Items.STICK, 4));
        bench.blueprintSlot().setItem(0, sticks.copy());
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
        helper.assertTrue(menu.archiveSlots() == 3 * BlueprintArchiveBlockEntity.SLOTS, "the panel lists the column of three");

        menu.clicked(ProjectBenchMenu.BLUEPRINT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.blueprintSlot().isEmpty() && ItemStack.isSameItemSameComponents(bottom.blueprints().getItem(0), sticks),
                "only the top block in reach, the blueprint goes to the bottom block's first slot: " + bottom.blueprints().getItem(0));
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(0)) && bench.blueprintSlot().getItem(0).is(ModItems.BLUEPRINT.get())
                && bottom.blueprints().getItem(0).isEmpty(), "and a click on the panel takes it back from the bottom block");

        player.setPos(farAway);
        menu.clicked(ProjectBenchMenu.BLUEPRINT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bottom.shelf().isEmpty() && player.getInventory().contains(sticks),
                "no block in reach: the column takes nothing and the blueprint goes to the player");
        helper.assertFalse(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(0)), "nor does the panel answer");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, sticks.copy());

        BlueprintArchiveMenu window = new BlueprintArchiveMenu(0, player.getInventory(), top.shelf(), top.column().size());
        helper.assertFalse(window.stillValid(player), "the archive's window: closed with no block in reach");
        player.setPos(nearTheTopOnly);
        helper.assertTrue(window.stillValid(player), "open with only the top block in reach");
        window.quickMoveStack(player, window.rows() * BlueprintArchiveMenu.COLUMNS + BlueprintArchiveMenu.COLUMNS * 3);
        helper.assertTrue(ItemStack.isSameItemSameComponents(bottom.blueprints().getItem(0), sticks), "and its shift-click fills the bottom block first");
        helper.destroyBlock(BENCH.east().above());
        helper.assertFalse(window.stillValid(player), "a block of the column broken, the window closes");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void craftRefillsFromTheChestBesideWhenSharing(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.assertTrue(bench.externalPanels().stream().mapToInt(p -> p.slots()).sum() == 27, "a chest's 27 slots");
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), bench.externalPanels());
        helper.assertTrue(menu.outsideSlots() == 27 && menu.outsideItem(0).is(Items.OAK_PLANKS), "the window shows the chest");
        bench.setShareMode(ShareMode.NONE);
        bench.craft(player);
        helper.assertTrue(bench.matrix().isEmpty() && count(chest, Items.OAK_PLANKS) == 4, "not sharing: the matrix pays and the chest is left alone");
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.setShareMode(ShareMode.ALL);
        bench.craft(player);
        helper.assertTrue(bench.matrix().getItem(1).getCount() == 1 && bench.matrix().getItem(4).getCount() == 1 && count(chest, Items.OAK_PLANKS) == 2,
                "sharing: the chest paid and the recipe stays laid out");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void clearGoesToTheInventoryThenTheChestThenThePlayer(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        bench.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        bench.setShareMode(ShareMode.ALL);
        bench.clearMatrix();
        helper.assertTrue(bench.matrix().isEmpty() && count(chest, Items.IRON_INGOT) == 5, "the full inventory sent it to the chest");
        bench.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        bench.setShareMode(ShareMode.NONE);
        bench.clearMatrix();
        helper.assertTrue(bench.matrix().getItem(0).getCount() == 5 && count(player.getInventory(), Items.IRON_INGOT) == 0,
                "no player (automatic benches): it stays in the matrix");
        bench.clearMatrix(player);
        helper.assertTrue(bench.matrix().isEmpty() && count(player.getInventory(), Items.IRON_INGOT) == 5
                        && player.getInventory().armor.stream().allMatch(ItemStack::isEmpty),
                "not sharing and full: the player's pockets take it, never the armor slots");
        for (int i = 0; i < player.getInventory().items.size(); ++i) {
            player.getInventory().items.set(i, new ItemStack(Items.DIRT, 64));
        }
        bench.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        bench.clearMatrix(player);
        helper.assertTrue(bench.matrix().getItem(0).getCount() == 5 && player.getInventory().armor.stream().allMatch(ItemStack::isEmpty),
                "everything full: it stays in the matrix");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void fillCraftAndClearNeitherMakeNorLosePlanks(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        List<ItemStack> sticks = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            sticks.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        bench.blueprintSlot().setItem(0, recorded(sticks, new ItemStack(Items.STICK, 4)));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 7));
        bench.storage().setItem(1, new ItemStack(Items.DIRT, 3));
        chest.setItem(3, new ItemStack(Items.OAK_PLANKS, 20));
        bench.matrix().setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        bench.setShareMode(ShareMode.ALL);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        bench.fillMatrix(player);
        int planks = count(bench.matrix(), Items.OAK_PLANKS) + count(bench.storage(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS);
        helper.assertTrue(planks == 27, "fill moves planks without making or losing any: " + planks);
        helper.assertTrue(bench.matrix().getItem(0).isEmpty() && count(bench.storage(), Items.COBBLESTONE) + count(chest, Items.COBBLESTONE) == 2,
                "what the blueprint does not want went back");
        bench.craft(player);
        planks = count(bench.matrix(), Items.OAK_PLANKS) + count(bench.storage(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS);
        helper.assertTrue(planks == 25, "a craft takes two planks: " + planks);
        bench.clearMatrix(player);
        helper.assertTrue(bench.matrix().isEmpty(), "cancelled: the matrix is empty");
        planks = count(bench.storage(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS) + count(player.getInventory(), Items.OAK_PLANKS);
        helper.assertTrue(planks == 25 && count(bench.storage(), Items.DIRT) == 3, "and everything is back: " + planks);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void fillLaysTheBlueprintOutFromTheInventoryAndTheChest(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        List<ItemStack> sticks = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            sticks.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        bench.blueprintSlot().setItem(0, recorded(sticks, new ItemStack(Items.STICK, 4)));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        chest.setItem(3, new ItemStack(Items.OAK_PLANKS, 20));
        bench.setShareMode(ShareMode.ALL);
        bench.fillMatrix();
        helper.assertTrue(bench.matrix().getItem(1).getCount() == 15 && bench.matrix().getItem(4).getCount() == 15,
                "30 planks over two positions: " + bench.matrix().getItem(1) + " " + bench.matrix().getItem(4));
        helper.assertTrue(count(bench.storage(), Items.OAK_PLANKS) + count(chest, Items.OAK_PLANKS) == 0, "all of them moved");
        helper.assertTrue(bench.result().getItem(0).is(Items.STICK), "and the matrix makes the recipe");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void fillTakesFromTheInternalInventoryFirst(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        bench.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        helper.assertTrue(bench.record(), "recorded");
        bench.clearMatrix();
        helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.OAK_PLANKS) == 10, "cleared into the inventory: " + count(bench.storage(), Items.OAK_PLANKS));
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 40));

        helper.assertTrue(bench.shareMode() == ShareMode.ALL, "sharing all by default");
        bench.fillMatrix();
        helper.assertTrue(bench.matrix().getItem(1).getCount() == 25 && bench.matrix().getItem(4).getCount() == 25, "25 each: " + bench.matrix().getItem(1) + " " + bench.matrix().getItem(4));
        helper.assertTrue(count(bench.storage(), Items.OAK_PLANKS) == 0 && count(chest, Items.OAK_PLANKS) == 0, "all of it: " + count(bench.storage(), Items.OAK_PLANKS) + " " + count(chest, Items.OAK_PLANKS));

        bench.setShareMode(ShareMode.NONE);
        bench.clearMatrix();
        helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.OAK_PLANKS) == 50, "cleared into the inventory: " + count(bench.storage(), Items.OAK_PLANKS));
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 40));
        bench.fillMatrix();
        helper.assertTrue(bench.matrix().getItem(1).getCount() == 25 && count(chest, Items.OAK_PLANKS) == 40 && count(bench.storage(), Items.OAK_PLANKS) == 0,
                "off: the inventory's 50, the chest untouched: " + bench.matrix().getItem(1) + " " + count(chest, Items.OAK_PLANKS));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aLoadedBenchShowsTheResultToTheClientsThatArrive(GameTestHelper helper) {
        ProjectBenchBlockEntity original = bench(helper, BENCH);
        original.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        original.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        original.toggleShowcase();
        var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        ProjectBenchBlockEntity loaded = bench(helper, BENCH.east());
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        var tag = loaded.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(tag.getBoolean("Show") && tag.contains("Made") && tag.contains("Items"),
                "the loaded bench tells a joining client what to draw: " + tag);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theSlotsOfTheOpenTabTakeAndGiveItems(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), bench.externalPanels());
        helper.assertTrue(menu.getSlot(menu.outsideIndex(0)).isActive() && menu.getSlot(menu.outsideIndex(26)).isActive()
                && !menu.getSlot(menu.outsideIndex(27)).isActive(), "a chest's 27 places are active, the rest not");
        menu.clicked(menu.outsideIndex(0), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 5 && chest.getItem(0).isEmpty(), "taken out of the chest");
        menu.clicked(menu.outsideIndex(7), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && chest.getItem(7).is(Items.DIAMOND) && chest.getItem(7).getCount() == 5, "put into another slot of the chest");
        menu.clicked(menu.outsideIndex(7), 1, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 3 && chest.getItem(7).getCount() == 2, "a right click takes half");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickOutOfTheOpenTabFollowsTheShareButton(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        chest.setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), bench.externalPanels());
        bench.setShareMode(ShareMode.NONE);
        menu.clicked(menu.outsideIndex(0), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(player.getInventory(), Items.DIAMOND) == 5 && count(bench.storage(), Items.DIAMOND) == 0 && chest.getItem(0).isEmpty(),
                "sharing off: into the player's inventory");
        bench.setShareMode(ShareMode.OPEN);
        menu.clicked(menu.outsideIndex(1), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(bench.storage(), Items.GOLD_INGOT) == 4 && count(player.getInventory(), Items.GOLD_INGOT) == 0 && chest.getItem(1).isEmpty(),
                "sharing on: into the bench's own inventory");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void everyInventoryBesideAndBelowIsATabAndAboveDoesNotCount(GameTestHelper helper) {
        BlockPos at = BENCH.above();
        ProjectBenchBlockEntity bench = bench(helper, at);
        helper.setBlock(at.above(), Blocks.BARREL);
        helper.assertTrue(bench.externalPanels().isEmpty(), "a barrel above is not beside the bench");
        helper.setBlock(at.below(), Blocks.HOPPER);
        helper.setBlock(at.north(), Blocks.CHEST);
        helper.setBlock(at.east(), Blocks.SHULKER_BOX);
        BlockEntity shulker = helper.getBlockEntity(at.east());
        CompoundTag named = shulker.saveWithFullMetadata(helper.getLevel().registryAccess());
        named.putString("CustomName", Component.Serializer.toJson(Component.literal("My tools"), helper.getLevel().registryAccess()));
        shulker.loadWithComponents(named, helper.getLevel().registryAccess());
        helper.setBlock(at.west(), Blocks.BARREL);
        var panels = bench.externalPanels();
        helper.assertTrue(panels.size() == 4, "hopper below, chest north, shulker east, barrel west: " + panels.size());
        helper.assertTrue(panels.get(0).sideIndex() == Direction.DOWN.get3DDataValue() && panels.get(0).icon().is(Items.HOPPER), "below comes first: " + panels.get(0));
        helper.assertTrue(panels.get(1).sideIndex() == Direction.NORTH.get3DDataValue() && panels.get(1).slots() == 27
                && panels.get(1).title().getString().equals("Chest") && panels.get(1).icon().is(Items.CHEST), "north: " + panels.get(1));
        helper.assertTrue(panels.stream().anyMatch(p -> p.title().getString().equals("My tools") && p.icon().is(Items.SHULKER_BOX)),
                "a named shulker box shows its name and its item (the client tints it by that item)");
        helper.assertTrue(panels.stream().anyMatch(p -> p.title().getString().equals("Barrel") && p.icon().is(Items.BARREL)), "a barrel too");
        helper.setBlock(at.east(), Blocks.RED_SHULKER_BOX);
        helper.assertTrue(bench.externalPanels().stream().anyMatch(p -> p.icon().is(Items.RED_SHULKER_BOX)), "a red shulker box's item is its own");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void showcaseSendsOnlyWhatItDraws(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper, BENCH);
        bench.storage().setItem(0, new ItemStack(Items.DIAMOND, 3));
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS, 7));
        helper.assertTrue(!bench.getUpdateTag(helper.getLevel().registryAccess()).contains("Show"), "off: nothing to draw");
        bench.toggleShowcase();
        var tag = bench.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(tag.getBoolean("Show") && tag.contains("Items") && !tag.contains("Storage") && !tag.contains("Matrix"),
                "on: the items to draw, none of the contents: " + tag);
        helper.succeed();
    }
}
