package com.darkona.projectbench.content.bench;

import dev.darkona.jdlib.api.inventory.ConcatContainer;
import dev.darkona.jdlib.api.inventory.Stacks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class BenchSlots {
    private BenchSlots() {
    }

    static final class ResultSlot extends Slot {
        private final ProjectBenchMenu menu;
        private final int which;
        private int removeCount;

        ResultSlot(ProjectBenchMenu menu, int which, int x, int y) {
            super(menu.benchOf(which).result(), 0, x, y);
            this.menu = menu;
            this.which = which;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack getItem() {
            return menu.craftable(which) ? super.getItem() : ItemStack.EMPTY;
        }

        @Override
        public boolean mayPickup(Player taker) {
            return !menu.benchOf(which).isRemoved() && menu.craftable(which);
        }

        @Override
        public ItemStack remove(int amount) {
            if (hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
            return super.remove(amount);
        }

        @Override
        protected void onQuickCraft(ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void onSwapCraft(int amount) {
            removeCount += amount;
        }

        @Override
        protected void checkTakeAchievements(ItemStack stack) {
            ProjectBenchBlockEntity owner = menu.benchOf(which);
            if (removeCount > 0) {
                stack.onCraftedBy(menu.player.level(), menu.player, removeCount);
                EventHooks.firePlayerCraftingEvent(menu.player, stack, owner.matrix());
            }
            owner.result().awardUsedRecipes(menu.player, owner.matrix().getItems());
            removeCount = 0;
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            checkTakeAchievements(stack);
            menu.benchOf(which).craft(taker);
        }

        @Override
        public boolean isFake() {
            return true;
        }
    }

    static final class ArchiveSlot extends Slot {
        ArchiveSlot(ConcatContainer archives, int slot) {
            super(archives, slot, -10000, -10000);
        }

        @Override
        public void setChanged() {
            ((ConcatContainer) container).partOf(getContainerSlot()).setChanged();
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    static class OutsideSlot extends Slot {
        final ProjectBenchMenu menu;
        private final int visibleIndex;

        OutsideSlot(ProjectBenchMenu menu, Container shown, int visibleIndex, int x, int y) {
            super(shown, visibleIndex, x, y);
            this.menu = menu;
            this.visibleIndex = visibleIndex;
        }

        int inventorySlot() {
            return (menu.outsideScroll() + visibleIndex / ProjectBenchMenu.OUTSIDE_COLUMNS) * ProjectBenchMenu.OUTSIDE_COLUMNS + visibleIndex % ProjectBenchMenu.OUTSIDE_COLUMNS;
        }

        @Nullable
        ExternalInventories.Panel panel() {
            ExternalInventories.Panel panel = menu.selectedPanel();
            return panel != null && panel.processor() == null && inventorySlot() < panel.slots() ? panel : null;
        }

        @Override
        public boolean isActive() {
            return panel() != null;
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(Math.min(amount, super.getItem().getMaxStackSize()));
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !ProjectBenchMenu.handsOff(menu.selectedPanel());
        }

        @Override
        public boolean mayPickup(Player taker) {
            return !ProjectBenchMenu.handsOff(menu.selectedPanel());
        }

        @Override
        public int getMaxStackSize() {
            return 99;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(stack.getMaxStackSize(), 99);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean isFake() {
            return true;
        }

        ItemStack putBack(ItemStack stack) {
            ExternalInventories.Panel panel = menu.selectedPanel();
            IItemHandler inventory = panel == null ? null : menu.handlerOf(panel);
            return inventory == null ? stack : ItemHandlerHelper.insertItemStacked(inventory, stack, false);
        }
    }

    static final class RealOutsideSlot extends OutsideSlot {
        RealOutsideSlot(ProjectBenchMenu menu, Container shown, int visibleIndex, int x, int y) {
            super(menu, shown, visibleIndex, x, y);
        }

        @Nullable
        private IItemHandler target() {
            ExternalInventories.Panel panel = panel();
            IItemHandler inventory = panel == null ? null : menu.handlerOf(panel);
            return inventory != null && inventorySlot() < inventory.getSlots() ? inventory : null;
        }

        @Override
        public ItemStack getItem() {
            IItemHandler target = target();
            return target == null ? ItemStack.EMPTY : target.getStackInSlot(inventorySlot());
        }

        @Override
        public void set(ItemStack stack) {
            if (panel() != null) {
                IItemHandler target = target();
                ItemStack rest = target == null ? stack : ExternalInventories.set(target, inventorySlot(), stack);
                if (!rest.isEmpty()) {
                    menu.player.drop(rest, false);
                }
            }
        }

        @Override
        public ItemStack remove(int amount) {
            IItemHandler target = target();
            return target == null ? ItemStack.EMPTY : Stacks.takeAtMostAStack(target, inventorySlot(), amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            IItemHandler target = super.mayPlace(stack) ? target() : null;
            return target != null && target.isItemValid(inventorySlot(), stack);
        }

        @Override
        public boolean mayPickup(Player taker) {
            IItemHandler target = super.mayPickup(taker) ? target() : null;
            return target != null && !target.extractItem(inventorySlot(), 1, true).isEmpty();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            IItemHandler target = target();
            int slot = inventorySlot();
            return target == null || !target.isItemValid(slot, stack) ? 0 : Math.min(target.getSlotLimit(slot), stack.getMaxStackSize());
        }
    }

    static class ProcessorSlot extends Slot {
        final ProjectBenchMenu menu;
        final boolean brewing;
        final int machineSlot;
        private int removeCount;

        ProcessorSlot(ProjectBenchMenu menu, Container shown, int index, int x, int y) {
            super(shown, index, x, y);
            this.menu = menu;
            this.brewing = index >= Processor.BREWING_STAND.firstSlot();
            this.machineSlot = brewing ? index - Processor.BREWING_STAND.firstSlot() : index;
        }

        @Nullable
        Processor processor() {
            Processor processor = menu.openProcessor();
            return processor != null && processor.brewing() == brewing ? processor : null;
        }

        boolean result() {
            Processor processor = processor();
            return processor != null && processor.resultSlot(machineSlot);
        }

        @Override
        public boolean isActive() {
            return processor() != null;
        }

        void countTaken(int amount) {
            if (result() && hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
        }

        @Override
        public ItemStack remove(int amount) {
            countTaken(amount);
            return super.remove(amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            Processor processor = processor();
            return processor != null && !processor.resultSlot(machineSlot) && processor.predictPlace(machineSlot, stack, getItem(), menu.player.level());
        }

        @Override
        public boolean mayPickup(Player taker) {
            return processor() != null;
        }

        @Override
        public int getMaxStackSize() {
            Processor processor = processor();
            return processor != null && processor.bottleSlot(machineSlot) ? 1 : super.getMaxStackSize();
        }

        @Override
        protected void onQuickCraft(ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void checkTakeAchievements(ItemStack stack) {
            if (!result()) {
                return;
            }
            stack.onCraftedBy(menu.player.level(), menu.player, removeCount);
            if (menu.player instanceof ServerPlayer serverPlayer && menu.openMachine() instanceof AbstractFurnaceBlockEntity furnace) {
                furnace.awardUsedRecipesAndPopExperience(serverPlayer);
            }
            removeCount = 0;
            EventHooks.firePlayerSmeltedEvent(menu.player, stack);
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            Processor processor = processor();
            if (processor != null && processor.resultSlot(machineSlot)) {
                checkTakeAchievements(stack);
            } else if (processor != null && processor.bottleSlot(machineSlot) && taker instanceof ServerPlayer serverPlayer) {
                Optional<Holder<Potion>> potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
                if (potion.isPresent()) {
                    EventHooks.onPlayerBrewedPotion(taker, stack);
                    CriteriaTriggers.BREWED_POTION.trigger(serverPlayer, potion.get());
                }
            }
            super.onTake(taker, stack);
        }

        @Override
        public boolean isFake() {
            return true;
        }
    }

    static final class RealProcessorSlot extends ProcessorSlot {
        RealProcessorSlot(ProjectBenchMenu menu, Container shown, int index, int x, int y) {
            super(menu, shown, index, x, y);
        }

        @Nullable
        private Container machine() {
            return processor() != null && menu.openMachine() instanceof Container container ? container : null;
        }

        @Override
        public ItemStack getItem() {
            Container machine = machine();
            return machine == null ? ItemStack.EMPTY : machine.getItem(machineSlot);
        }

        @Override
        public void set(ItemStack stack) {
            Container machine = machine();
            if (machine != null) {
                machine.setItem(machineSlot, stack);
                machine.setChanged();
            } else if (!stack.isEmpty()) {
                menu.player.drop(stack, false);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            countTaken(amount);
            Container machine = machine();
            return machine == null ? ItemStack.EMPTY : machine.removeItem(machineSlot, amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            Container machine = result() ? null : machine();
            return machine != null && machine.canPlaceItem(machineSlot, stack);
        }

        @Override
        public boolean mayPickup(Player taker) {
            return machine() != null;
        }

        @Override
        public void setChanged() {
            Container machine = machine();
            if (machine != null) {
                machine.setChanged();
            } else {
                super.setChanged();
            }
        }
    }
}
