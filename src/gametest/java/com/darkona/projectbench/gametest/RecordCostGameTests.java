package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.blueprint.Blueprint;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.network.RecordRecipe;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecordCostGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final ResourceLocation TORCH = ResourceLocation.withDefaultNamespace("torch");

    private RecordCostGameTests() {
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper) {
        helper.setBlock(BENCH, ModBlocks.PROJECT_BENCH.get());
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
        return bench;
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.above())));
        return player;
    }

    private static ProjectBenchMenu open(ProjectBenchBlockEntity bench, Player player) {
        ProjectBenchMenu menu = new ProjectBenchMenu(0, player.getInventory(), bench, bench.archives(), List.of());
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void recordSpendsTheCost(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = player(helper);
        ProjectBenchMenu menu = open(bench, player);
        Slot cost = menu.getSlot(menu.costIndex(0));
        helper.assertTrue(cost.isActive() && cost.container == bench.costSlot(), "the cost slot is there, the last of the window");
        helper.assertTrue(menu.costIndex(0) == menu.slots.size() - 1 - ProjectBenchMenu.PROCESSOR_SLOTS, "after every other slot but a processor tab's: " + menu.costIndex(0));
        helper.assertTrue(!menu.canRecord() && menu.missingCost() == 1, "no dye: 'record' is off, one dye missing");
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD);
        helper.assertTrue(bench.blueprint() == null && !bench.record(), "and nothing is recorded");

        helper.assertFalse(cost.mayPlace(new ItemStack(Items.RED_DYE)) || bench.costSlot().canPlaceItem(0, new ItemStack(Items.OAK_PLANKS)),
                "the slot takes no other item");
        player.getInventory().setItem(0, new ItemStack(Items.RED_DYE, 2));
        player.getInventory().setItem(1, new ItemStack(Items.WHITE_DYE, 5));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.costSlot().isEmpty() && count(bench.storage(), Items.RED_DYE) == 2, "shift-click: red dye goes to the inventory");
        menu.clicked(ProjectBenchMenu.PLAYER_END - 8, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.costSlot().getItem(0).is(Items.WHITE_DYE) && bench.costSlot().getItem(0).getCount() == 5
                && count(bench.storage(), Items.WHITE_DYE) == 0, "and white dye into the cost slot: " + bench.costSlot().getItem(0));

        helper.assertTrue(menu.canRecord() && menu.missingCost() == 0, "with the dye, 'record' is on");
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD) && bench.blueprint() != null, "it records");
        helper.assertTrue(bench.costSlot().getItem(0).getCount() == 4, "and spends one dye: " + bench.costSlot().getItem(0));
        helper.assertTrue(count(bench.matrix(), Items.OAK_PLANKS) == 2, "and nothing from the matrix");

        menu.clicked(menu.costIndex(0), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.costSlot().isEmpty() && player.getInventory().countItem(Items.WHITE_DYE) == 4,
                "shift-click on the cost slot gives the dye back to the player");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theViewersPayToo(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = player(helper);
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.blankBlueprint() && !menu.canRecordRecipe(), "a blank blueprint but no dye: the viewers may not record");
        RecordRecipe.receive(player, new RecordRecipe(TORCH, false));
        helper.assertTrue(bench.blueprint() == null, "and the server records nothing either");

        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        helper.assertTrue(menu.canRecordRecipe(), "with the dye they may");
        RecordRecipe.receive(player, new RecordRecipe(TORCH, false));
        Blueprint bp = bench.blueprint();
        helper.assertTrue(bp != null && bp.recipe().equals(TORCH) && bench.costSlot().isEmpty(), "the torch recorded, the dye spent: " + bp);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void theCostFollowsTheConfig(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = player(helper);
        ProjectBenchMenu menu = open(bench, player);
        Slot cost = menu.getSlot(menu.costIndex(0));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE, 2));
        try {
            Config.override(Config.Settings.builder().recordCost(false).build());
            helper.assertFalse(cost.isActive() || cost.mayPlace(new ItemStack(Items.WHITE_DYE)), "off: the slot is hidden and takes nothing");
            helper.assertTrue(menu.canRecord() && menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD) && bench.blueprint() != null,
                    "and recording is free");
            helper.assertTrue(bench.costSlot().getItem(0).getCount() == 2, "the dye left there stays");

            bench.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
            Config.override(Config.Settings.builder().recordCost(true).recordCostItem(ResourceLocation.withDefaultNamespace("paper")).recordCostCount(3).build());
            helper.assertTrue(cost.isActive() && cost.mayPlace(new ItemStack(Items.PAPER)) && !cost.mayPlace(new ItemStack(Items.WHITE_DYE)),
                    "three paper: the slot takes paper now, not dye");
            helper.assertTrue(menu.missingCost() == 3 && !bench.record(), "the dye in it pays nothing");
            bench.costSlot().setItem(0, new ItemStack(Items.PAPER, 2));
            helper.assertTrue(menu.missingCost() == 1 && !menu.canRecord() && !bench.record(), "two paper are one short");
            bench.costSlot().setItem(0, new ItemStack(Items.PAPER, 4));
            helper.assertTrue(bench.record() && bench.costSlot().getItem(0).getCount() == 1, "four: it records and spends three");

            bench.blueprintSlot().setItem(0, new ItemStack(ModItems.BLUEPRINT.get()));
            Config.override(Config.Settings.builder().recordCost(true).recordCostItem(ResourceLocation.fromNamespaceAndPath(ProjectBench.MOD_ID, "no_such_item"))
                    .recordCostCount(1).build());
            helper.assertTrue(!cost.isActive() && bench.record() && bench.costSlot().getItem(0).getCount() == 1,
                    "an item that does not exist: free, and the slot is hidden");
        } finally {
            Config.clearOverride();
        }
        helper.succeed();
    }
}
