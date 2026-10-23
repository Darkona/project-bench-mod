package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.bench.BenchSlots.OutsideSlot;
import com.darkona.projectbench.block.bench.BenchSlots.ProcessorSlot;
import com.darkona.projectbench.blueprint.BlueprintContainer;
import com.darkona.projectbench.blueprint.BlueprintItem;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import dev.darkona.jdlib.api.inventory.GridFill;
import dev.darkona.jdlib.api.inventory.Stacks;
import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.projectbench.block.bench.ProjectBenchMenu.BLUEPRINT;
import static com.darkona.projectbench.block.bench.ProjectBenchMenu.PLAYER_END;
import static com.darkona.projectbench.block.bench.ProjectBenchMenu.PLAYER_START;
import static com.darkona.projectbench.block.bench.ProjectBenchMenu.RESULT;
import static com.darkona.projectbench.block.bench.ProjectBenchMenu.STORAGE_START;
import static com.darkona.projectbench.block.bench.ProjectBenchMenu.handsOff;

final class QuickMove {
    private final ProjectBenchMenu menu;

    QuickMove(ProjectBenchMenu menu) {
        this.menu = menu;
    }

    ItemStack quickMove(Player player, int index) {
        Slot slot = menu.slots.get(index);
        if (slot instanceof OutsideSlot outside) {
            return fromOutside(outside);
        }
        if (slot instanceof ProcessorSlot machineSlot) {
            return fromProcessor(machineSlot);
        }
        int half = menu.halfOf(index);
        boolean fromCost = index >= menu.costStart;
        if (!slot.hasItem() || half < 0 && !fromCost && (index < PLAYER_START || index >= PLAYER_END)) {
            return ItemStack.EMPTY;
        }
        int local = half < 0 ? -1 : index - menu.base(half);
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (local == RESULT) {
            if (!fits(stack) || !menu.moveTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (fromCost) {
            if (!menu.moveTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0 && local < STORAGE_START) {
            if (!intoStorage(stack, half) && !menu.moveTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0 && local < BLUEPRINT) {
            boolean moved = intoMatrix(stack);
            moved |= !stack.isEmpty() && intoNeighbours(stack);
            if (!stack.isEmpty() && !menu.moveTo(stack, PLAYER_START, PLAYER_END, false) && !moved) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0) {
            if (!(local == BLUEPRINT && intoArchives(stack)) && !menu.moveTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BlueprintItem.blank(stack)) {
            if (!intoBlueprintSlot(stack)) {
                return ItemStack.EMPTY;
            }
        } else if (Feature.BLUEPRINT.enabled() && BlueprintItem.recorded(stack) && (intoArchives(stack) || intoBlueprintSlot(stack))) {
        } else {
            boolean moved = intoCost(stack);
            moved |= !stack.isEmpty() && intoMatrix(stack);
            moved |= !stack.isEmpty() && intoProcessor(stack);
            boolean stored = !stack.isEmpty() && intoStorage(stack, menu.active());
            stored |= !stack.isEmpty() && intoNeighbours(stack);
            if (!stack.isEmpty() && !stored) {
                boolean hotbar = index >= PLAYER_END - 9;
                if (!menu.moveTo(stack, hotbar ? PLAYER_START : PLAYER_END - 9, hotbar ? PLAYER_END - 9 : PLAYER_END, false) && !moved) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private ItemStack fromOutside(OutsideSlot slot) {
        if (menu.player.level().isClientSide || !slot.isActive() || !slot.mayPickup(menu.player)) {
            return ItemStack.EMPTY;
        }
        ItemStack present = slot.getItem();
        if (present.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = slot.remove(Math.min(present.getCount(), present.getMaxStackSize()));
        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = taken.copy();
        if (menu.bench.shareMode() != ShareMode.NONE) {
            intoMatrix(taken);
            if (!taken.isEmpty()) {
                intoStorage(taken, menu.active());
            }
        }
        if (!taken.isEmpty()) {
            menu.moveTo(taken, PLAYER_START, PLAYER_END, true);
        }
        if (!taken.isEmpty()) {
            ItemStack rest = slot.putBack(taken);
            if (!rest.isEmpty()) {
                menu.player.drop(rest, false);
            }
        }
        int moved = original.getCount() - taken.getCount();
        return moved > 0 ? original.copyWithCount(moved) : ItemStack.EMPTY;
    }

    private ItemStack fromProcessor(ProcessorSlot slot) {
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(menu.player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean result = slot.result();
        if (!menu.moveTo(stack, PLAYER_START, PLAYER_END, result || slot.brewing)) {
            return ItemStack.EMPTY;
        }
        if (result) {
            slot.onQuickCraft(stack, original);
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(menu.player, stack);
        return original;
    }

    private boolean intoCost(ItemStack stack) {
        int at = menu.costIndex(menu.active());
        return menu.slots.get(at).mayPlace(stack) && menu.moveTo(stack, at, at + 1, false);
    }

    private boolean intoBlueprintSlot(ItemStack stack) {
        int at = menu.blueprintIndex(menu.active());
        return !menu.slots.get(at).hasItem() && menu.moveTo(stack, at, at + 1, false);
    }

    private boolean intoStorage(ItemStack stack, int which) {
        boolean moved = menu.benchOf(which).storage().insertLocked(stack);
        if (menu.second != null) {
            moved |= menu.benchOf(1 - which).storage().insertLocked(stack);
        }
        int start = menu.storageStart(which);
        moved |= !stack.isEmpty() && menu.moveTo(stack, start, start + ProjectBenchBlockEntity.STORAGE, false);
        if (menu.second != null && !stack.isEmpty()) {
            int other = menu.storageStart(1 - which);
            moved |= menu.moveTo(stack, other, other + ProjectBenchBlockEntity.STORAGE, false);
        }
        return moved;
    }

    private boolean intoArchives(ItemStack stack) {
        if (!BlueprintItem.recorded(stack) || !menu.archives.stillValid(menu.player)) {
            return false;
        }
        int rest = Stacks.insert(menu.archives, stack).getCount();
        if (rest == stack.getCount()) {
            return false;
        }
        stack.setCount(rest);
        return true;
    }

    private boolean intoProcessor(ItemStack stack) {
        Processor processor = menu.openProcessor();
        if (processor == null) {
            return false;
        }
        int target = processor.shiftTarget(stack, menu.player.level());
        if (target < 0) {
            return false;
        }
        if (processor.brewing() && target == Processor.BOTTLES) {
            int first = menu.processorIndex(processor, 0);
            return menu.moveTo(stack, first, first + 3, false);
        }
        int at = menu.processorIndex(processor, target);
        if (menu.moveTo(stack, at, at + 1, false)) {
            return true;
        }
        int ingredient = menu.processorIndex(processor, Processor.INGREDIENT);
        return processor.brewing() && target == Processor.BREWING_FUEL && menu.moveTo(stack, ingredient, ingredient + 1, false);
    }

    private boolean intoNeighbours(ItemStack stack) {
        if (menu.player.level().isClientSide) {
            return false;
        }
        return switch (menu.bench.shareMode()) {
            case NONE -> false;
            case OPEN -> intoOpenTab(stack);
            case ALL -> intoEveryTab(stack);
        };
    }

    private boolean intoEveryTab(ItemStack stack) {
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); ++pass) {
            for (int i = 0, n = menu.outsidePanels.size(); i < n && !stack.isEmpty(); ++i) {
                ExternalInventories.Panel panel = menu.outsidePanels.get(i);
                IItemHandler inventory = playerTarget(panel);
                if (inventory != null && (pass == 1 || ExternalInventories.holds(inventory, stack))) {
                    stack.setCount(ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false).getCount());
                }
            }
        }
        return stack.getCount() != before;
    }

    private boolean intoOpenTab(ItemStack stack) {
        IItemHandler inventory = playerTarget(menu.selectedPanel());
        if (inventory == null) {
            return false;
        }
        ItemStack rest = ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false);
        if (rest.getCount() == stack.getCount()) {
            return false;
        }
        stack.setCount(rest.getCount());
        return true;
    }

    @Nullable
    private IItemHandler playerTarget(@Nullable ExternalInventories.Panel panel) {
        return panel == null || handsOff(panel) ? null : menu.handlerOf(panel);
    }

    private boolean intoMatrix(ItemStack stack) {
        boolean moved = ghosted(stack) && intoGhosts(stack) > 0;
        return !stack.isEmpty() && BenchStacks.topUp(menu.activeBench().matrix(), stack) > 0 || moved;
    }

    private int intoGhosts(ItemStack stack) {
        ProjectBenchBlockEntity target = menu.activeBench();
        Blueprint blueprint = target.blueprint();
        return blueprint == null ? 0 : GridFill.spread(target.matrix(), blueprint.pattern(), stack);
    }

    private boolean ghosted(ItemStack stack) {
        Blueprint blueprint = menu.fillsGhosts();
        return blueprint != null && GridFill.acceptsAny(blueprint.pattern(), stack);
    }

    private boolean fits(ItemStack stack) {
        int room = 0;
        for (int i = PLAYER_START; i < PLAYER_END && room < stack.getCount(); ++i) {
            ItemStack present = menu.slots.get(i).getItem();
            if (present.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(present, stack)) {
                room += present.getMaxStackSize() - present.getCount();
            }
        }
        return room >= stack.getCount();
    }

    boolean outsideClick(OutsideSlot slot, int button, ClickType type, Player player) {
        ItemStack present = slot.getItem();
        int stack = present.getMaxStackSize();
        boolean oversized = present.getCount() > stack;
        if (type == ClickType.SWAP && (button >= 0 && button < Inventory.getSelectionSize() || button == Inventory.SLOT_OFFHAND)) {
            Inventory inventory = player.getInventory();
            if (present.isEmpty()) {
                return false;
            }
            if (!inventory.getItem(button).isEmpty()) {
                return oversized;
            }
            if (slot.mayPickup(player)) {
                inventory.setItem(button, slot.safeTake(Math.min(present.getCount(), stack), Integer.MAX_VALUE, player));
            }
            return true;
        }
        if (!oversized || button != 0 && button != 1) {
            return false;
        }
        if (type == ClickType.QUICK_MOVE) {
            if (slot.mayPickup(player)) {
                menu.quickMoveStack(player, slot.index);
            }
            return true;
        }
        if (type == ClickType.PICKUP) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                slot.tryRemove(button == 0 ? stack : (stack + 1) / 2, Integer.MAX_VALUE, player).ifPresent(taken -> {
                    menu.setCarried(taken);
                    slot.onTake(player, taken);
                });
                return true;
            }
            return true;
        }
        return false;
    }

    @Nullable
    private Container archiveOf(int slot) {
        Container archive = menu.archives.partOf(slot);
        return archive.stillValid(menu.player) ? archive : null;
    }

    boolean store(int slot) {
        ItemStack carried = menu.getCarried();
        Container archive = archiveOf(slot);
        int slotInArchive = slot % BlueprintArchiveBlockEntity.SLOTS;
        if (archive == null || !BlueprintItem.recorded(carried) || !archive.canPlaceItem(slotInArchive, carried)) {
            return false;
        }
        ItemStack stored = archive.getItem(slotInArchive);
        int max = Math.min(archive.getMaxStackSize(carried), carried.getMaxStackSize());
        if (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, carried)) {
            int moved = Math.min(carried.getCount(), max - stored.getCount());
            if (moved <= 0) {
                return false;
            }
            archive.setItem(slotInArchive, carried.copyWithCount(stored.getCount() + moved));
            carried.shrink(moved);
            menu.setCarried(carried);
            return true;
        }
        if (carried.getCount() > max) {
            return false;
        }
        archive.setItem(slotInArchive, carried);
        menu.setCarried(stored);
        return true;
    }

    boolean swap(int slot) {
        Container archive = archiveOf(slot);
        int slotInArchive = slot % BlueprintArchiveBlockEntity.SLOTS;
        if (archive == null) {
            return false;
        }
        ItemStack stored = archive.getItem(slotInArchive);
        BlueprintContainer target = menu.activeBench().blueprintSlot();
        ItemStack held = target.getItem(0);
        if (stored.isEmpty() && held.isEmpty()) {
            return false;
        }
        if (stored.getCount() > 1) {
            if (!held.isEmpty()) {
                return false;
            }
            target.setItem(0, archive.removeItem(slotInArchive, 1));
            return true;
        }
        archive.setItem(slotInArchive, held);
        target.setItem(0, stored);
        return true;
    }

    boolean recordRecipe(RecipeHolder<CraftingRecipe> holder, boolean fill) {
        ProjectBenchBlockEntity target = menu.activeBench();
        if (!target.record(holder)) {
            return false;
        }
        Blueprint recorded = target.blueprint();
        List<Ingredient> grid = Blueprint.grid(holder.value());
        if (!fill || recorded == null || grid == null) {
            return true;
        }
        List<Slot> stock = menu.stockSlots();
        int start = menu.matrixStart(menu.active());
        for (int i = 0; i < ProjectBenchBlockEntity.MATRIX; ++i) {
            Slot at = menu.slots.get(start + i);
            Ingredient want = grid.get(i);
            ItemStack held = at.getItem();
            if (!held.isEmpty() && !want.test(held)) {
                ItemStack rest = held.copy();
                intoStorage(rest, menu.active());
                if (!rest.isEmpty()) {
                    menu.moveTo(rest, PLAYER_START, PLAYER_END, false);
                }
                at.set(rest);
            }
            if (!want.isEmpty() && at.getItem().isEmpty()) {
                Slot from = source(stock, recorded.ghost(i), want);
                if (from != null) {
                    at.set(from.remove(1));
                }
            }
        }
        return true;
    }

    @Nullable
    private Slot source(List<Slot> stock, ItemStack ghost, Ingredient want) {
        Slot any = null;
        for (Slot slot : stock) {
            ItemStack s = slot.getItem();
            if (s.isEmpty() || !want.test(s) || !slot.mayPickup(menu.player)) {
                continue;
            }
            if (s.is(ghost.getItem())) {
                return slot;
            }
            if (any == null) {
                any = slot;
            }
        }
        return any;
    }
}
