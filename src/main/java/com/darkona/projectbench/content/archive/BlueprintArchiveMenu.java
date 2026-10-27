package com.darkona.projectbench.content.archive;

import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.content.blueprint.BlueprintContainer;
import com.darkona.projectbench.core.BenchMenu;
import com.darkona.projectbench.index.BenchMenuTypes;
import dev.darkona.jdlib.api.inventory.ConcatContainer;
import dev.darkona.jdlib.api.inventory.Stacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BlueprintArchiveMenu extends BenchMenu {
    public static final int COLUMNS = 9;
    public static final int MAX_ROWS = 6;

    private final Container blueprints;
    private final int totalRows;
    private final int rows;
    private final boolean server;
    private int scroll;

    public BlueprintArchiveMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, null, buf.readVarInt());
    }

    public BlueprintArchiveMenu(int id, Inventory inventory, @Nullable ConcatContainer shelf, int blocks) {
        super(BenchMenuTypes.BLUEPRINT_ARCHIVE.get(), id);
        this.totalRows = Math.max(1, blocks) * BlueprintArchiveBlockEntity.SLOTS / COLUMNS;
        this.rows = Math.min(totalRows, MAX_ROWS);
        this.server = shelf != null;
        this.blueprints = shelf != null ? shelf
                : new BlueprintContainer(rows * COLUMNS, () -> BenchConfig.get().archiveStack());
        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < COLUMNS; ++column) {
                int index = column + row * COLUMNS;
                int x = 8 + column * 18;
                int y = 18 + row * 18;
                addSlot(shelf != null ? new ScrolledSlot(shelf, index, x, y) : new BlueprintContainer.BlueprintSlot(blueprints, index, x, y));
            }
        }
        addPlayerInventory(inventory, 8, 18 + rows * 18 + 14);
    }

    public int rows() {
        return rows;
    }

    public int totalRows() {
        return totalRows;
    }

    public int scroll() {
        return scroll;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > totalRows - rows) {
            return false;
        }
        scroll = id;
        return true;
    }

    private int ownSlots() {
        return rows * COLUMNS;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (server && index >= ownSlots()) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            ItemStack rest = Stacks.insert(blueprints, stack);
            if (rest.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.setByPlayer(rest);
            return original;
        }
        return moveBetween(index, ownSlots());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean gather = server && clickType == ClickType.PICKUP_ALL && slotId >= 0 && !getCarried().isEmpty()
                && (!slots.get(slotId).hasItem() || !slots.get(slotId).mayPickup(player));
        super.clicked(slotId, button, clickType, player);
        if (gather) {
            gatherHidden();
        }
    }

    private void gatherHidden() {
        ItemStack carried = getCarried();
        int windowStart = scroll * COLUMNS;
        int windowEnd = windowStart + ownSlots();
        for (int pass = 0; pass < 2; ++pass) {
            for (int i = 0; i < blueprints.getContainerSize() && carried.getCount() < carried.getMaxStackSize(); ++i) {
                if (i >= windowStart && i < windowEnd) {
                    continue;
                }
                ItemStack there = blueprints.getItem(i);
                if (there.isEmpty() || !ItemStack.isSameItemSameComponents(carried, there)
                        || pass == 0 && there.getCount() == there.getMaxStackSize()) {
                    continue;
                }
                ItemStack taken = blueprints.removeItem(i, Math.min(there.getCount(), carried.getMaxStackSize() - carried.getCount()));
                carried.grow(taken.getCount());
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blueprints.stillValid(player);
    }

    private final class ScrolledSlot extends BlueprintContainer.BlueprintSlot {
        private final ConcatContainer shelf;
        private final int visibleIndex;

        ScrolledSlot(ConcatContainer shelf, int visibleIndex, int x, int y) {
            super(shelf, visibleIndex, x, y);
            this.shelf = shelf;
            this.visibleIndex = visibleIndex;
        }

        private int shelfSlot() {
            return visibleIndex + scroll * COLUMNS;
        }

        @Override
        public ItemStack getItem() {
            return container.getItem(shelfSlot());
        }

        @Override
        public void set(ItemStack stack) {
            container.setItem(shelfSlot(), stack);
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            return container.removeItem(shelfSlot(), amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(shelfSlot(), stack);
        }

        @Override
        public void setChanged() {
            shelf.partOf(shelfSlot()).setChanged();
        }
    }
}
