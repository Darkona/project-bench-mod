package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public final class ExternalInventories implements Stock {
    private static final Direction[] SIDES = Direction.values();

    private static final int UP = Direction.UP.get3DDataValue();

    static final byte UNKNOWN = 0;
    public static final int NOTHING = 1;
    public static final int STORAGE = 2;
    public static final int SIDED = 3;
    public static final int PROCESSOR = 4;
    public static final int NO_TAB = 0;
    public static final int TAB_SLOTS = 1;
    public static final int TAB_PROCESSOR = 2;

    private final BlockEntity bench;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IItemHandler, Direction>[] caches = new BlockCapabilityCache[6];
    private final byte[] kinds = new byte[6];
    private final Block[] blocks = new Block[6];
    private final BlockEntity[] processors = new BlockEntity[6];
    private final int[] faceSlots = new int[6];
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final Stock everySide;
    private final Stock unique;

    public ExternalInventories(BlockEntity bench) {
        this.bench = bench;
        Stock chain = new HandlerStock(() -> handler(0));
        Stock notShared = new HandlerStock(() -> leftToPartner(0) ? null : handler(0));
        for (int next = 1; next < 6; ++next) {
            int sideIndex = next;
            chain = Stock.chain(chain, new HandlerStock(() -> handler(sideIndex)));
            notShared = Stock.chain(notShared, new HandlerStock(() -> leftToPartner(sideIndex) ? null : handler(sideIndex)));
        }
        this.everySide = chain;
        Stock taking = notShared;
        this.unique = new Stock() {
            @Override
            public int count(ItemStack like) {
                return taking.count(like);
            }

            @Override
            public int take(ItemStack like, int amount) {
                return taking.take(like, amount);
            }

            @Override
            public ItemStack find(Predicate<ItemStack> test) {
                return taking.find(test);
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                return ExternalInventories.this.insert(stack, true);
            }
        };
    }

    public Stock unique() {
        return unique;
    }

    public boolean leftToPartner(int sideIndex) {
        BlockState self = bench.getBlockState();
        Level level = bench.getLevel();
        if (level == null || !(self.getBlock() instanceof ProjectBenchBlock) || !ProjectBenchBlock.secondHalf(self)) {
            return false;
        }
        Direction partner = ProjectBenchBlock.partnerSide(self);
        if (partner == null) {
            return false;
        }
        BlockState there = level.getBlockState(probe.setWithOffset(bench.getBlockPos(), SIDES[sideIndex]));
        return there.getBlock() instanceof ChestBlock && there.hasProperty(ChestBlock.TYPE) && there.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && ChestBlock.getConnectedDirection(there) == partner;
    }

    @Nullable
    public IItemHandler handler(int sideIndex) {
        BlockCapabilityCache<IItemHandler, Direction> cache = cache(sideIndex);
        if (cache == null) {
            return null;
        }
        return tab(sideIndex, cache) == TAB_SLOTS ? cache.getCapability() : null;
    }

    @Nullable
    public BlockEntity processor(int sideIndex) {
        BlockCapabilityCache<IItemHandler, Direction> cache = cache(sideIndex);
        if (cache == null || tab(sideIndex, cache) != TAB_PROCESSOR) {
            return null;
        }
        BlockEntity found = processors[sideIndex];
        if (found == null || found.isRemoved()) {
            found = cache.level().getBlockEntity(cache.pos());
            processors[sideIndex] = found;
        }
        return Processor.of(found) == null ? null : found;
    }

    @Nullable
    private BlockCapabilityCache<IItemHandler, Direction> cache(int sideIndex) {
        Level level = bench.getLevel();
        if (sideIndex == UP || !(level instanceof ServerLevel server) || !Config.get().external()) {
            return null;
        }
        Direction side = SIDES[sideIndex];
        if (bench.getBlockState().getBlock() instanceof ProjectBenchBlock && side == ProjectBenchBlock.partnerSide(bench.getBlockState())) {
            return null;
        }
        BlockPos at = probe.setWithOffset(bench.getBlockPos(), side);
        BlockCapabilityCache<IItemHandler, Direction> cache = caches[sideIndex];
        Block there = cache != null && kinds[sideIndex] != UNKNOWN ? blocks[sideIndex] : server.getBlockState(at).getBlock();
        if (there == ModBlocks.BLUEPRINT_ARCHIVE.get() || !Feature.allows(there)) {
            return null;
        }
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, server, at.immutable(), side.getOpposite(),
                    () -> !bench.isRemoved(), () -> kinds[sideIndex] = UNKNOWN);
            caches[sideIndex] = cache;
            kinds[sideIndex] = UNKNOWN;
        }
        return cache;
    }

    private int tab(int sideIndex, BlockCapabilityCache<IItemHandler, Direction> cache) {
        int kind = kind(sideIndex, cache);
        Config.Settings settings = Config.get();
        boolean listed = kind >= SIDED && Config.alwaysShow().contains(blocks[sideIndex]);
        return tab(kind, settings.processors(), settings.partialTabs() || listed);
    }

    private int kind(int sideIndex, BlockCapabilityCache<IItemHandler, Direction> cache) {
        int kind = kinds[sideIndex];
        if (kind == UNKNOWN) {
            cache.getCapability();
            kind = classify(cache.level(), cache.pos());
            kinds[sideIndex] = (byte) kind;
            blocks[sideIndex] = cache.level().getBlockState(cache.pos()).getBlock();
            processors[sideIndex] = null;
        }
        return kind;
    }

    private int classify(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Processor processor = Processor.of(blockEntity);
        if (processor != null) {
            return PROCESSOR + processor.ordinal();
        }
        if (blockEntity instanceof ProjectBenchBlockEntity) {
            return STORAGE;
        }
        for (Direction face : SIDES) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, face);
            faceSlots[face.get3DDataValue()] = handler == null ? -1 : handler.getSlots();
        }
        return kindOf(faceSlots);
    }

    public static int kindOf(int[] faceSlots) {
        boolean any = false;
        for (int slots : faceSlots) {
            any |= slots >= 0;
            if (slots != faceSlots[0]) {
                return SIDED;
            }
        }
        return any ? STORAGE : NOTHING;
    }

    public static int tab(int kind, boolean processors, boolean partialTabs) {
        if (kind >= PROCESSOR) {
            return processors ? TAB_PROCESSOR : partialTabs ? TAB_SLOTS : NO_TAB;
        }
        return kind == STORAGE || kind == SIDED && partialTabs ? TAB_SLOTS : NO_TAB;
    }

    public void forget() {
        Arrays.fill(caches, null);
        Arrays.fill(kinds, UNKNOWN);
        Arrays.fill(processors, null);
        Arrays.fill(blocks, null);
    }

    public record Panel(int sideIndex, int slots, Component title, ItemStack icon, boolean handsOff, @Nullable Processor processor) {
        public static final int SECOND = 6;

        public Panel ofSecond() {
            return new Panel(sideIndex + SECOND, slots, title, icon, handsOff, processor);
        }
    }

    public List<Panel> panels(int maxSlots) {
        List<Panel> out = new ArrayList<>(5);
        for (int sideIndex = 0; sideIndex < 6; ++sideIndex) {
            BlockPos at = bench.getBlockPos().relative(SIDES[sideIndex]);
            Processor processor = Processor.of(processor(sideIndex));
            if (processor != null) {
                out.add(new Panel(sideIndex, processor.slots(), titleOf(at), new ItemStack(bench.getLevel().getBlockState(at).getBlock()), false, processor));
                continue;
            }
            IItemHandler inventory = handler(sideIndex);
            if (inventory != null && inventory.getSlots() > 0) {
                BlockState state = bench.getLevel().getBlockState(at);
                out.add(new Panel(sideIndex, Math.min(inventory.getSlots(), maxSlots), titleOf(at), new ItemStack(state.getBlock()),
                        HandsOff.test(bench.getLevel(), at, state), null));
            }
        }
        return out;
    }

    private Component titleOf(BlockPos at) {
        Level level = bench.getLevel();
        BlockState state = level.getBlockState(at);
        BlockEntity blockEntity = level.getBlockEntity(at);
        if (blockEntity instanceof Nameable named) {
            if (named.hasCustomName()) {
                return named.getDisplayName();
            }
            if (blockEntity instanceof ChestBlockEntity && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                return Component.translatable("container.chestDouble");
            }
            return named.getDisplayName();
        }
        return state.getBlock().getName();
    }

    public static ItemStack set(IItemHandler inventory, int slot, ItemStack stack) {
        if (inventory instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
            return ItemStack.EMPTY;
        }
        ItemStack present = inventory.getStackInSlot(slot);
        if (!present.isEmpty()) {
            inventory.extractItem(slot, present.getCount(), false);
        }
        return stack.isEmpty() ? ItemStack.EMPTY : inventory.insertItem(slot, stack, false);
    }

    @Override
    public int count(ItemStack like) {
        return everySide.count(like);
    }

    @Override
    public int take(ItemStack like, int amount) {
        return everySide.take(like, amount);
    }

    @Override
    public ItemStack find(Predicate<ItemStack> test) {
        return everySide.find(test);
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        return insert(stack, false);
    }

    private ItemStack insert(ItemStack stack, boolean notShared) {
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); ++pass) {
            for (int sideIndex = 0; sideIndex < 6 && !rest.isEmpty(); ++sideIndex) {
                IItemHandler inventory = handler(sideIndex);
                if (inventory != null && (pass == 1 || holds(inventory, rest)) && !(notShared && leftToPartner(sideIndex))) {
                    rest = ItemHandlerHelper.insertItemStacked(inventory, rest, false);
                }
            }
        }
        return rest;
    }

    public static boolean holds(IItemHandler inventory, ItemStack like) {
        for (int i = 0, size = inventory.getSlots(); i < size; ++i) {
            if (ItemStack.isSameItemSameComponents(inventory.getStackInSlot(i), like)) {
                return true;
            }
        }
        return false;
    }
}
