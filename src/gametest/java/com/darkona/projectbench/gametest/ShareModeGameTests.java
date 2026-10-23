package com.darkona.projectbench.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.Opening;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShareModeGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);
    private static final int TOP = 1;
    private static final int UNDER = 4;
    private static final int WEST_TAB = 0;
    private static final int EAST_TAB = 1;

    private ShareModeGameTests() {
    }

    private record Setup(ProjectBenchBlockEntity bench, Container west, Container east, Player player, ProjectBenchMenu menu) {
    }

    private static Setup setup(GameTestHelper helper) {
        helper.setBlock(BENCH, Content.Blocks.PROJECT_BENCH.get());
        helper.setBlock(BENCH.west(), Blocks.BARREL);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new Opening(bench, null, bench.archives(), bench.externalPanels()).menu(0, player.getInventory());
        player.containerMenu = menu;
        helper.assertTrue(menu.outsidePanels().size() == 2, "two tabs, not " + menu.outsidePanels().size());
        return new Setup(bench, helper.getBlockEntity(BENCH.west()), helper.getBlockEntity(BENCH.east()), player, menu);
    }

    private static void sticks(ProjectBenchBlockEntity bench) {
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS));
    }

    private static void fillStorage(ProjectBenchBlockEntity bench) {
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aCraftRefillsAsShareSays(GameTestHelper helper) {
        Setup s = setup(helper);
        s.west().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        s.east().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_TAB + EAST_TAB);

        s.bench().setShareMode(ShareMode.NONE);
        sticks(s.bench());
        s.bench().craft(s.player());
        helper.assertTrue(s.bench().matrix().isEmpty() && count(s.west(), Items.OAK_PLANKS) == 4 && count(s.east(), Items.OAK_PLANKS) == 4,
                "NONE: the matrix pays, both left alone");

        s.bench().setShareMode(ShareMode.OPEN);
        sticks(s.bench());
        s.bench().craft(s.player());
        helper.assertTrue(s.bench().matrix().getItem(TOP).getCount() == 1 && count(s.west(), Items.OAK_PLANKS) == 4 && count(s.east(), Items.OAK_PLANKS) == 2,
                "OPEN: the open tab (east) pays: west " + count(s.west(), Items.OAK_PLANKS) + ", east " + count(s.east(), Items.OAK_PLANKS));
        helper.assertTrue(s.bench().stock().count(new ItemStack(Items.OAK_PLANKS)) == 0,
                "OPEN with no window (an automatic bench) shares nothing");

        s.bench().setShareMode(ShareMode.ALL);
        s.bench().craft(s.player());
        helper.assertTrue(s.bench().matrix().getItem(TOP).getCount() == 1 && count(s.west(), Items.OAK_PLANKS) == 2 && count(s.east(), Items.OAK_PLANKS) == 2,
                "ALL: the first in the tabs' order (west) pays: west " + count(s.west(), Items.OAK_PLANKS));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void clearOverflowsAsShareSays(GameTestHelper helper) {
        Setup s = setup(helper);
        fillStorage(s.bench());
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_TAB + EAST_TAB);
        s.bench().setShareMode(ShareMode.OPEN);
        s.bench().matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_CLEAR);
        helper.assertTrue(s.bench().matrix().isEmpty() && count(s.east(), Items.IRON_INGOT) == 5 && count(s.west(), Items.IRON_INGOT) == 0,
                "OPEN: Clear's overflow goes to the open tab");
        s.bench().matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        s.bench().setShareMode(ShareMode.NONE);
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_CLEAR);
        helper.assertTrue(s.bench().matrix().isEmpty() && count(s.player().getInventory(), Items.IRON_INGOT) == 5,
                "NONE and full: the player's pockets take it");
        s.bench().matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        for (int i = 0; i < s.player().getInventory().items.size(); ++i) {
            s.player().getInventory().items.set(i, new ItemStack(Items.DIRT, 64));
        }
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_CLEAR);
        helper.assertTrue(s.bench().matrix().getItem(0).getCount() == 5, "everything full: it stays in the matrix");
        s.player().getInventory().clearContent();
        s.bench().matrix().setItem(0, ItemStack.EMPTY);
        s.east().setItem(3, new ItemStack(Items.GOLD_INGOT, 1));
        s.bench().matrix().setItem(0, new ItemStack(Items.GOLD_INGOT, 4));
        s.bench().setShareMode(ShareMode.ALL);
        s.bench().clearMatrix(s.player());
        helper.assertTrue(count(s.east(), Items.GOLD_INGOT) == 5 && count(s.west(), Items.GOLD_INGOT) == 0,
                "ALL: Clear's overflow goes where that item already is (east), not to the first tab");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickFromThePlayerGoesToTheInventoryThenAsShareSays(GameTestHelper helper) {
        Setup s = setup(helper);
        int hotbar = ProjectBenchMenu.PLAYER_END - 9;
        s.bench().setShareMode(ShareMode.ALL);
        s.player().getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 12));
        s.menu().clicked(hotbar, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.bench().storage(), Items.IRON_INGOT) == 12 && count(s.west(), Items.IRON_INGOT) + count(s.east(), Items.IRON_INGOT) == 0,
                "the internal inventory comes before the inventories beside");

        fillStorage(s.bench());
        s.bench().setShareMode(ShareMode.NONE);
        s.player().getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 7));
        s.menu().clicked(hotbar, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.west(), Items.IRON_INGOT) + count(s.east(), Items.IRON_INGOT) == 0 && count(s.player().getInventory(), Items.IRON_INGOT) == 7,
                "NONE and a full inventory: it stays with the player");

        s.player().getInventory().clearContent();
        s.bench().setShareMode(ShareMode.OPEN);
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_TAB + EAST_TAB);
        s.player().getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 7));
        s.menu().clicked(hotbar, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 7 && count(s.west(), Items.IRON_INGOT) == 0, "OPEN: the open tab (east)");

        s.bench().setShareMode(ShareMode.ALL);
        s.player().getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        s.menu().clicked(hotbar, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(s.west(), Items.IRON_INGOT) == 0,
                "ALL: the one that already has iron (east) before the first tab (west)");
        s.player().getInventory().setItem(0, new ItemStack(Items.EMERALD, 2));
        s.menu().clicked(hotbar, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.west(), Items.EMERALD) == 2 && count(s.east(), Items.EMERALD) == 0, "ALL: nobody has emeralds, the first tab with room (west)");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickFromTheInventoryGoesToTheMatrixThenAsShareSaysThenThePlayer(GameTestHelper helper) {
        Setup s = setup(helper);
        int slot = s.menu().storageStart(0);
        s.bench().matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 60));
        s.bench().storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        s.bench().setShareMode(ShareMode.NONE);
        s.menu().clicked(slot, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(s.bench().matrix().getItem(TOP).getCount() == 64 && count(s.player().getInventory(), Items.OAK_PLANKS) == 6
                        && count(s.west(), Items.OAK_PLANKS) + count(s.east(), Items.OAK_PLANKS) == 0,
                "NONE: 4 top the matrix up, 6 to the player: " + s.bench().matrix().getItem(TOP));

        s.bench().setShareMode(ShareMode.OPEN);
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_TAB + WEST_TAB);
        s.bench().storage().setItem(0, new ItemStack(Items.DIAMOND, 3));
        s.menu().clicked(slot, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.west(), Items.DIAMOND) == 3 && count(s.player().getInventory(), Items.DIAMOND) == 0, "OPEN: the open tab (west)");

        s.bench().setShareMode(ShareMode.ALL);
        s.east().setItem(5, new ItemStack(Items.EMERALD, 1));
        s.bench().storage().setItem(0, new ItemStack(Items.EMERALD, 5));
        s.menu().clicked(slot, 0, ClickType.QUICK_MOVE, s.player());
        helper.assertTrue(count(s.east(), Items.EMERALD) == 6 && count(s.west(), Items.EMERALD) == 0 && count(s.player().getInventory(), Items.EMERALD) == 0,
                "ALL: the one that already has emeralds (east)");
        helper.succeed();
    }
}
