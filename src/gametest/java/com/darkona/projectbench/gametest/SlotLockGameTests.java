package com.darkona.projectbench.gametest;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.content.bench.Opening;
import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import com.darkona.projectbench.config.BreakMode;
import com.darkona.projectbench.config.BenchConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

import static com.darkona.projectbench.gametest.GameTests.count;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SlotLockGameTests {
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final int S = ProjectBenchMenu.STORAGE_START;
    private static final int MIDDLE = 2;

    private SlotLockGameTests() {
    }

    private static ProjectBenchBlockEntity bench(GameTestHelper helper) {
        helper.setBlock(BENCH, Content.Blocks.PROJECT_BENCH.get());
        return helper.getBlockEntity(BENCH);
    }

    private static ProjectBenchMenu open(ProjectBenchBlockEntity bench, Player player) {
        return new Opening(bench, null, bench.archives(), List.of()).menu(0, player.getInventory());
    }

    private static boolean lockedTo(ProjectBenchBlockEntity bench, int slot, ItemStack like) {
        return ItemStack.isSameItemSameComponents(bench.storage().lock(slot), like);
    }

    private static ProjectBenchMenu lockedEmpty(GameTestHelper helper, ProjectBenchBlockEntity bench, Player player) {
        ProjectBenchMenu menu = open(bench, player);
        bench.storage().setItem(0, new ItemStack(Items.COBBLESTONE));
        menu.clicked(S, MIDDLE, ClickType.CLONE, player);
        bench.storage().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(lockedTo(bench, 0, new ItemStack(Items.COBBLESTONE)), "slot 0 locked to cobblestone");
        return menu;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aMiddleClickLocksAndFrees(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.storage().setItem(3, new ItemStack(Items.COBBLESTONE, 5));
        menu.clicked(S + 3, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(lockedTo(bench, 3, new ItemStack(Items.COBBLESTONE)) && bench.storage().getItem(3).getCount() == 5,
                "a middle click locks the slot to what it holds, which stays");
        menu.clicked(S + 4, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(bench.storage().lock(4).isEmpty(), "an empty free slot does not lock");
        menu.clicked(S + 3, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(bench.storage().lock(3).isEmpty() && bench.storage().getItem(3).getCount() == 5,
                "locked and holding items: a middle click frees it and the items stay");
        menu.clicked(S + 3, MIDDLE, ClickType.CLONE, player);
        bench.storage().setItem(3, ItemStack.EMPTY);
        helper.assertTrue(lockedTo(bench, 3, new ItemStack(Items.COBBLESTONE)), "locked again, then emptied");
        menu.clicked(S + 3, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(bench.storage().lock(3).isEmpty(), "locked and empty: a middle click frees it");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aLockedSlotTakesOnlyItsItem(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = lockedEmpty(helper, bench, player);

        menu.setCarried(new ItemStack(Items.DIRT, 3));
        menu.clicked(S, 0, ClickType.PICKUP, player);
        helper.assertTrue(bench.storage().getItem(0).isEmpty() && menu.getCarried().is(Items.DIRT), "a click with dirt leaves the slot empty");
        menu.setCarried(ItemStack.EMPTY);

        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 4));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.storage().getItem(0).isEmpty() && bench.storage().getItem(1).is(Items.DIRT), "shift-clicked dirt skips it");

        IItemHandler pipe = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(BENCH), Direction.DOWN);
        helper.assertTrue(pipe != null && pipe.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1
                && !pipe.isItemValid(0, new ItemStack(Items.DIRT)), "a hopper or pipe cannot put dirt there");
        helper.assertTrue(pipe.insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false).isEmpty()
                && bench.storage().getItem(0).getCount() == 2, "but cobblestone goes in");

        menu.setCarried(new ItemStack(Items.COBBLESTONE, 3));
        menu.clicked(S, 0, ClickType.PICKUP, player);
        helper.assertTrue(bench.storage().getItem(0).getCount() == 5 && menu.getCarried().isEmpty(), "and so does a click with it");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void itsItemGoesToItsLockedSlotFirst(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        bench.storage().setItem(2, new ItemStack(Items.COBBLESTONE, 10));
        bench.storage().setItem(5, new ItemStack(Items.COBBLESTONE));
        menu.clicked(S + 5, MIDDLE, ClickType.CLONE, player);
        bench.storage().setItem(5, ItemStack.EMPTY);

        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(bench.storage().getItem(5).getCount() == 4 && bench.storage().getItem(2).getCount() == 10,
                "shift-click fills the locked slot, not the stack before it: " + bench.storage().getItem(5));

        helper.assertTrue(bench.stock().insert(new ItemStack(Items.COBBLESTONE, 6)).isEmpty()
                && bench.storage().getItem(5).getCount() == 10 && bench.storage().getItem(2).getCount() == 10, "and so does the bench's own insertion");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theARefillsFromItAndClearPutsBackThereFirst(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        for (int i = 0; i < AutoMode.values().length && bench.autoMode() != AutoMode.ITEM; ++i) {
            bench.cycleAutoMode();
        }
        bench.storage().setItem(4, new ItemStack(Items.OAK_PLANKS, 3));
        menu.clicked(S + 4, MIDDLE, ClickType.CLONE, player);
        bench.matrix().setItem(1, new ItemStack(Items.OAK_PLANKS));
        bench.matrix().setItem(4, new ItemStack(Items.OAK_PLANKS));
        bench.craft(player);
        helper.assertTrue(bench.matrix().getItem(1).getCount() == 1 && bench.matrix().getItem(4).getCount() == 1
                && bench.storage().getItem(4).getCount() == 1, "the A refills from the locked slot");

        bench.clearMatrix();
        helper.assertTrue(bench.matrix().isEmpty() && bench.storage().getItem(4).getCount() == 3 && bench.storage().getItem(0).isEmpty(),
                "Clear puts the planks back in their locked slot, not the first empty one");
        helper.succeed();
    }

    private static ItemStack breakWithSilkTouch(GameTestHelper helper) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.enchant(helper.getLevel().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH), 1);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BENCH);
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        Block.dropResources(state, level, pos, entity, player, pick);
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(state.getBlock().asItem())) {
                ItemStack found = e.getItem().copy();
                e.discard();
                return found;
            }
        }
        return ItemStack.EMPTY;
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void locksSurviveSavingAndSilkTouch(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = open(bench, player);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 2);
        bench.storage().setItem(2, cobble.copy());
        menu.clicked(S + 2, MIDDLE, ClickType.CLONE, player);
        CompoundTag tag = bench.saveWithFullMetadata(helper.getLevel().registryAccess());
        ProjectBenchBlockEntity loaded = (ProjectBenchBlockEntity) ProjectBenchBlockEntity.loadStatic(bench.getBlockPos(), bench.getBlockState(),
                tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null && lockedTo(loaded, 2, cobble) && loaded.storage().lock(3).isEmpty(), "saved and loaded, still locked");
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().benchOnBreak(BreakMode.SILK_TOUCH).archiveOnBreak(BreakMode.DROP_ALL).build());
            ItemStack dropped = breakWithSilkTouch(helper);
            helper.assertTrue(dropped.has(Content.Components.SLOT_LOCKS.get()), "the bench broken with Silk Touch keeps its locks");
            ProjectBenchBlockEntity placed = bench(helper);
            placed.applyComponentsFromItemStack(dropped);
            helper.assertTrue(lockedTo(placed, 2, cobble) && placed.storage().getItem(2).getCount() == 2, "placed again, locked again");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void theStationLocksTheSlotsOfEachHalf(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        ProjectBenchBlock.Pair pair = DoubleBenchGameTests.joinedAt(helper, at);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(at)));
        ProjectBenchMenu menu = Opening.of(pair.first()).menu(0, player.getInventory());
        helper.assertTrue(menu.station(), "a double bench opens the Project Station");
        ProjectBenchBlockEntity second = pair.second();
        second.storage().setItem(0, new ItemStack(Items.COBBLESTONE));
        menu.clicked(menu.storageStart(1), MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(lockedTo(second, 0, new ItemStack(Items.COBBLESTONE)) && pair.first().storage().lock(0).isEmpty(),
                "the second half's slot 0 is locked, the first's is not");
        second.storage().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(!menu.getSlot(menu.storageStart(1)).mayPlace(new ItemStack(Items.DIRT))
                && menu.getSlot(menu.storageStart(0)).mayPlace(new ItemStack(Items.DIRT)), "only the locked half refuses dirt");
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(second.storage().getItem(0).getCount() == 3 && count(pair.first().storage(), Items.COBBLESTONE) == 0,
                "shift-click sends cobblestone to the other half's locked slot before the active half's free ones");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY)
    public static void inCreativeAMiddleClickThereDoesNotClone(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        ProjectBenchMenu menu = open(bench, player);
        bench.storage().setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        menu.clicked(S, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(menu.getCarried().isEmpty() && lockedTo(bench, 0, new ItemStack(Items.COBBLESTONE)), "it locks, nothing cloned");
        player.getInventory().setItem(0, new ItemStack(Items.DIRT));
        menu.clicked(ProjectBenchMenu.PLAYER_END - 9, MIDDLE, ClickType.CLONE, player);
        helper.assertTrue(menu.getCarried().is(Items.DIRT) && menu.getCarried().getCount() == 64, "on the player's own slots it still clones");
        helper.succeed();
    }

    @GameTest(template = GameTests.EMPTY, batch = "projectbench_switches")
    public static void switchedOffLocksDoNothingAndComeBack(GameTestHelper helper) {
        ProjectBenchBlockEntity bench = bench(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ProjectBenchMenu menu = lockedEmpty(helper, bench, player);
        try {
            BenchConfig.CONFIG.override(BenchConfig.Settings.builder().slotLocks(false).build());
            helper.assertTrue(bench.storage().lock(0).isEmpty() && menu.getSlot(S).mayPlace(new ItemStack(Items.DIRT)), "off: the lock is ignored");
            bench.storage().setItem(5, new ItemStack(Items.DIRT));
            menu.clicked(S + 5, MIDDLE, ClickType.CLONE, player);
            menu.clicked(S, MIDDLE, ClickType.CLONE, player);
            BenchConfig.CONFIG.clearOverride();
            helper.assertTrue(bench.storage().lock(5).isEmpty() && lockedTo(bench, 0, new ItemStack(Items.COBBLESTONE)),
                    "off, a middle click neither locks nor frees; on again, the stored lock is back");
        } finally {
            BenchConfig.CONFIG.clearOverride();
        }
        helper.succeed();
    }
}
