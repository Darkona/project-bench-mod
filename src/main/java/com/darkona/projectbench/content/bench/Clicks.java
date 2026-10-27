package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.bench.BenchSlots.OutsideSlot;
import com.darkona.projectbench.content.blueprint.BlueprintContainer;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.projectbench.content.bench.ProjectBenchMenu.PLAYER_END;
import static com.darkona.projectbench.content.bench.ProjectBenchMenu.PLAYER_START;

final class Clicks {
    private final ProjectBenchMenu menu;
    private final QuickMove moves;

    Clicks(ProjectBenchMenu menu, QuickMove moves) {
        this.menu = menu;
        this.moves = moves;
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
                moves.quickMove(player, slot.index);
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
                moves.intoStorage(rest, menu.active());
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
