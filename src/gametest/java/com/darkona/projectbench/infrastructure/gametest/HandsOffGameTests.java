package com.darkona.projectbench.infrastructure.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.bench.HandsOff;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.content.bench.Opening;
import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.projectbench.infrastructure.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HandsOffGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);
    private static final int TOP = 1;
    private static final int UNDER = 4;
    private static final int EAST_TAB = 1;

    static {
        HandsOff.register((level, pos, state) -> state.is(Blocks.TRAPPED_CHEST));
    }

    private HandsOffGameTests() {
    }

    private record Setup(ProjectBenchBlockEntity bench, Container west, Container east, Player player, ProjectBenchMenu menu) {
        int slot(int i) {
            return menu.outsideIndex(i);
        }
    }

    private static Setup setup(GameTestHelper helper) {
        helper.setBlock(BENCH, Content.Blocks.PROJECT_BENCH.get());
        helper.setBlock(BENCH.west(), Blocks.BARREL);
        helper.setBlock(BENCH.east(), Blocks.TRAPPED_CHEST);
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        Container east = helper.getBlockEntity(BENCH.east());
        east.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = new Opening(bench, null, bench.archives(), bench.externalPanels()).menu(0, player.getInventory());
        player.containerMenu = menu;
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_TAB + EAST_TAB);
        return new Setup(bench, helper.getBlockEntity(BENCH.west()), east, player, menu);
    }

    private static void fillStorage(ProjectBenchBlockEntity bench) {
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aHandsOffTabRefusesEveryTaking(GameTestHelper helper) {
        Setup s = setup(helper);
        helper.assertTrue(s.menu().outsidePanels().get(EAST_TAB).handsOff() && !s.menu().outsidePanels().get(0).handsOff(),
                "the trapped chest's tab is hands-off, the barrel's is not");
        helper.assertTrue(s.menu().handsOff(s.menu().getSlot(s.slot(0))), "and so are its slots");
        Player p = s.player();
        s.menu().clicked(s.slot(0), 0, ClickType.PICKUP, p);
        s.menu().clicked(s.slot(0), 1, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().isEmpty() && count(s.east(), Items.IRON_INGOT) == 10, "a click takes nothing");
        s.menu().clicked(s.slot(0), 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(p.getInventory(), Items.IRON_INGOT) == 0
                && count(s.bench().storage(), Items.IRON_INGOT) == 0, "shift-click takes nothing");
        s.menu().clicked(s.slot(0), 0, ClickType.SWAP, p);
        s.menu().clicked(s.slot(0), 40, ClickType.SWAP, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(p.getInventory(), Items.IRON_INGOT) == 0, "number keys and F take nothing");
        s.menu().clicked(s.slot(0), 1, ClickType.THROW, p);
        s.menu().clicked(s.slot(0), 0, ClickType.THROW, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10, "Q takes nothing");
        p.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
        s.menu().clicked(s.slot(0), 0, ClickType.SWAP, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(p.getInventory(), Items.GOLD_INGOT) == 2, "a number key swaps nothing");
        p.getInventory().clearContent();
        s.menu().setCarried(new ItemStack(Items.GOLD_INGOT, 1));
        s.menu().clicked(s.slot(0), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().is(Items.GOLD_INGOT) && count(s.east(), Items.IRON_INGOT) == 10, "a click with another item swaps nothing");
        s.menu().setCarried(new ItemStack(Items.IRON_INGOT, 1));
        s.menu().clicked(s.slot(0), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 1 && count(s.east(), Items.IRON_INGOT) == 10, "a click with the same item neither adds nor takes");
        s.menu().clicked(ProjectBenchMenu.PLAYER_START, 0, ClickType.PICKUP_ALL, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 1 && count(s.east(), Items.IRON_INGOT) == 10, "a double click collects nothing from it");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aHandsOffTabRefusesPuttingByHand(GameTestHelper helper) {
        Setup s = setup(helper);
        Player p = s.player();
        s.menu().setCarried(new ItemStack(Items.IRON_INGOT, 6));
        s.menu().clicked(s.slot(1), 0, ClickType.PICKUP, p);
        s.menu().clicked(s.slot(1), 1, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 6 && count(s.east(), Items.IRON_INGOT) == 10, "a click puts nothing in");
        s.menu().clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, p);
        s.menu().clicked(s.slot(1), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        s.menu().clicked(s.slot(2), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        s.menu().clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 6 && count(s.east(), Items.IRON_INGOT) == 10, "a drag puts nothing in");
        s.menu().setCarried(ItemStack.EMPTY);
        p.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        s.menu().clicked(s.slot(1), 0, ClickType.SWAP, p);
        helper.assertTrue(count(p.getInventory(), Items.IRON_INGOT) == 5 && count(s.east(), Items.IRON_INGOT) == 10, "a number key puts nothing in");

        fillStorage(s.bench());
        s.bench().setShareMode(ShareMode.ALL);
        s.menu().clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(s.west(), Items.IRON_INGOT) == 5,
                "ALL: shift-click skips the hands-off tab and goes to the barrel");
        s.west().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        s.bench().setShareMode(ShareMode.OPEN);
        s.menu().clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10 && count(p.getInventory(), Items.IRON_INGOT) == 5,
                "OPEN on the hands-off tab: shift-click leaves it alone");
        s.bench().storage().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        s.bench().setShareMode(ShareMode.ALL);
        s.menu().clicked(ProjectBenchMenu.STORAGE_START, 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(count(s.east(), Items.IRON_INGOT) == 10, "shift-click from the internal inventory skips it too");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theBenchStillSharesAHandsOffTab(GameTestHelper helper) {
        Setup s = setup(helper);
        s.east().setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
        s.bench().setShareMode(ShareMode.ALL);
        s.bench().matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS));
        s.bench().matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS));
        s.bench().craft(s.player());
        helper.assertTrue(s.bench().matrix().getItem(TOP).getCount() == 1 && count(s.east(), Items.OAK_PLANKS) == 2,
                "a craft refills from it: " + count(s.east(), Items.OAK_PLANKS) + " planks left");
        s.bench().matrix().clearContent();
        fillStorage(s.bench());
        s.bench().matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        s.menu().clickMenuButton(s.player(), ProjectBenchMenu.BUTTON_CLEAR);
        helper.assertTrue(s.bench().matrix().isEmpty() && count(s.east(), Items.IRON_INGOT) == 14,
                "Clear's overflow goes where the iron already is, hands-off or not");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void protectVaultsOffMakesItAPlainTab(GameTestHelper helper) {
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().protectVaults(false).build());
            Setup s = setup(helper);
            helper.assertFalse(s.menu().outsidePanels().get(EAST_TAB).handsOff(), "off: no tab is hands-off");
            s.menu().clicked(s.slot(0), 0, ClickType.PICKUP, s.player());
            helper.assertTrue(s.menu().getCarried().getCount() == 10 && count(s.east(), Items.IRON_INGOT) == 0, "and a click takes");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        Setup s = setup(helper);
        helper.assertTrue(s.menu().handsOff(s.menu().getSlot(s.slot(0))), "on again: hands-off");
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().protectVaults(false).build());
            s.menu().clicked(s.slot(0), 0, ClickType.PICKUP, s.player());
            helper.assertTrue(s.menu().getCarried().getCount() == 10, "turned off with the window open, it is read at once");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.succeed();
    }
}
