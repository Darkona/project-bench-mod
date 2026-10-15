package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.ShareMode;
import com.darkona.projectbench.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OversizedSlotGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);
    private static final int TOP = 1;
    private static final int UNDER = 4;

    private OversizedSlotGameTests() {
    }

    private record Setup(ProjectBenchBlockEntity bench, Player player, ProjectBenchMenu menu, int slot) {
        int iron() {
            return count(player.getInventory(), Items.IRON_INGOT) + count(bench.storage(), Items.IRON_INGOT) + count(bench.matrix(), Items.IRON_INGOT)
                    + (menu.getCarried().is(Items.IRON_INGOT) ? menu.getCarried().getCount() : 0);
        }
    }

    private static Setup setup(GameTestHelper helper, IItemHandler fake) {
        helper.setBlock(BENCH, ModBlocks.PROJECT_BENCH.get());
        FakeInventories.place(helper, BENCH.east(), fake);
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), bench.externalPanels());
        player.containerMenu = menu;
        helper.assertTrue(menu.outsidePanels().size() == 1, "the fake's tab, not " + menu.outsidePanels().size());
        return new Setup(bench, player, menu, menu.outsideIndex(0));
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aCreativeDrawerGivesAStackPerClick(GameTestHelper helper) {
        Setup s = setup(helper, new FakeInventories.Vending(Items.IRON_INGOT));
        Player p = s.player();
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 64, "a left click: one stack, not " + s.menu().getCarried().getCount());
        s.menu().setCarried(ItemStack.EMPTY);
        s.menu().clicked(s.slot(), 1, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 32, "a right click: half a stack, not " + s.menu().getCarried().getCount());
        s.menu().setCarried(new ItemStack(Items.GOLD_INGOT, 3));
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().is(Items.GOLD_INGOT) && s.menu().getCarried().getCount() == 3, "a click with another item swaps nothing");
        s.menu().setCarried(new ItemStack(Items.IRON_INGOT, 60));
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 60, "a click with the same item does nothing, not " + s.menu().getCarried().getCount());
        s.menu().clicked(ProjectBenchMenu.PLAYER_START, 0, ClickType.PICKUP_ALL, p);
        helper.assertTrue(s.menu().getCarried().getCount() <= 64, "a double click gathers nothing past a stack");
        s.menu().clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, p);
        s.menu().clicked(s.slot(), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        s.menu().clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, p);
        helper.assertTrue(s.menu().getCarried().getCount() <= 64, "a drag takes nothing");
        s.menu().setCarried(ItemStack.EMPTY);

        s.menu().clicked(s.slot(), 0, ClickType.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(0).getCount() == 64, "a number key: one stack, not " + p.getInventory().getItem(0).getCount());
        p.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
        s.menu().clicked(s.slot(), 0, ClickType.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(0).is(Items.GOLD_INGOT), "a number key on a full hotbar slot swaps nothing");
        s.menu().clicked(s.slot(), Inventory.SLOT_OFFHAND, ClickType.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(Inventory.SLOT_OFFHAND).getCount() == 64, "F: one stack");
        p.getInventory().clearContent();

        s.bench().setShareMode(ShareMode.NONE);
        s.menu().clicked(s.slot(), 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(s.iron() == 64, "a shift-click: one stack, not " + s.iron());
        p.getInventory().clearContent();
        s.bench().setShareMode(ShareMode.ALL);
        s.menu().clicked(s.slot(), 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(s.iron() == 64, "a shift-click into the bench: one stack, not " + s.iron());
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aBigDrawerLosesWhatEachClickTakes(GameTestHelper helper) {
        FakeInventories.Drawer drawer = new FakeInventories.Drawer(Items.IRON_INGOT, 1000);
        Setup s = setup(helper, drawer);
        Player p = s.player();
        s.menu().setCarried(new ItemStack(Items.IRON_INGOT, 10));
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 10 && drawer.count() == 1000, "a click with the same item neither dupes nor loses: "
                + s.menu().getCarried().getCount() + " in hand, " + drawer.count() + " in the drawer");
        s.menu().setCarried(ItemStack.EMPTY);
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 64 && drawer.count() == 936, "left click: 64 out, " + drawer.count() + " left");
        s.menu().setCarried(ItemStack.EMPTY);
        s.menu().clicked(s.slot(), 1, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().getCount() == 32 && drawer.count() == 904, "right click: 32 out, " + drawer.count() + " left");
        s.menu().setCarried(ItemStack.EMPTY);
        s.menu().clicked(s.slot(), 2, ClickType.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(2).getCount() == 64 && drawer.count() == 840, "number key: 64 out, " + drawer.count() + " left");
        p.getInventory().clearContent();
        s.menu().clicked(s.slot(), 1, ClickType.THROW, p);
        helper.assertTrue(drawer.count() == 776, "Ctrl+Q: a stack thrown, " + drawer.count() + " left");
        s.menu().clicked(s.slot(), 0, ClickType.THROW, p);
        helper.assertTrue(drawer.count() == 775, "Q: one thrown, " + drawer.count() + " left");
        s.bench().setShareMode(ShareMode.NONE);
        s.menu().clicked(s.slot(), 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(s.iron() == 64 && drawer.count() == 711, "shift-click: once, one stack: " + s.iron() + " moved, " + drawer.count() + " left");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void anInventoryThatGivesMoreThanAskedIsRefused(GameTestHelper helper) {
        Setup s = setup(helper, new FakeInventories.Greedy(Items.IRON_INGOT, false));
        Player p = s.player();
        s.menu().clicked(s.slot(), 0, ClickType.PICKUP, p);
        helper.assertTrue(s.menu().getCarried().isEmpty(), "a click gets nothing, not " + s.menu().getCarried().getCount());
        s.menu().clicked(s.slot(), 0, ClickType.SWAP, p);
        s.menu().clicked(s.slot(), 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(s.iron() == 0, "nor a number key or a shift-click: " + s.iron());
        helper.assertTrue(s.bench().stock().take(new ItemStack(Items.IRON_INGOT), 1) == 0, "nor the bench's stock");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theStockOfACreativeDrawer(GameTestHelper helper) {
        Setup s = setup(helper, new FakeInventories.Vending(Items.OAK_PLANKS));
        s.bench().setShareMode(ShareMode.ALL);
        s.bench().storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        helper.assertTrue(s.bench().stock().count(planks) == Integer.MAX_VALUE, "the count stops at Integer.MAX_VALUE: " + s.bench().stock().count(planks));
        helper.assertTrue(s.bench().stock().take(planks, 200) == 200, "take gets what it asks for, a stack at a time");
        s.bench().storage().clearContent();
        s.bench().matrix().setItem(TOP, planks.copy());
        s.bench().matrix().setItem(UNDER, planks.copy());
        s.bench().craft(s.player());
        helper.assertTrue(s.bench().matrix().getItem(TOP).getCount() == 1 && s.bench().matrix().getItem(UNDER).getCount() == 1,
                "a craft refills one of each from the drawer");
        helper.succeed();
    }
}
