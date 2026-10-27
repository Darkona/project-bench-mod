package com.darkona.projectbench.infrastructure.gametest;

import com.darkona.projectbench.index.BenchBlocks;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.bench.ExternalInventories;
import com.darkona.projectbench.content.bench.Processor;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.content.bench.Opening;
import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.ShareMode;
import dev.darkona.jdlib.testkit.TestPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

import static com.darkona.projectbench.infrastructure.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProcessorGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);

    private ProcessorGameTests() {
    }

    private static ProjectBenchMenu open(ProjectBenchBlockEntity bench, Player player) {
        ProjectBenchMenu menu = new Opening(bench, null, bench.archives(), bench.externalPanels()).menu(0, player.getInventory());
        player.containerMenu = menu;
        return menu;
    }

    private static ProjectBenchBlockEntity benchWith(GameTestHelper helper, BlockPos east, Block block) {
        helper.setBlock(BENCH, BenchBlocks.PROJECT_BENCH.get());
        helper.setBlock(east, block);
        return helper.getBlockEntity(BENCH);
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aFurnaceBesideGetsItsOwnWindow(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        furnace.setItem(Processor.INPUT, new ItemStack(Items.RAW_IRON, 3));
        furnace.setItem(Processor.FUEL, new ItemStack(Items.COAL, 2));
        furnace.setItem(Processor.RESULT, new ItemStack(Items.IRON_INGOT, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        ExternalInventories.Panel panel = menu.outsidePanels().getFirst();
        helper.assertTrue(menu.outsidePanels().size() == 1 && panel.processor() == Processor.FURNACE && panel.slots() == 3,
                "one tab, the furnace's own window: " + menu.outsidePanels());
        helper.assertTrue(menu.openProcessor() == Processor.FURNACE, "it is the open tab");
        helper.assertTrue(menu.getSlot(menu.processorIndex(Processor.FURNACE, Processor.INPUT)).getItem().is(Items.RAW_IRON)
                && menu.getSlot(menu.processorIndex(Processor.FURNACE, Processor.FUEL)).getItem().is(Items.COAL)
                && menu.getSlot(menu.processorIndex(Processor.FURNACE, Processor.RESULT)).getItem().is(Items.IRON_INGOT),
                "input, fuel and result are the furnace's own slots");
        helper.assertFalse(menu.getSlot(menu.outsideIndex(0)).isActive(), "no grid of slots on a processor's tab");
        helper.assertFalse(menu.getSlot(menu.processorIndex(Processor.BREWING_STAND, 0)).isActive(), "nor the brewing stand's slots");
        helper.assertFalse(menu.anyShared(), "nothing to share: no Share button");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theFurnaceTakesOnlyWhatItsRulesAllow(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        int input = menu.processorIndex(Processor.FURNACE, Processor.INPUT);
        int fuel = menu.processorIndex(Processor.FURNACE, Processor.FUEL);
        int result = menu.processorIndex(Processor.FURNACE, Processor.RESULT);
        menu.setCarried(new ItemStack(Items.DIRT, 4));
        menu.clicked(fuel, 0, ClickType.PICKUP, player);
        menu.clicked(result, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 4 && furnace.getItem(Processor.FUEL).isEmpty() && furnace.getItem(Processor.RESULT).isEmpty(),
                "dirt is no fuel, and nothing goes into the result");
        menu.clicked(input, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && furnace.getItem(Processor.INPUT).is(Items.DIRT), "the input takes anything, as in the furnace's own window");
        menu.setCarried(new ItemStack(Items.COAL, 5));
        menu.clicked(fuel, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && furnace.getItem(Processor.FUEL).getCount() == 5, "coal is fuel");
        menu.clicked(input, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIRT) && furnace.getItem(Processor.INPUT).isEmpty(), "and the player takes back what is there");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theResultGivesTheFurnacesExperience(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")).ifPresent(recipe -> {
            for (int i = 0; i < 10; ++i) {
                furnace.setRecipeUsed(recipe);
            }
        });
        furnace.setItem(Processor.RESULT, new ItemStack(Items.IRON_INGOT, 10));
        FakePlayer player = TestPlayers.survival(helper, "processor-test");
        Vec3 at = helper.absoluteVec(Vec3.atCenterOf(BENCH.above(3)));
        player.setPos(at);
        ProjectBenchMenu menu = open(bench, player);
        menu.clicked(menu.processorIndex(Processor.FURNACE, Processor.RESULT), 0, ClickType.PICKUP, player);
        int experience = 0;
        for (ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(at, at).inflate(2))) {
            experience += orb.getValue();
        }
        helper.assertTrue(menu.getCarried().getCount() == 10 && furnace.getItem(Processor.RESULT).isEmpty(), "the player takes the iron");
        helper.assertTrue(experience == 7, "and the furnace's stored experience pops at the player: " + experience);
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickRoutesLikeTheFurnace(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        int hotbar = ProjectBenchMenu.PLAYER_END - 9;
        player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 10));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        player.getInventory().setItem(0, new ItemStack(Items.COAL, 6));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 3));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(furnace.getItem(Processor.INPUT).getCount() == 10 && furnace.getItem(Processor.FUEL).getCount() == 6,
                "raw iron to the input, coal to the fuel: " + furnace.getItem(Processor.INPUT) + " " + furnace.getItem(Processor.FUEL));
        helper.assertTrue(count(bench.storage(), Items.DIRT) == 3, "what the furnace has no use for goes the bench's usual way");
        furnace.setItem(Processor.RESULT, new ItemStack(Items.IRON_INGOT, 4));
        menu.clicked(menu.processorIndex(Processor.FURNACE, Processor.RESULT), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(player.getInventory(), Items.IRON_INGOT) == 4 && furnace.getItem(Processor.RESULT).isEmpty(),
                "shift-click on the result: into the player's inventory");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aBrewingStandBesideGetsItsOwnWindow(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.BREWING_STAND);
        Container stand = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.openProcessor() == Processor.BREWING_STAND && menu.outsidePanels().getFirst().slots() == 5, "the brewing stand's window");
        int hotbar = ProjectBenchMenu.PLAYER_END - 9;
        player.getInventory().setItem(0, PotionContents.createItemStack(Items.POTION, Potions.WATER).copyWithCount(1));
        player.getInventory().setItem(1, PotionContents.createItemStack(Items.POTION, Potions.WATER).copyWithCount(1));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(hotbar + 1, 0, ClickType.QUICK_MOVE, player);
        player.getInventory().setItem(0, new ItemStack(Items.NETHER_WART, 4));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        player.getInventory().setItem(0, new ItemStack(Items.BLAZE_POWDER, 2));
        menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(stand.getItem(0).is(Items.POTION) && stand.getItem(1).is(Items.POTION) && stand.getItem(2).isEmpty(),
                "a bottle to each free bottle slot");
        helper.assertTrue(stand.getItem(Processor.INGREDIENT).getCount() == 4 && stand.getItem(Processor.BREWING_FUEL).getCount() == 2,
                "nether wart to the ingredient, blaze powder to the fuel");
        menu.setCarried(new ItemStack(Items.DIRT, 1));
        menu.clicked(menu.processorIndex(Processor.BREWING_STAND, 2), 0, ClickType.PICKUP, player);
        menu.clicked(menu.processorIndex(Processor.BREWING_STAND, Processor.INGREDIENT), 0, ClickType.PICKUP, player);
        menu.clicked(menu.processorIndex(Processor.BREWING_STAND, Processor.BREWING_FUEL), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIRT) && stand.getItem(2).isEmpty(), "dirt goes nowhere in a brewing stand");
        menu.setCarried(new ItemStack(Items.GLASS_BOTTLE, 3));
        menu.clicked(menu.processorIndex(Processor.BREWING_STAND, 2), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 2 && stand.getItem(2).getCount() == 1, "a bottle slot holds one");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void processorsAreNeverShared(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        helper.setBlock(BENCH.west(), Blocks.BREWING_STAND);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        Container stand = helper.getBlockEntity(BENCH.west());
        furnace.setItem(Processor.FUEL, new ItemStack(Items.COAL, 8));
        furnace.setItem(Processor.RESULT, new ItemStack(Items.IRON_INGOT, 8));
        stand.setItem(Processor.BREWING_FUEL, new ItemStack(Items.BLAZE_POWDER, 8));
        bench.setShareMode(ShareMode.ALL);
        helper.assertTrue(bench.stock().count(new ItemStack(Items.COAL)) == 0 && bench.stock().count(new ItemStack(Items.IRON_INGOT)) == 0
                && bench.stock().count(new ItemStack(Items.BLAZE_POWDER)) == 0, "the bench counts nothing in them");
        helper.assertTrue(bench.external().insert(new ItemStack(Items.COAL, 4)).getCount() == 4, "and puts nothing into them");
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            bench.storage().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        bench.matrix().setItem(0, new ItemStack(Items.COAL, 4));
        bench.matrix().setItem(1, new ItemStack(Items.BLAZE_POWDER, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_CLEAR);
        helper.assertTrue(furnace.getItem(Processor.FUEL).getCount() == 8 && stand.getItem(Processor.BREWING_FUEL).getCount() == 8,
                "Clear's overflow never goes into them");
        helper.succeed();
    }

    private static IItemHandler[] sided() {
        IItemHandler[] byFace = new IItemHandler[6];
        for (Direction face : Direction.values()) {
            byFace[face.get3DDataValue()] = new ItemStackHandler(face.get3DDataValue() + 1);
        }
        ((ItemStackHandler) byFace[Direction.WEST.get3DDataValue()]).setStackInSlot(0, new ItemStack(Items.REDSTONE, 9));
        return byFace;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aSidedBlockGetsNoTabButStorageKeepsIts(GameTestHelper helper) {
        helper.setBlock(BENCH, BenchBlocks.PROJECT_BENCH.get());
        helper.setBlock(BENCH.north(), Blocks.CHEST);
        helper.setBlock(BENCH.south(), Blocks.BARREL);
        FakeInventories.placeSided(helper, BENCH.east(), sided());
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        bench.setShareMode(ShareMode.ALL);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.outsidePanels().size() == 2 && menu.outsidePanels().stream().allMatch(p -> p.processor() == null && p.slots() == 27),
                "the chest and the barrel keep their tabs, the sided block gets none: " + menu.outsidePanels());
        helper.assertTrue(bench.stock().count(new ItemStack(Items.REDSTONE)) == 0, "nor is it shared");
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().processors(true).partialTabs(true).build());
            ProjectBenchMenu partial = open(bench, player);
            helper.assertTrue(partial.outsidePanels().size() == 3 && partial.outsidePanels().get(2).slots() == Direction.WEST.get3DDataValue() + 1,
                    "partialTabs: a tab with the slots of the face that touches the bench: " + partial.outsidePanels());
            helper.assertTrue(bench.stock().count(new ItemStack(Items.REDSTONE)) == 9, "and the bench shares it as before");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aSidedBlockInAlwaysShowGetsItsTab(GameTestHelper helper) {
        helper.setBlock(BENCH, BenchBlocks.PROJECT_BENCH.get());
        FakeInventories.placeSided(helper, BENCH.east(), sided());
        ProjectBenchBlockEntity bench = helper.getBlockEntity(BENCH);
        bench.setShareMode(ShareMode.ALL);
        helper.assertTrue(bench.externalPanels().isEmpty(), "by default the sided block gets no tab: " + bench.externalPanels());
        try {
            BenchConfig.overrideAlwaysShow(List.of("nosuchmod:controller", "not an id"));
            helper.assertTrue(bench.externalPanels().isEmpty() && bench.stock().count(new ItemStack(Items.REDSTONE)) == 0,
                    "unknown ids change nothing: " + bench.externalPanels());
            BenchConfig.overrideAlwaysShow(List.of("nosuchmod:controller", BuiltInRegistries.BLOCK.getKey(FakeInventories.SIDED_BLOCK).toString()));
            ExternalInventories.Panel panel = bench.externalPanels().isEmpty() ? null : bench.externalPanels().getFirst();
            helper.assertTrue(panel != null && panel.processor() == null && panel.slots() == Direction.WEST.get3DDataValue() + 1,
                    "listed: a tab with the slots of the face that touches the bench: " + bench.externalPanels());
            helper.assertTrue(bench.stock().count(new ItemStack(Items.REDSTONE)) == 9, "and the bench shares it like a chest");
        } finally {
            BenchConfig.clearAlwaysShow();
        }
        helper.assertTrue(bench.externalPanels().isEmpty(), "off the list again: no tab");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void processorsOffMakesAFurnaceASidedBlock(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = benchWith(helper, BENCH.east(), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(BENCH.east());
        furnace.setItem(Processor.FUEL, new ItemStack(Items.COAL, 8));
        bench.setShareMode(ShareMode.ALL);
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().processors(false).partialTabs(false).build());
            helper.assertTrue(bench.externalPanels().isEmpty() && bench.stock().count(new ItemStack(Items.COAL)) == 0,
                    "processors off: the furnace gets no tab and is not shared");
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().processors(false).partialTabs(true).build());
            ExternalInventories.Panel panel = bench.externalPanels().isEmpty() ? null : bench.externalPanels().getFirst();
            helper.assertTrue(panel != null && panel.processor() == null && panel.slots() == 1,
                    "with partialTabs too: the old tab, only the fuel the side shows: " + bench.externalPanels());
            helper.assertTrue(bench.stock().count(new ItemStack(Items.COAL)) == 8, "and the old sharing");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.assertTrue(bench.externalPanels().size() == 1 && bench.externalPanels().getFirst().processor() == Processor.FURNACE,
                "on again: the furnace's own window");
        helper.succeed();
    }
}
