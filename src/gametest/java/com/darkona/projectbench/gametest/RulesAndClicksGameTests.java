package com.darkona.projectbench.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RulesAndClicksGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final int TOP = 1;
    private static final int UNDER = 4;

    private RulesAndClicksGameTests() {
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "tester"), ClientInformation.createDefault());
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper) {
        helper.setBlock(BENCH, Content.Blocks.PROJECT_BENCH.get());
        return helper.getBlockEntity(BENCH);
    }

    private static ProjectBenchMenu open(ProjectBenchBlockEntity bench, Player player) {
        return new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
    }

    private static void sticks(ProjectBenchBlockEntity bench, int planks) {
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, planks));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS, planks));
    }

    @GameTest(template = GameTests.EMPTY)
    public static void limitedCraftingHidesRecipesThePlayerHasNotUnlocked(GameTestHelper helper) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING);
        boolean before = rule.get();
        try {
            rule.set(true, helper.getLevel().getServer());
            ProjectBenchBlockEntity bench = bench(helper);
            ServerPlayer player = serverPlayer(helper);
            ProjectBenchMenu menu = open(bench, player);
            sticks(bench, 1);
            helper.assertTrue(bench.recipe() != null && !bench.result().getItem(0).isEmpty(), "the bench makes sticks");
            helper.assertTrue(!menu.craftable(0) && menu.getSlot(ProjectBenchMenu.RESULT).getItem().isEmpty()
                    && !menu.getSlot(ProjectBenchMenu.RESULT).mayPickup(player), "locked: no result shown, none to take");
            menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty() && bench.matrix().getItem(TOP).getCount() == 1, "and a click takes nothing and spends nothing");
            player.getRecipeBook().add(bench.recipe());
            helper.assertTrue(menu.craftable(0) && menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "unlocked: the result is there");
            menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(Items.STICK), "and can be taken");
            rule.set(false, helper.getLevel().getServer());
            helper.succeed();
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
    }

    @GameTest(template = GameTests.EMPTY)
    public static void withTheRuleOffEveryRecipeIsOpen(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        ServerPlayer player = serverPlayer(helper);
        ProjectBenchMenu menu = open(bench, player);
        sticks(bench, 1);
        helper.assertTrue(menu.craftable(0) && menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "no lock");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void dragSplittingOverTheMatrixKeepsEveryItem(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        menu.setCarried(new ItemStack(Items.OAK_PLANKS, 10));
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, player);
        menu.clicked(TOP, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, player);
        menu.clicked(UNDER, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 5 && bench.matrix().getItem(UNDER).getCount() == 5 && menu.getCarried().isEmpty(),
                "5 and 5, nothing left on the cursor: " + bench.matrix().getItem(TOP) + " " + bench.matrix().getItem(UNDER));
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "the recipe follows");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void swapAndThrowOnTheResultCraftOnce(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        sticks(bench, 3);
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.SWAP, player);
        helper.assertTrue(count(player.getInventory(), Items.STICK) == 4, "four sticks in the hotbar slot: " + count(player.getInventory(), Items.STICK));
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 2 && bench.matrix().getItem(UNDER).getCount() == 2, "one plank each spent");
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.THROW, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 1 && bench.matrix().getItem(UNDER).getCount() == 1, "the throw crafted once: "
                + bench.matrix().getItem(TOP));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void doubleClickNeverTakesTheResult(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        sticks(bench, 2);
        menu.setCarried(new ItemStack(Items.STICK));
        menu.clicked(ProjectBenchMenu.PLAYER_START, 0, ClickType.PICKUP_ALL, player);
        helper.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 1, "the result was not gathered: " + menu.getCarried());
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 2 && bench.matrix().getItem(UNDER).getCount() == 2, "nor spent");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickFromThePlayerNeverEntersTheMatrix(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 20));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.OAK_PLANKS) == 20, "all 20 in the internal inventory");
        helper.succeed();
    }

    private static ProjectBenchMenu openWithTabs(ProjectBenchBlockEntity bench, Player player) {
        return new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), bench.externalPanels());
    }

    private static ItemStack sticksBlueprint() {
        List<ItemStack> pattern = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        pattern.set(TOP, new ItemStack(Items.OAK_PLANKS));
        pattern.set(UNDER, new ItemStack(Items.OAK_PLANKS));
        return ColumnAndBesideGameTests.recorded(pattern, new ItemStack(Items.STICK, 4));
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickTopsUpTheMatrixSlotsThatHoldTheItem(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 60));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS, 10));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 64 && bench.matrix().getItem(UNDER).getCount() == 64,
                "both planks slots full: " + bench.matrix().getItem(TOP) + " " + bench.matrix().getItem(UNDER));
        helper.assertTrue(count(bench.storage(), Items.OAK_PLANKS) == 6 && player.getInventory().getItem(0).isEmpty(),
                "the 6 left over in the internal inventory: " + count(bench.storage(), Items.OAK_PLANKS));
        helper.assertTrue(count(bench.matrix(), Items.OAK_PLANKS) == 128, "no empty matrix slot took any");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void anItemTheMatrixNeitherHoldsNorWantsNeverEntersIt(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 1));
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 20));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(bench.matrix(), Items.STONE) == 0 && count(bench.storage(), Items.STONE) == 20, "no blueprint: the stone goes to the inventory");
        bench.blueprintSlot().setItem(0, sticksBlueprint());
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 20));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(bench.matrix(), Items.STONE) == 0 && count(bench.storage(), Items.STONE) == 40, "a blueprint of sticks: the stone stays out too");
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 9));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 5 && bench.matrix().getItem(UNDER).getCount() == 5,
                "the planks spread over the ghosts: " + bench.matrix().getItem(TOP) + " " + bench.matrix().getItem(UNDER));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void withShareOpenTheRestGoesToTheOpenTabAfterTheInventory(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = openWithTabs(bench, player);
        bench.setShareMode(ShareMode.OPEN);
        for (int i = 1; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 62));
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 60));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 64 && count(bench.storage(), Items.OAK_PLANKS) == 64 && count(chest, Items.OAK_PLANKS) == 4,
                "4 top the matrix up, 2 fill the internal inventory, 4 go to the open tab: " + bench.matrix().getItem(TOP) + ", chest " + count(chest, Items.OAK_PLANKS));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickFromASharedTabTopsUpTheMatrixToo(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(BENCH.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 30));
        chest.setItem(1, new ItemStack(Items.STONE, 8));
        chest.setItem(2, new ItemStack(Items.OAK_PLANKS, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = openWithTabs(bench, player);
        bench.setShareMode(ShareMode.ALL);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 50));
        menu.clicked(menu.outsideIndex(0), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 64 && count(bench.storage(), Items.OAK_PLANKS) == 16 && chest.getItem(0).isEmpty(),
                "sharing: 14 top the matrix up, 16 to the internal inventory: " + bench.matrix().getItem(TOP));
        menu.clicked(menu.outsideIndex(1), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(bench.matrix(), Items.STONE) == 0 && count(bench.storage(), Items.STONE) == 8, "the stone does not enter the matrix");
        bench.setShareMode(ShareMode.NONE);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 50));
        menu.clicked(menu.outsideIndex(2), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 50 && count(player.getInventory(), Items.OAK_PLANKS) == 5,
                "not sharing: straight to the player, as before");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theClicksOfTheInventoryModsFollowTheSameRule(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 40));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 20));
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 10));
        player.getInventory().setItem(2, new ItemStack(Items.OAK_PLANKS, 20));
        for (int i = 0; i < 3; ++i) {
            menu.clicked(ProjectBenchMenu.PLAYER_END - 9 + i, 0, ClickType.QUICK_MOVE, player);
        }
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 64 && count(bench.matrix(), Items.STONE) == 0
                        && count(bench.storage(), Items.OAK_PLANKS) == 16 && count(bench.storage(), Items.STONE) == 10,
                "the drag tops the planks up and leaves the stone out: " + bench.matrix().getItem(TOP) + ", " + count(bench.storage(), Items.OAK_PLANKS));
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 3));
        bench.storage().clearContent();
        menu.setCarried(new ItemStack(Items.OAK_PLANKS));
        menu.clicked(ProjectBenchMenu.PLAYER_START, 0, ClickType.PICKUP_ALL, player);
        helper.assertTrue(menu.getCarried().getCount() == 4 && bench.matrix().getItem(TOP).isEmpty(), "a double click gathers from the matrix: " + menu.getCarried());
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aResultOfADisabledItemIsNoResult(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        bench.matrix().setItem(1, new ItemStack(Items.STRING));
        bench.matrix().setItem(4, new ItemStack(Items.LEATHER));
        helper.assertTrue(bench.result().getItem(0).isEmpty() || bench.result().getItem(0).isItemEnabled(helper.getLevel().enabledFeatures()),
                "no result of a disabled item: " + bench.result().getItem(0));
        helper.succeed();
    }

    private static void autoMode(ProjectBenchBlockEntity bench, AutoMode mode) {
        while (bench.autoMode() != mode) {
            bench.cycleAutoMode();
        }
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theAutoButtonDecidesWhatTheStockGivesWhenAMatrixSlotRunsOut(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (AutoMode mode : AutoMode.values()) {
            ProjectBenchBlockEntity bench = bench(helper);
            autoMode(bench, mode);
            sticks(bench, 1);
            bench.storage().setItem(0, new ItemStack(Items.BIRCH_PLANKS, 8));
            bench.craft(player);
            switch (mode) {
                case OFF, ITEM -> helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.BIRCH_PLANKS) == 8,
                        mode + ": the matrix empties and the other wood stays in the inventory");
                case TAGS -> helper.assertTrue(bench.matrix().getItem(TOP).is(Items.BIRCH_PLANKS) && bench.matrix().getItem(UNDER).is(Items.BIRCH_PLANKS)
                        && count(bench.storage(), Items.BIRCH_PLANKS) == 6, "tags: birch planks take the place of the oak ones used up: "
                        + bench.matrix().getItem(TOP) + ", " + count(bench.storage(), Items.BIRCH_PLANKS) + " left");
            }
            bench.matrix().clearContent();
            bench.storage().clearContent();
        }
        ProjectBenchBlockEntity bench = bench(helper);
        autoMode(bench, AutoMode.TAGS);
        sticks(bench, 1);
        bench.storage().setItem(0, new ItemStack(Items.BIRCH_PLANKS, 8));
        bench.storage().setItem(1, new ItemStack(Items.OAK_PLANKS, 2));
        bench.craft(player);
        helper.assertTrue(bench.matrix().getItem(TOP).is(Items.OAK_PLANKS) && count(bench.storage(), Items.OAK_PLANKS) == 0 && count(bench.storage(), Items.BIRCH_PLANKS) == 8,
                "the same wood first, the other one only when there is none: " + bench.matrix().getItem(TOP));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void buttonsSwitchedOffDoNothing(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(0, new ItemStack(Items.IRON_INGOT, 9));
        bench.matrix().setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        try {
            Config.CONFIG.override(Config.Settings.builder().balanceButton(false).clearMatrix(false).autoFill(false).autoButton(false).build());
            menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_BALANCE);
            helper.assertTrue(bench.matrix().getItem(0).getCount() == 9, "balance off: the matrix stays as it was");
            menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_CLEAR);
            helper.assertTrue(bench.matrix().getItem(0).getCount() == 9 && bench.storage().isEmpty(), "clear off: nothing moves");
            AutoMode fixed = Config.get().autoDefault();
            menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_AUTO);
            helper.assertTrue(bench.autoMode() == fixed, "the A off: the bench stays on autoDefault, " + fixed);
        } finally {
            Config.CONFIG.clearOverride();
        }
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_BALANCE) && bench.matrix().getItem(0).getCount() == 5,
                "on again: balance works");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void ghostsOffStillTopsTheMatrixUp(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.blueprintSlot().setItem(0, sticksBlueprint());
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 2));
        try {
            Config.CONFIG.override(Config.Settings.builder().shiftClickFillsGhosts(false).build());
            player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
            menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
            helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 12 && bench.matrix().getItem(UNDER).isEmpty() && bench.storage().isEmpty(),
                    "the ghost under stays empty, the planks on top take all ten: " + bench.matrix().getItem(TOP) + " " + bench.matrix().getItem(UNDER));
        } finally {
            Config.CONFIG.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void blueprintSwitchedOffTurnsTheWholeMechanicOff(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Content.Blocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.archiveSlots() > 0, "on: the window lists the archive beside the bench");
        try {
            Config.CONFIG.override(Config.Settings.builder().blueprint(false).build());
            helper.assertFalse(Feature.ARCHIVE.enabled(), "the archive needs the blueprint");
            helper.assertTrue(bench.archives().isEmpty(), "and is not listed by the bench");
            Slot slot = menu.getSlot(ProjectBenchMenu.BLUEPRINT);
            helper.assertFalse(slot.mayPlace(new ItemStack(Content.Items.BLUEPRINT.get())) || slot.isActive(), "the blueprint slot takes nothing and is covered");
            player.getInventory().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get(), 3));
            menu.quickMoveStack(player, ProjectBenchMenu.PLAYER_END - 9);
            helper.assertTrue(count(bench.storage(), Content.Items.BLUEPRINT.get()) == 3 && bench.blueprintSlot().isEmpty()
                    && count(archive.blueprints(), Content.Items.BLUEPRINT.get()) == 0, "a shift-clicked blueprint goes to the internal inventory");
        } finally {
            Config.CONFIG.clearOverride();
        }
        helper.succeed();
    }
}
