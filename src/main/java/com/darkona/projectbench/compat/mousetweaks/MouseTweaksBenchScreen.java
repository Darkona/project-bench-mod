package com.darkona.projectbench.compat.mousetweaks;

import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchScreen;
import com.darkona.projectbench.compat.mousetweaks.WheelRoute.Group;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.ShareMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.WheelScrollDirection;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MouseTweaksBenchScreen extends ProjectBenchScreen implements IMTModGuiContainer3Ex {
    private static final Group[] GROUPS = Group.values();

    private final Inventory inventory;
    private final Proxy[][] matrixProxies;
    private final Proxy[][] storageProxies;
    private final Proxy[] outsideProxies;

    private final List<Slot> turn = new ArrayList<>();
    private final int[] groupStart = new int[GROUPS.length];
    private final int[] groupEnd = new int[GROUPS.length];
    private final Predicate<Group> canMove = this::canMove;
    private boolean turning;
    private Group[] route = new Group[0];
    private boolean push;
    @Nullable
    private Slot source;
    @Nullable
    private Slot underMouse;
    private double scrollY;
    private boolean skipRelease;

    public MouseTweaksBenchScreen(ProjectBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
        int halves = menu.station() ? 2 : 1;
        matrixProxies = new Proxy[halves][ProjectBenchBlockEntity.MATRIX];
        storageProxies = new Proxy[halves][ProjectBenchBlockEntity.STORAGE];
        for (int which = 0; which < halves; ++which) {
            for (int i = 0; i < ProjectBenchBlockEntity.MATRIX; ++i) {
                matrixProxies[which][i] = new Proxy(inventory, menu.slots.get(menu.matrixStart(which) + i));
            }
            for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
                storageProxies[which][i] = new Proxy(inventory, menu.slots.get(menu.storageStart(which) + i));
            }
        }
        outsideProxies = new Proxy[ProjectBenchMenu.OUTSIDE_VISIBLE];
        for (int i = 0; i < ProjectBenchMenu.OUTSIDE_VISIBLE; ++i) {
            outsideProxies[i] = new Proxy(inventory, menu.slots.get(menu.outsideIndex(i)));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY = scrollY;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean MT_isMouseTweaksDisabled() {
        return false;
    }

    @Override
    public boolean MT_isWheelTweakDisabled() {
        return false;
    }

    @Override
    public List<Slot> MT_getSlots() {
        if (!ClientConfig.mouseTweaks() || underMouse == null || menu.handsOff(underMouse)) {
            turning = false;
            return menu.slots;
        }
        Slot from = underMouse;
        Group fromGroup = groupOf(from, false);
        ShareMode share = menu.shareMode();
        boolean playerSide = fromGroup == Group.PLAYER;
        turn.clear();
        if (!playerSide) {
            turn.add(from);
        }
        for (Group group : WheelRoute.reach(fromGroup, share)) {
            groupStart[group.ordinal()] = turn.size();
            addGroup(group, from, playerSide);
            groupEnd[group.ordinal()] = turn.size();
        }
        if (playerSide) {
            turn.add(from);
        }
        push = pushes(from);
        route = WheelRoute.route(fromGroup, push, share);
        source = from;
        turning = true;
        return turn;
    }

    private void addGroup(Group group, Slot from, boolean playerSide) {
        int active = menu.active();
        int halves = menu.station() ? 2 : 1;
        switch (group) {
            case PLAYER -> {
                for (int i = ProjectBenchMenu.PLAYER_START; i < ProjectBenchMenu.PLAYER_END; ++i) {
                    turn.add(menu.slots.get(i));
                }
            }
            case STORAGE -> {
                int first = menu.halfOf(from.index) >= 0 ? menu.halfOf(from.index) : active;
                for (int k = 0; k < halves; ++k) {
                    int which = k == 0 ? first : 1 - first;
                    for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
                        turn.add(playerSide ? menu.slots.get(menu.storageStart(which) + i) : storageProxies[which][i]);
                    }
                }
            }
            case MATRIX -> {
                for (int i = 0; i < ProjectBenchBlockEntity.MATRIX; ++i) {
                    turn.add(playerSide ? menu.slots.get(menu.matrixStart(active) + i) : matrixProxies[active][i]);
                }
            }
            case NEIGHBOUR -> {
                for (int i = 0; i < ProjectBenchMenu.OUTSIDE_VISIBLE; ++i) {
                    Slot real = menu.slots.get(menu.outsideIndex(i));
                    if (real.isActive() && !menu.handsOff(real)) {
                        turn.add(playerSide ? real : outsideProxies[i]);
                    }
                }
            }
            case RESULT -> turn.add(menu.slots.get(menu.resultIndex(active)));
            default -> {
            }
        }
    }

    @Override
    @Nullable
    public Slot MT_getSlotUnderMouse(double mouseX, double mouseY) {
        turning = false;
        underMouse = null;
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                underMouse = slot;
                break;
            }
        }
        return underMouse;
    }

    @Override
    public boolean MT_isCraftingOutput(Slot slot) {
        int index = slot.index;
        return !(slot instanceof Proxy) && menu.halfOf(index) >= 0 && index == menu.resultIndex(menu.halfOf(index));
    }

    @Override
    public boolean MT_isIgnored(Slot slot) {
        if (!turning || slot == source) {
            return false;
        }
        Group group = groupOf(slot instanceof Proxy proxy ? proxy.real : slot, true);
        if (group != WheelRoute.open(route, canMove)) {
            return true;
        }
        return push && group == Group.MATRIX && !takes(slot, group);
    }

    @Override
    public boolean MT_disableRMBDraggingFunctionality() {
        skipRelease = true;
        if (isQuickCrafting) {
            isQuickCrafting = false;
            quickCraftSlots.clear();
            return true;
        }
        return false;
    }

    @Override
    public void MT_clickSlot(Slot slot, int mouseButton, ClickType clickType) {
        Slot real = slot instanceof Proxy proxy ? proxy.real : slot;
        slotClicked(real, real.index, mouseButton, clickType);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        skipRelease = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (skipRelease) {
            skipRelease = false;
            isQuickCrafting = false;
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean canMove(Group group) {
        ItemStack item = source == null ? ItemStack.EMPTY : source.getItem();
        if (item.isEmpty()) {
            return false;
        }
        for (int k = groupStart[group.ordinal()], end = groupEnd[group.ordinal()]; k < end; ++k) {
            Slot slot = turn.get(k);
            if (push ? takes(slot, group) : gives(slot, item)) {
                return true;
            }
        }
        return false;
    }

    private boolean takes(Slot slot, Group group) {
        ItemStack item = source == null ? ItemStack.EMPTY : source.getItem();
        ItemStack present = slot.getItem();
        if (present.isEmpty()) {
            return slot.mayPlace(item) && (group != Group.MATRIX || menu.ghostWants((slot instanceof Proxy proxy ? proxy.real : slot).getContainerSlot(), item));
        }
        return ItemStack.isSameItemSameComponents(present, item) && present.getCount() < slot.getMaxStackSize(present);
    }

    private static boolean gives(Slot slot, ItemStack item) {
        ItemStack present = slot.getItem();
        return !present.isEmpty() && ItemStack.isSameItemSameComponents(present, item);
    }

    private boolean pushes(Slot from) {
        boolean inverted = false;
        boolean positionAware = false;
        try {
            if (Main.config != null) {
                WheelScrollDirection direction = Main.config.wheelScrollDirection;
                inverted = direction.isInverted();
                positionAware = direction.isPositionAware();
            }
        } catch (LinkageError e) {
        }
        return WheelRoute.pushes(scrollY, inverted, positionAware, positionAware && otherSideAbove(from));
    }

    private boolean otherSideAbove(Slot from) {
        boolean fromPlayer = from.container == inventory;
        int above = 0;
        int below = 0;
        for (Slot slot : turn) {
            if ((slot.container == inventory) != fromPlayer) {
                if (slot.y < from.y) {
                    ++above;
                } else {
                    ++below;
                }
            }
        }
        return above > below;
    }

    private Group groupOf(Slot slot, boolean target) {
        int index = slot.index;
        if (index >= ProjectBenchMenu.PLAYER_START && index < ProjectBenchMenu.PLAYER_END) {
            return Group.PLAYER;
        }
        if (index >= menu.outsideIndex(0) && index < menu.outsideIndex(ProjectBenchMenu.OUTSIDE_VISIBLE)) {
            return Group.NEIGHBOUR;
        }
        int which = menu.halfOf(index);
        if (which < 0) {
            return Group.OTHER;
        }
        int matrix = menu.matrixStart(which);
        if (index >= matrix && index < matrix + ProjectBenchBlockEntity.MATRIX) {
            return !target || which == menu.active() ? Group.MATRIX : Group.OTHER;
        }
        if (index == menu.resultIndex(which)) {
            return !target || which == menu.active() ? Group.RESULT : Group.OTHER;
        }
        return menu.storageHalf(index) >= 0 ? Group.STORAGE : Group.OTHER;
    }

    private static final class Proxy extends Slot {
        private final Slot real;

        Proxy(Inventory inventory, Slot real) {
            super(inventory, 0, real.x, real.y);
            this.real = real;
        }

        @Override
        public ItemStack getItem() {
            return real.getItem();
        }

        @Override
        public boolean hasItem() {
            return real.hasItem();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return real.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return real.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return real.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return real.getMaxStackSize(stack);
        }

        @Override
        public boolean isActive() {
            return real.isActive();
        }
    }
}
