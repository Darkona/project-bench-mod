package com.darkona.projectbench.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.config.AutoMode;
import dev.darkona.jdlib.api.inventory.GridFill;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.content.bench.Opening;
import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
import com.darkona.projectbench.content.blueprint.BlueprintContainer;
import com.darkona.projectbench.content.bench.RecordRecipe;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProjectBenchGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);

    private static final int TOP = 1;
    private static final int UNDER = 4;

    private ProjectBenchGameTests() {
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper) {
        helper.setBlock(BENCH, Content.Blocks.PROJECT_BENCH.get());
        return helper.getBlockEntity(BENCH);
    }

    private static ProjectBenchMenu open(ProjectBenchBlockEntity bench, Player player) {
        return new Opening(bench, null, bench.archives(), List.of()).menu(0, player.getInventory());
    }

    private static void sticksIn(ProjectBenchBlockEntity bench, int planks) {
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS));
        if (planks > 0) {
            bench.storage().setItem(5, new ItemStack(Items.OAK_PLANKS, planks));
        }
    }

    private static ItemStack recorded(List<ItemStack> pattern, ResourceLocation recipe, ItemStack result) {
        ItemStack stack = new ItemStack(Content.Items.BLUEPRINT.get());
        stack.set(Content.Components.BLUEPRINT.get(), new Blueprint(pattern, recipe, result));
        return stack;
    }

    private static List<ItemStack> grid(int at, Item item) {
        List<ItemStack> out = new ArrayList<>(9);
        for (int i = 0; i < 9; ++i) {
            out.add(i == at ? new ItemStack(item) : ItemStack.EMPTY);
        }
        return out;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void craftingRefillsFromTheInventoryAndKeepsTheRecipe(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        sticksIn(bench, 2);
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "the matrix makes sticks");
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 4, "four sticks taken: " + menu.getCarried());
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 1 && bench.matrix().getItem(UNDER).getCount() == 1,
                "both planks still in the matrix");
        helper.assertTrue(count(bench.storage(), Items.OAK_PLANKS) == 0, "the inventory paid for them: " + count(bench.storage(), Items.OAK_PLANKS));
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "the recipe is still laid out");
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 8, "eight sticks: " + menu.getCarried());
        helper.assertTrue(bench.matrix().isEmpty() && menu.getSlot(ProjectBenchMenu.RESULT).getItem().isEmpty(),
                "with nothing to refill from, the matrix empties and the recipe goes");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void shiftClickCraftsAllItCan(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        sticksIn(bench, 16);
        bench.storage().setItem(9, new ItemStack(Items.OAK_PLANKS, 16));
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.QUICK_MOVE, player);

        int sticks = player.getInventory().countItem(Items.STICK);
        helper.assertTrue(sticks == 68, "17 crafts: " + sticks + " sticks");
        helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.OAK_PLANKS) == 0, "every plank used");
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().isEmpty(), "and no result left");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void remaindersGoToTheInventory(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(0, new ItemStack(Items.HONEY_BOTTLE));
        bench.storage().setItem(0, new ItemStack(Items.HONEY_BOTTLE));
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.SUGAR), "honey makes sugar");
        menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
        helper.assertTrue(bench.matrix().getItem(0).is(Items.HONEY_BOTTLE), "the honey in the matrix stays");
        helper.assertTrue(count(bench.storage(), Items.HONEY_BOTTLE) == 0 && count(bench.storage(), Items.GLASS_BOTTLE) == 1,
                "the inventory's honey went and its bottle came back there");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void balanceButtonSpreadsTheMatrix(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.matrix().setItem(TOP, new ItemStack(Items.OAK_PLANKS, 9));
        bench.matrix().setItem(UNDER, new ItemStack(Items.OAK_PLANKS, 1));
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_BALANCE), "the window takes the button");
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 5 && bench.matrix().getItem(UNDER).getCount() == 5,
                "five and five: " + bench.matrix().getItem(TOP) + ", " + bench.matrix().getItem(UNDER));
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void autoAndShareButtonsReachTheBench(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        AutoMode before = bench.autoMode();
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_AUTO), "the window takes the A button");
        helper.assertTrue(bench.autoMode() != before, "the A cycles: " + before + " -> " + bench.autoMode());
        helper.assertTrue(menu.outsideScroll() == 0, "the A does not scroll the window beside the bench");
        for (ShareMode mode : new ShareMode[]{ShareMode.NONE, ShareMode.OPEN, ShareMode.ALL}) {
            helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_SHARE), "the window takes the Share button");
            helper.assertTrue(bench.shareMode() == mode && menu.outsideScroll() == 0, "Share cycles to " + mode + ", not " + bench.shareMode());
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void blueprintShowsGhostsAndShiftClickFillsThem(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        sticksIn(bench, 0);
        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE));
        helper.assertTrue(menu.canRecord(), "a blank blueprint, its dye and a recipe: 'record' is on");
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.BUTTON_RECORD), "it records");
        Blueprint bp = bench.blueprint();
        helper.assertTrue(bp != null && bp.recipe().equals(ResourceLocation.withDefaultNamespace("stick")) && bp.result().is(Items.STICK)
                && bp.ghost(TOP).is(Items.OAK_PLANKS) && bp.ghost(0).isEmpty(), "the blueprint holds the stick recipe: " + bp);
        helper.assertTrue(count(bench.matrix(), Items.OAK_PLANKS) == 2 && bench.costSlot().isEmpty(), "recording spends the dye, not the matrix");
        helper.assertFalse(menu.canRecord() || bench.record(), "a recorded blueprint is not written again");

        ItemStack blueprint = bench.blueprintSlot().removeItem(0, 1);
        bench.matrix().clearContent();
        bench.matrix().setItem(TOP, new ItemStack(Items.DIRT));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        bench.blueprintSlot().setItem(0, blueprint);
        helper.assertTrue(bench.matrix().getItem(TOP).is(Items.DIRT) && bench.matrix().getItem(UNDER).isEmpty(), "the blueprint moves nothing in the matrix");
        helper.assertTrue(count(bench.storage(), Items.OAK_PLANKS) == 5, "nor takes from the inventory");
        helper.assertTrue(bp != null && GridFill.missing(bench.matrix(), bp.pattern(), UNDER), "the window draws the missing ghost");

        menu.clicked(ProjectBenchMenu.STORAGE_START, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(UNDER).getCount() == 5 && bench.storage().getItem(0).isEmpty(),
                "shift-click from the inventory: all five planks on the empty ghost (the dirt's slot holds no plank): " + bench.matrix().getItem(UNDER));
        menu.clicked(ProjectBenchMenu.MATRIX_START + TOP, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).isEmpty() && count(bench.storage(), Items.DIRT) == 1, "the dirt shift-clicked out to the inventory");
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 2));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.matrix().getItem(TOP).getCount() == 3 && bench.matrix().getItem(UNDER).getCount() == 5
                && player.getInventory().getItem(0).isEmpty(), "shift-click from the player: onto the emptiest ghost");
        menu.clicked(ProjectBenchMenu.PLAYER_END - 8, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(count(bench.storage(), Items.COBBLESTONE) == 2 && count(bench.matrix(), Items.COBBLESTONE) == 0,
                "what no ghost takes goes to the inventory, as without a blueprint");
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.STICK), "and the recipe is laid out");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void recipeSelectorPicksAmongTwo(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        List<RecipeHolder<?>> before = new ArrayList<>(manager.getOrderedRecipes());
        ResourceLocation stick = Content.id("test_bush_stick");
        ResourceLocation string = Content.id("test_bush_string");
        List<RecipeHolder<?>> with = new ArrayList<>(before);
        with.add(new RecipeHolder<>(stick, bush(Items.STICK)));
        with.add(new RecipeHolder<>(string, bush(Items.STRING)));
        manager.replaceRecipes(with);
        try {
            ProjectBenchBlockEntity bench = bench(helper);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ProjectBenchMenu menu = open(bench, player);
            bench.matrix().setItem(4, new ItemStack(Items.DEAD_BUSH));
            List<RecipeHolder<CraftingRecipe>> listed = ProjectBenchBlockEntity.recipesFor(helper.getLevel(),
                    CraftingInput.of(3, 3, bench.matrix().getItems()));
            helper.assertTrue(listed.size() == 2 && listed.get(0).id().equals(stick) && listed.get(1).id().equals(string),
                    "two recipes, by id: " + listed);
            helper.assertTrue(menu.choice(0) == 0 && bench.result().getItem(0).is(Items.STICK), "the first one: " + bench.result().getItem(0));
            helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.recipeButton(0, 1)), "the selector picks the second");
            helper.assertTrue(menu.choice(0) == 1 && bench.result().getItem(0).is(Items.STRING), "string now: " + bench.result().getItem(0));
            menu.clickMenuButton(player, ProjectBenchMenu.recipeButton(0, 5));
            helper.assertTrue(menu.choice(0) == 1, "a recipe past the list changes nothing");
            bench.matrix().setItem(4, ItemStack.EMPTY);
            bench.matrix().setItem(4, new ItemStack(Items.DEAD_BUSH));
            helper.assertTrue(bench.result().getItem(0).is(Items.STRING), "the bench keeps the choice when the matrix makes it again");
            bench.blueprintSlot().setItem(0, recorded(grid(4, Items.DEAD_BUSH), stick, new ItemStack(Items.STICK)));
            helper.assertTrue(menu.choice(0) == 0 && bench.result().getItem(0).is(Items.STICK), "a blueprint of the stick sets the stick");
            menu.clicked(ProjectBenchMenu.RESULT, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(Items.STICK), "and crafts it: " + menu.getCarried());
        } finally {
            manager.replaceRecipes(before);
        }
        helper.succeed();
    }

    private static ShapelessRecipe bush(Item result) {
        return new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(result), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DEAD_BUSH)));
    }

    private static Blueprint recordedOn(ProjectBenchBlockEntity bench) {
        Blueprint bp = bench.blueprint();
        if (bp == null) {
            throw new GameTestAssertException("the blueprint was not recorded");
        }
        return bp;
    }

    private static final ResourceLocation TORCH = ResourceLocation.withDefaultNamespace("torch");
    private static final ResourceLocation STEW = ResourceLocation.withDefaultNamespace("mushroom_stew");

    @GameTest(template = GameTests.EMPTY)
    public static void viewersRecordOnABlankBlueprint(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.above())));
        ProjectBenchMenu menu = open(bench, player);
        player.containerMenu = menu;
        bench.matrix().setItem(0, new ItemStack(Items.DIRT));

        RecordRecipe.receive(player, new RecordRecipe(TORCH, true));
        helper.assertTrue(bench.blueprintSlot().getItem(0).isEmpty() && bench.matrix().getItem(0).is(Items.DIRT), "no blueprint: nothing happens");
        ItemStack old = recorded(grid(4, Items.DEAD_BUSH), ResourceLocation.withDefaultNamespace("stick"), new ItemStack(Items.STICK));
        bench.blueprintSlot().setItem(0, old.copy());
        RecordRecipe.receive(player, new RecordRecipe(TORCH, true));
        helper.assertTrue(ItemStack.matches(old, bench.blueprintSlot().getItem(0)) && bench.matrix().getItem(0).is(Items.DIRT),
                "a recorded blueprint is not written again");

        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.costSlot().setItem(0, new ItemStack(Items.WHITE_DYE, 3));
        helper.assertTrue(menu.canRecordRecipe(), "a blank blueprint and its dye: the viewers may record");
        RecordRecipe.receive(player, new RecordRecipe(ResourceLocation.withDefaultNamespace("no_such_recipe"), false));
        RecordRecipe.receive(player, new RecordRecipe(ResourceLocation.withDefaultNamespace("map_cloning"), false));
        helper.assertTrue(bench.blueprint() == null, "an unknown or special recipe is not recorded");

        RecordRecipe.receive(player, new RecordRecipe(TORCH, false));
        Blueprint bp = recordedOn(bench);
        helper.assertTrue(bp.recipe().equals(TORCH) && bp.result().is(Items.TORCH) && bp.result().getCount() == 4,
                "the torch recorded with its result: " + bp);
        helper.assertTrue(bp.ghost(0).is(Items.COAL) && bp.ghost(3).is(Items.STICK) && bp.ghost(1).isEmpty() && bp.ghost(4).isEmpty(),
                "shaped: coal over a stick from the top left: " + bp.pattern());
        helper.assertTrue(bench.matrix().getItem(0).is(Items.DIRT) && GridFill.missing(bench.matrix(), bp.pattern(), 3),
                "without fill the matrix is left to the viewer; the stick shows as a ghost");

        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.storage().setItem(0, new ItemStack(Items.STICK, 2));
        RecordRecipe.receive(player, new RecordRecipe(TORCH, true));
        bp = recordedOn(bench);
        helper.assertTrue(bp.recipe().equals(TORCH), "recorded again on the new blank blueprint");
        helper.assertTrue(bench.matrix().getItem(3).is(Items.STICK) && bench.matrix().getItem(3).getCount() == 1
                && count(bench.storage(), Items.STICK) == 1, "fill: the stick it has, one");
        helper.assertTrue(bench.matrix().getItem(0).isEmpty() && count(bench.storage(), Items.DIRT) == 1, "the dirt back to the inventory");
        helper.assertTrue(GridFill.missing(bench.matrix(), bp.pattern(), 0), "the coal it lacks stays a ghost");

        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.matrix().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.BOWL));
        player.getInventory().setItem(1, new ItemStack(Items.RED_MUSHROOM, 3));
        player.getInventory().setItem(2, new ItemStack(Items.BROWN_MUSHROOM, 3));
        RecordRecipe.receive(player, new RecordRecipe(STEW, true));
        bp = recordedOn(bench);
        CraftingRecipe stew = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(STEW).orElseThrow().value();
        helper.assertTrue(bp.recipe().equals(STEW) && bp.result().is(Items.MUSHROOM_STEW), "the stew recorded: " + bp);
        for (int i = 0; i < Blueprint.SIZE; ++i) {
            boolean expected = i < stew.getIngredients().size() ? stew.getIngredients().get(i).test(bp.ghost(i)) : bp.ghost(i).isEmpty();
            helper.assertTrue(expected, "shapeless: one slot per ingredient in order, slot " + i + ": " + bp.pattern());
        }
        helper.assertTrue(menu.getSlot(ProjectBenchMenu.RESULT).getItem().is(Items.MUSHROOM_STEW) && player.getInventory().getItem(0).isEmpty(),
                "fill from the player's inventory: the whole stew is laid out");
        helper.assertTrue(bench.costSlot().isEmpty(), "three recordings, three dyes; the ones refused spent none");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void archivePanelSwapsBlueprints(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Content.Blocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.above())));
        List<ItemStack> sticks = new ArrayList<>(grid(TOP, Items.OAK_PLANKS));
        sticks.set(UNDER, new ItemStack(Items.OAK_PLANKS));
        archive.blueprints().setItem(3, recorded(sticks, ResourceLocation.withDefaultNamespace("stick"), new ItemStack(Items.STICK, 4)));
        archive.blueprints().setItem(7, new ItemStack(Content.Items.BLUEPRINT.get(), 2));
        bench.blueprintSlot().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get()));
        bench.storage().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.archiveSlots() == BlueprintArchiveBlockEntity.SLOTS, "one archive beside it: " + menu.archiveSlots());
        helper.assertTrue(BlueprintItem.blueprint(menu.archiveItem(3)) != null, "the window sees the archive's blueprint");
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(3)), "a click on it");
        helper.assertTrue(bench.blueprint() != null && BlueprintItem.blueprint(archive.blueprints().getItem(3)) == null
                && archive.blueprints().getItem(3).is(Content.Items.BLUEPRINT.get()), "swaps it with the bench's blank one");
        helper.assertTrue(bench.matrix().isEmpty() && count(bench.storage(), Items.OAK_PLANKS) == 2, "and moves no plank into the matrix");
        helper.assertFalse(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(7)), "a stack of two with a blueprint in the bench: no swap");
        helper.assertFalse(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(BlueprintArchiveBlockEntity.SLOTS)), "past the panel: nothing");
        BlueprintContainer.BlueprintSlot slot = new BlueprintContainer.BlueprintSlot(archive.blueprints(), 0, 0, 0);
        helper.assertFalse(slot.mayPlace(new ItemStack(Items.DIRT)), "the archive takes only blueprints");
        helper.assertTrue(slot.mayPlace(new ItemStack(Content.Items.BLUEPRINT.get())), "and takes blueprints");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void blankBlueprintsAreNeverFiledFromTheBench(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Content.Blocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.above())));
        ProjectBenchMenu menu = open(bench, player);
        player.getInventory().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get(), 5));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(archive.blueprints().isEmpty() && bench.blueprintSlot().getItem(0).getCount() == 1
                        && player.getInventory().getItem(0).getCount() == 4,
                "blanks from the player: one to the blueprint slot, the rest stays, the archive is left empty: " + player.getInventory().getItem(0));
        menu.clicked(ProjectBenchMenu.BLUEPRINT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(archive.blueprints().isEmpty() && bench.blueprintSlot().isEmpty()
                        && player.getInventory().countItem(Content.Items.BLUEPRINT.get()) == 5,
                "a blank out of the blueprint slot goes to the player, not to the archive");
        ItemStack sticks = recorded(grid(TOP, Items.OAK_PLANKS), ResourceLocation.withDefaultNamespace("stick"), new ItemStack(Items.STICK, 4));
        bench.blueprintSlot().setItem(0, sticks.copy());
        menu.clicked(ProjectBenchMenu.BLUEPRINT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.blueprintSlot().isEmpty() && ItemStack.isSameItemSameComponents(archive.blueprints().getItem(0), sticks),
                "a recorded one still goes to the archive");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void blueprintsGoIntoTheArchive(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        helper.setBlock(BENCH.east(), Content.Blocks.BLUEPRINT_ARCHIVE.get());
        BlueprintArchiveBlockEntity archive = helper.getBlockEntity(BENCH.east());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.above())));
        ItemStack sticks = recorded(grid(TOP, Items.OAK_PLANKS), ResourceLocation.withDefaultNamespace("stick"), new ItemStack(Items.STICK, 4));
        ItemStack buttons = recorded(grid(TOP, Items.STONE), ResourceLocation.withDefaultNamespace("stone_button"), new ItemStack(Items.STONE_BUTTON));
        archive.blueprints().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get(), 2));
        bench.blueprintSlot().setItem(0, sticks.copy());
        ProjectBenchMenu menu = open(bench, player);
        helper.assertTrue(menu.archiveSlots() == 36, "thirty-six slots an archive: " + menu.archiveSlots());

        menu.clicked(ProjectBenchMenu.BLUEPRINT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.blueprintSlot().isEmpty() && ItemStack.isSameItemSameComponents(archive.blueprints().getItem(1), sticks),
                "the bench's blueprint went to the first empty archive slot: " + archive.blueprints().getItem(1));
        player.getInventory().setItem(0, new ItemStack(Content.Items.BLUEPRINT.get(), 3));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.blueprintSlot().getItem(0).getCount() == 1 && BlueprintItem.blueprint(bench.blueprintSlot().getItem(0)) == null
                        && archive.blueprints().getItem(0).getCount() == 2 && player.getInventory().getItem(0).getCount() == 2,
                "one blank blueprint goes to the bench's empty slot, ready to record, the rest stay with the player (never filed from here): "
                        + bench.blueprintSlot().getItem(0) + ", " + archive.blueprints().getItem(0));
        bench.blueprintSlot().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(0, sticks.copy());
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        int sameRecorded = 0;
        for (int i = 0; i < archive.blueprints().getContainerSize(); ++i) {
            if (ItemStack.isSameItemSameComponents(archive.blueprints().getItem(i), sticks)) {
                sameRecorded += archive.blueprints().getItem(i).getCount();
                helper.assertTrue(archive.blueprints().getItem(i).getCount() == 1, "a recorded blueprint is one to a slot");
            }
        }
        helper.assertTrue(bench.blueprintSlot().isEmpty() && sameRecorded == 2 && player.getInventory().getItem(0).isEmpty(),
                "a recorded one from the player goes to the archive, to a slot of its own: " + sameRecorded);

        menu.setCarried(buttons.copy());
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(5)), "a click on the panel with a blueprint on the cursor");
        helper.assertTrue(menu.getCarried().isEmpty() && ItemStack.isSameItemSameComponents(archive.blueprints().getItem(5), buttons),
                "puts it there: " + archive.blueprints().getItem(5));
        menu.setCarried(sticks.copy());
        helper.assertTrue(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(5)), "on another blueprint");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getCarried(), buttons) && ItemStack.isSameItemSameComponents(archive.blueprints().getItem(5), sticks),
                "the two change places");
        menu.setCarried(new ItemStack(Content.Items.BLUEPRINT.get()));
        helper.assertFalse(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(6)), "a blank one is not filed from the bench");
        helper.assertTrue(menu.getCarried().is(Content.Items.BLUEPRINT.get()) && archive.blueprints().getItem(6).isEmpty(), "and stays on the cursor");
        menu.setCarried(new ItemStack(Items.PAPER));
        helper.assertFalse(menu.clickMenuButton(player, ProjectBenchMenu.archiveButton(6)), "paper is no blueprint");
        menu.setCarried(ItemStack.EMPTY);

        for (int i = 0; i < BlueprintArchiveBlockEntity.SLOTS; ++i) {
            if (archive.blueprints().getItem(i).isEmpty()) {
                archive.blueprints().setItem(i, sticks.copy());
            }
        }
        player.getInventory().setItem(0, buttons.copy());
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(ItemStack.isSameItemSameComponents(bench.blueprintSlot().getItem(0), buttons), "a full archive: into the bench's slot");
        helper.succeed();
    }
}
