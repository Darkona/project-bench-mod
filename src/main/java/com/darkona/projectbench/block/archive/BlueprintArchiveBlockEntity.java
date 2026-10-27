package com.darkona.projectbench.block.archive;

import com.darkona.projectbench.Content;
import dev.darkona.jdlib.api.inventory.ConcatContainer;
import com.darkona.projectbench.foundation.BenchMenu;
import com.darkona.projectbench.foundation.BenchBlockEntity;
import dev.darkona.jdlib.api.world.RelativeSide;
import com.darkona.projectbench.foundation.BenchBlock;
import dev.darkona.jdlib.api.inventory.SidedItemHandler;
import com.darkona.projectbench.blueprint.BlueprintContainer;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlueprintArchiveBlockEntity extends BenchBlockEntity implements MenuProvider {
    public static final int SLOTS = 36;
    private static final Direction[] VERTICAL = {Direction.UP, Direction.DOWN};
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int EXTRACTS = 8;

    private final BlueprintContainer blueprints = new BlueprintContainer(SLOTS, () -> Config.get().archiveStack()) {
        @Override
        public void setChanged() {
            super.setChanged();
            BlueprintArchiveBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return inReach(player);
        }
    };

    private final InvWrapper wrapper = new InvWrapper(blueprints);
    private final SidedItemHandler[] handlers = SidedItemHandler.forEverySide(new ColumnHandler(), Feature.ARCHIVE::enabled,
            side -> allows(side, 0), side -> allows(side, EXTRACTS));
    private int sideRules;
    @Nullable
    private BlockState rulesState;
    @Nullable
    private Config.Settings rulesSettings;

    @Nullable
    private List<BlueprintArchiveBlockEntity> column;
    @Nullable
    private ConcatContainer shelf;

    public BlueprintArchiveBlockEntity(BlockPos pos, BlockState state) {
        super(Content.BlockEntities.BLUEPRINT_ARCHIVE.get(), pos, state);
    }

    public BlueprintContainer blueprints() {
        return blueprints;
    }

    public List<BlueprintArchiveBlockEntity> column() {
        List<BlueprintArchiveBlockEntity> c = column;
        if (c != null) {
            boolean valid = true;
            for (int i = 0, n = c.size(); i < n && valid; ++i) {
                valid = !c.get(i).isRemoved();
            }
            if (valid) {
                return c;
            }
        }
        c = worksOutColumn();
        column = c;
        shelf = null;
        return c;
    }

    public boolean inReach(Player player) {
        if (isRemoved()) {
            return false;
        }
        List<BlueprintArchiveBlockEntity> c = column();
        for (int i = 0, n = c.size(); i < n; ++i) {
            if (BenchMenu.near(player, c.get(i))) {
                return true;
            }
        }
        return false;
    }

    public ConcatContainer shelf() {
        List<BlueprintArchiveBlockEntity> c = column();
        ConcatContainer s = shelf;
        if (s == null) {
            s = shelfOf(c);
            shelf = s;
        }
        return s;
    }

    public static ConcatContainer shelfOf(List<? extends BlueprintArchiveBlockEntity> archives) {
        List<BlueprintContainer> parts = new ArrayList<>(archives.size());
        for (BlueprintArchiveBlockEntity archive : archives) {
            parts.add(archive.blueprints);
        }
        return new ConcatContainer(SLOTS, parts);
    }

    private List<BlueprintArchiveBlockEntity> worksOutColumn() {
        if (level == null) {
            return List.of(this);
        }
        int height = Config.get().archiveHeight();
        Block block = getBlockState().getBlock();
        BlockPos.MutableBlockPos p = worldPosition.mutable();
        int below = 0;
        while (p.getY() > level.getMinBuildHeight() && level.getBlockState(p.move(Direction.DOWN)).is(block)) {
            ++below;
        }
        int start = worldPosition.getY() - below % height;
        List<BlueprintArchiveBlockEntity> out = new ArrayList<>(height);
        for (int k = 0; k < height; ++k) {
            p.set(worldPosition.getX(), start + k, worldPosition.getZ());
            if (!(level.getBlockEntity(p) instanceof BlueprintArchiveBlockEntity b) || b.isRemoved() || !level.getBlockState(p).is(block)) {
                break;
            }
            out.add(b);
        }
        return out.isEmpty() ? List.of(this) : List.copyOf(out);
    }

    public static void forgetRun(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (Direction d : VERTICAL) {
            p.set(pos);
            for (int i = 0; i < 64 && level.getBlockEntity(p) instanceof BlueprintArchiveBlockEntity b; ++i) {
                b.column = null;
                b.shelf = null;
                p.move(d);
            }
        }
    }

    public SidedItemHandler itemHandler(@Nullable Direction side) {
        return SidedItemHandler.of(handlers, side);
    }

    private boolean allows(@Nullable Direction side, int shift) {
        BlockState state = getBlockState();
        Config.Settings settings = Config.get();
        if (state != rulesState || settings != rulesSettings) {
            rulesState = state;
            rulesSettings = settings;
            int rules = (settings.archiveInserts(null) ? 1 << 6 : 0) | (settings.archiveExtracts(null) ? 1 << 6 + EXTRACTS : 0);
            for (Direction d : DIRECTIONS) {
                RelativeSide relative = RelativeSide.of(state.getValue(BenchBlock.FACING), d);
                int bit = d.get3DDataValue();
                rules |= (settings.archiveInserts(relative) ? 1 << bit : 0) | (settings.archiveExtracts(relative) ? 1 << bit + EXTRACTS : 0);
            }
            sideRules = rules;
        }
        return (sideRules >> shift + (side == null ? 6 : side.get3DDataValue()) & 1) != 0;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (keeping()) {
            components.set(Content.Components.CONTENTS.get(), ItemContainerContents.fromItems(blueprints.getItems()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemContainerContents contents = input.get(Content.Components.CONTENTS);
        if (contents != null) {
            contents.copyInto(blueprints.getItems());
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, blueprints);
    }

    @Override
    public void clearContent() {
        blueprints.clearContent();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BlueprintArchiveMenu(id, inventory, shelf(), column().size());
    }

    private final class ColumnHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return column().size() * SLOTS;
        }

        private BlueprintArchiveBlockEntity of(int slot) {
            return column().get(slot / SLOTS);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return of(slot).blueprints.getItem(slot % SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return of(slot).wrapper.insertItem(slot % SLOTS, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return of(slot).wrapper.extractItem(slot % SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return wrapper.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return wrapper.isItemValid(0, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Blueprints", ContainerHelper.saveAllItems(new CompoundTag(), blueprints.getItems(), registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(blueprints, tag, "Blueprints", registries);
    }
}
