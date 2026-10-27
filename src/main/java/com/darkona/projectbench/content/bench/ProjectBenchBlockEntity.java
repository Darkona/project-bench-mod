package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.foundation.BenchBlock;
import com.darkona.projectbench.foundation.BenchBlockEntity;
import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.blueprint.BlueprintContainer;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
import com.darkona.projectbench.content.blueprint.CostContainer;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import com.darkona.projectbench.config.SideMode;
import dev.darkona.jdlib.api.data.CraftingRecipeCache;
import dev.darkona.jdlib.api.inventory.ChainedItemHandler;
import dev.darkona.jdlib.api.inventory.GridFill;
import dev.darkona.jdlib.api.inventory.SidedItemHandler;
import dev.darkona.jdlib.api.inventory.SlotLockedContainer;
import dev.darkona.jdlib.api.inventory.Stacks;
import dev.darkona.jdlib.api.inventory.Stock;
import dev.darkona.jdlib.api.pattern.Blueprint;
import dev.darkona.jdlib.api.world.RelativeSide;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ProjectBenchBlockEntity extends BenchBlockEntity {
    public static final CraftingRecipeCache RECIPES = new CraftingRecipeCache(256);
    public static final int MATRIX = 9;
    public static final int STORAGE = 18;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String[] CONTAINER_KEYS = {"Matrix", "Storage", "Blueprint", "Cost"};

    private final CraftingGrid matrix = new CraftingGrid();
    private final Storage storage = new Storage(STORAGE);
    private final BlueprintContainer blueprint = new BlueprintContainer(1, () -> 1);
    private final CostContainer cost = new CostContainer();
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer[] containers = {matrix, storage, blueprint, cost};

    private final MatrixHandler matrixHandler = new MatrixHandler(this);
    private final InvWrapper storageHandler = new InvWrapper(storage);
    private final SidedItemHandler[][] views = {
            null,
            sided(new ChainedItemHandler(storageHandler, matrixHandler), SideMode.INVENTORY),
            sided(new ChainedItemHandler(matrixHandler, storageHandler), SideMode.GRID),
            sided(new CombinedInvWrapper(new InvWrapper(blueprint), new InvWrapper(cost)), SideMode.BLUEPRINT),
            sided(new ResultHandler(this), SideMode.RESULT)
    };
    private final SideMode[] sideModes = new SideMode[6];
    private boolean works;
    @Nullable
    private BlockState rulesState;
    @Nullable
    private Config.Settings rulesSettings;
    private boolean rulesAutocraft;
    private final List<ItemStack> leftovers = new ArrayList<>(MATRIX);
    @Nullable
    private RecipeHolder<CraftingRecipe> recipe;

    private List<RecipeHolder<CraftingRecipe>> recipes = List.of();
    private int choice = -1;

    @Nullable
    private ResourceLocation chosen;

    private final MatrixLayout listed = new MatrixLayout();
    @Nullable
    private ItemStack assembled;
    private int assembledCount;

    private boolean updating;

    private ShareMode share = Config.get().shareMode();

    private boolean showcase = Config.get().showcaseDefault();

    private boolean autocraft = Config.get().autocraftDefault();

    private final Substitute substitute = new Substitute();
    private final Showcase display = new Showcase(this);

    private boolean activeSecond;

    private AutoMode autoMode = Config.get().autoDefault();
    private final ExternalInventories external = new ExternalInventories(this);
    private final Stock own = Stock.of(storage);
    private final Stock shared = Stock.chain(own, external);
    @Nullable
    private ProjectBenchBlockEntity partner;
    private final Stock partnerOwn = Stock.lazy(() -> {
        ProjectBenchBlockEntity other = partner();
        return other == null ? null : other.own;
    });
    private final Stock pairOwn = Stock.chain(own, partnerOwn);
    private final Stock pairShared = Stock.chain(pairOwn, Stock.chain(external.unique(), Stock.lazy(() -> {
        ProjectBenchBlockEntity other = partner();
        return other == null ? null : other.external.unique();
    })));
    private final Stock ownStock = Stock.lazy(() -> pairs() ? pairOwn : own);

    public ProjectBenchBlockEntity(BlockPos pos, BlockState state) {
        super(Content.BlockEntities.PROJECT_BENCH.get(), pos, state);
        matrix.addListener(c -> contentsChanged());

        storage.addListener(c -> setChanged());
        blueprint.addListener(c -> blueprintChanged());
        cost.addListener(c -> setChanged());
    }

    public SimpleContainer matrix() {
        return matrix;
    }

    public Storage storage() {
        return storage;
    }

    public boolean toggleLock(int slot) {
        if (!Config.get().slotLocks() || slot < 0 || slot >= STORAGE) {
            return false;
        }
        boolean empty = storage.getItem(slot).isEmpty();
        if (!storage.lock(slot).isEmpty()) {
            storage.setLock(slot, ItemStack.EMPTY);
        } else if (!empty) {
            storage.setLock(slot, storage.getItem(slot));
        } else {
            return false;
        }
        setChanged();
        if (level instanceof ServerLevel server) {
            SlotLocks payload = new SlotLocks(worldPosition, slot, storage.locks().get(slot));
            for (ServerPlayer player : server.players()) {
                if (player.containerMenu instanceof ProjectBenchMenu menu && menu.shows(this)) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
        return true;
    }

    public BlueprintContainer blueprintSlot() {
        return blueprint;
    }

    public CostContainer costSlot() {
        return cost;
    }

    public ResultContainer result() {
        return result;
    }

    @Nullable
    public SidedItemHandler itemHandler(@Nullable Direction side) {
        if (!works()) {
            return null;
        }
        SidedItemHandler[] view = views[sideMode(side).ordinal()];
        return view == null ? null : SidedItemHandler.of(view, side);
    }

    public SideMode sideMode(@Nullable Direction side) {
        if (side == null) {
            return SideMode.INVENTORY;
        }
        checkRules();
        return sideModes[side.get3DDataValue()];
    }

    private boolean works() {
        checkRules();
        return works;
    }

    private void checkRules() {
        BlockState state = getBlockState();
        Config.Settings settings = Config.get();
        if (state == rulesState && settings == rulesSettings && autocraft == rulesAutocraft) {
            return;
        }
        rulesState = state;
        rulesSettings = settings;
        rulesAutocraft = autocraft;
        works = Feature.allows(state.getBlock());
        for (Direction side : DIRECTIONS) {
            sideModes[side.get3DDataValue()] = workOutSideMode(state, settings, side);
        }
    }

    private SideMode workOutSideMode(BlockState state, Config.Settings s, Direction side) {
        if (side == ProjectBenchBlock.partnerSide(state)) {
            return SideMode.NONE;
        }
        RelativeSide relative = RelativeSide.of(state.getValue(BenchBlock.FACING), side);
        SideMode mode = automatic() && Feature.AUTOCRAFT.enabled() ? s.autoSide(relative) : s.benchSide(relative);
        if (mode == SideMode.RESULT) {
            return automates() ? SideMode.RESULT : SideMode.NONE;
        }
        return mode == SideMode.BLUEPRINT && !Feature.BLUEPRINT.enabled() ? SideMode.INVENTORY : mode;
    }

    public boolean automatic() {
        return getBlockState().getBlock() instanceof ProjectBenchBlock block && block.kind().automatic();
    }

    public boolean automates() {
        return autocraft && automatic() && Feature.AUTOCRAFT.enabled();
    }

    public boolean autocraft() {
        return autocraft;
    }

    public int autocraftButton() {
        return !automatic() || !Feature.AUTOCRAFT.enabled() ? 0 : autocraft ? 2 : 1;
    }

    public void toggleAutocraft() {
        if (level == null || autocraftButton() == 0) {
            return;
        }
        autocraft = !autocraft;
        setChanged();
        level.invalidateCapabilities(worldPosition);
    }

    private SidedItemHandler[] sided(IItemHandler view, SideMode mode) {
        SidedItemHandler.Rule rule = side -> sideMode(side) == mode;
        return SidedItemHandler.forEverySide(view, this::works, rule, rule);
    }

    @Nullable
    public RecipeHolder<CraftingRecipe> recipe() {
        return recipe;
    }

    public int choice() {
        return choice;
    }

    public boolean mayCraft(ServerPlayer player) {
        return recipe == null || level == null || recipe.value().isSpecial() || !level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
                || player.getRecipeBook().contains(recipe);
    }

    public int recipeCount() {
        return recipes.size();
    }

    @Nullable
    public Blueprint blueprint() {
        return Feature.BLUEPRINT.enabled() ? BlueprintItem.blueprint(blueprint.getItem(0)) : null;
    }

    public ShareMode shareMode() {
        return Config.get().external() ? first().share : ShareMode.NONE;
    }

    public ProjectBenchBlockEntity first() {
        ProjectBenchBlockEntity other = ProjectBenchBlock.secondHalf(getBlockState()) ? partner() : null;
        return other == null ? this : other;
    }

    @Nullable
    public ProjectBenchBlockEntity partner() {
        BlockState state = getBlockState();
        Direction side = ProjectBenchBlock.partnerSide(state);
        if (side == null || level == null) {
            return null;
        }
        ProjectBenchBlockEntity known = partner;
        if (known == null || !besideOn(known, side)) {
            known = level.getBlockEntity(worldPosition.relative(side)) instanceof ProjectBenchBlockEntity found ? found : null;
            partner = known;
        }
        return known != null && ProjectBenchBlock.joined(state, known.getBlockState()) ? known : null;
    }

    private boolean besideOn(ProjectBenchBlockEntity other, Direction side) {
        BlockPos at = other.getBlockPos();
        return !other.isRemoved() && other.level == level && at.getX() == worldPosition.getX() + side.getStepX()
                && at.getY() == worldPosition.getY() && at.getZ() == worldPosition.getZ() + side.getStepZ();
    }

    public boolean showcase() {
        return showcase && Config.get().showcase();
    }

    public ExternalInventories external() {
        return external;
    }

    public Stock stock() {
        return stock(null);
    }

    public Stock stock(@Nullable Stock window) {
        return switch (shareMode()) {
            case ALL -> pairs() ? pairShared : shared;
            case OPEN -> window != null ? window : ownStock;
            case NONE -> ownStock;
        };
    }

    public Stock stockFor(@Nullable Player player) {
        return stock(player != null && player.containerMenu instanceof ProjectBenchMenu menu ? menu.windowStock(this) : null);
    }

    public Stock ownStock() {
        return ownStock;
    }

    public boolean pairs() {
        return Config.get().slabShare() && partner() != null;
    }

    private void contentsChanged() {
        if (updating || level == null || level.isClientSide) {
            return;
        }
        updating = true;
        try {
            updateResult();
        } finally {
            updating = false;
        }
        setChanged();
        display.send(false);
    }

    private void blueprintChanged() {
        Blueprint bp = blueprint();
        if (bp != null) {
            chosen = bp.recipe();
        }
        contentsChanged();
    }

    public static List<RecipeHolder<CraftingRecipe>> recipesFor(Level level, CraftingInput input) {
        if (input.isEmpty()) {
            return List.of();
        }
        return RECIPES.find(level, input);
    }

    private void updateResult() {
        if (level == null) {
            return;
        }
        boolean relisted = listed.update(matrix.getItems());
        CraftingInput input = null;
        if (relisted) {
            input = CraftingInput.of(3, 3, matrix.getItems());
            recipes = recipesFor(level, input);
        }
        int now = recipes.isEmpty() ? -1 : Math.max(0, indexOf(recipes, chosen));
        ItemStack held = result.getItem(0);
        if (!relisted && now == choice && held == assembled && held.getCount() == assembledCount) {
            return;
        }
        choice = now;
        recipe = choice < 0 ? null : recipes.get(choice);
        result.setRecipeUsed(recipe);

        ItemStack made = ItemStack.EMPTY;
        if (recipe != null) {
            made = recipe.value().assemble(input != null ? input : CraftingInput.of(3, 3, matrix.getItems()), level.registryAccess());
        }
        assembled = made.isItemEnabled(level.enabledFeatures()) ? made : ItemStack.EMPTY;
        assembledCount = assembled.getCount();
        result.setItem(0, assembled);
    }

    private void forgetLayout() {
        listed.clear();
        assembled = null;
    }

    public static int indexOf(List<RecipeHolder<CraftingRecipe>> list, @Nullable ResourceLocation id) {
        if (id != null) {
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).id().equals(id)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void selectRecipe(int index) {
        if (index < 0 || index >= recipes.size() || index == choice) {
            return;
        }
        chosen = recipes.get(index).id();
        updateResult();
        setChanged();
        display.send(false);
    }

    public void craft(Player player) {
        craftOnce(player);
        for (ItemStack rest : leftovers) {
            store(player, rest);
        }
        leftovers.clear();
    }

    private ItemStack craftOnce(@Nullable Player player) {
        leftovers.clear();
        if (level == null || level.isClientSide || recipe == null) {
            return ItemStack.EMPTY;
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, matrix.getItems());
        CraftingInput input = positioned.input();
        if (!recipe.value().matches(input, level)) {
            contentsChanged();
            return ItemStack.EMPTY;
        }
        ItemStack made = recipe.value().assemble(input, level.registryAccess());
        if (player == null && (made.isEmpty() || !made.isItemEnabled(level.enabledFeatures()))) {
            return ItemStack.EMPTY;
        }
        AutoMode mode = autoMode();
        Stock refill = mode == AutoMode.OFF ? null : stockFor(player);
        CommonHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remainders = recipe.value().getRemainingItems(input);
        CommonHooks.setCraftingPlayer(null);
        updating = true;
        substitute.input = mode == AutoMode.TAGS ? input : null;
        try {
            for (int y = 0; y < input.height(); ++y) {
                for (int x = 0; x < input.width(); ++x) {
                    substitute.at = x + y * input.width();
                    GridFill.consume(matrix, refill, positioned.left() + x + (positioned.top() + y) * 3,
                            substitute.input == null ? null : substitute);
                }
            }
        } finally {
            updating = false;
            substitute.input = null;
        }
        for (ItemStack rest : remainders) {
            if (!rest.isEmpty()) {
                leftovers.add(rest);
            }
        }
        contentsChanged();
        return made;
    }

    public void craftOnPulse() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack shown = result.getItem(0);
        boolean ejecting = getBlockState().getBlock() instanceof ProjectBenchBlock block && block.kind().ejecting();
        Direction out = ejecting ? getBlockState().getValue(BenchBlock.FACING).getOpposite() : Direction.DOWN;
        IItemHandler target = automates() && !shown.isEmpty()
                ? level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(out), out.getOpposite()) : null;
        boolean room = target == null ? ejecting : ItemHandlerHelper.insertItemStacked(target, shown, true).getCount() < shown.getCount();
        ItemStack made = automates() && !shown.isEmpty() && room ? craftOnce(null) : ItemStack.EMPTY;
        if (made.isEmpty()) {
            level.levelEvent(LevelEvent.SOUND_CRAFTER_FAIL, worldPosition, 0);
            return;
        }
        made.onCraftedBySystem(level);
        deliver(made, target, out);
        for (ItemStack rest : leftovers) {
            ItemStack left = stock().insert(rest);
            if (!left.isEmpty()) {
                deliver(left, target, out);
            }
        }
        leftovers.clear();
        level.levelEvent(LevelEvent.SOUND_CRAFTER_CRAFT, worldPosition, 0);
    }

    private void deliver(ItemStack stack, @Nullable IItemHandler target, Direction out) {
        if (level == null) {
            return;
        }
        if (target == null) {
            DefaultDispenseItemBehavior.spawnItem(level, stack, 6, out, Vec3.atCenterOf(worldPosition).add(out.getStepX() * 0.7,
                    out.getStepY() * 0.7, out.getStepZ() * 0.7));
            return;
        }
        ItemStack rest = Stacks.insert(storage, ItemHandlerHelper.insertItemStacked(target, stack, false));
        if (!rest.isEmpty()) {
            Block.popResourceFromFace(level, worldPosition, Direction.UP, rest);
        }
    }

    ItemStack pull(int amount, boolean simulate) {
        ItemStack shown = result.getItem(0);
        if (amount <= 0 || shown.isEmpty() || level == null || level.isClientSide || !automates()) {
            return ItemStack.EMPTY;
        }
        int held = own.count(shown);
        if (held > 0) {
            int n = Math.min(amount, Math.min(held, shown.getMaxStackSize()));
            ItemStack out = shown.copyWithCount(n);
            if (!simulate) {
                own.take(out, n);
            }
            return out;
        }
        int n = Math.min(amount, shown.getCount());
        int extra = shown.getCount() - n;
        if (extra > 0 && Stacks.room(storage, shown) < extra) {
            return ItemStack.EMPTY;
        }
        if (simulate) {
            return shown.copyWithCount(n);
        }
        ItemStack made = craftOnce(null);
        if (made.isEmpty()) {
            return ItemStack.EMPTY;
        }
        made.onCraftedBySystem(level);
        if (made.getCount() > n) {
            ItemStack rest = Stacks.insert(storage, made.split(made.getCount() - n));
            if (!rest.isEmpty()) {
                Block.popResourceFromFace(level, worldPosition, Direction.UP, rest);
            }
        }
        for (ItemStack rest : leftovers) {
            ItemStack left = stock().insert(rest);
            if (!left.isEmpty()) {
                Block.popResourceFromFace(level, worldPosition, Direction.UP, left);
            }
        }
        leftovers.clear();
        return made;
    }

    private final class Substitute implements Predicate<ItemStack> {
        @Nullable
        CraftingInput input;
        int at;

        @Override
        public boolean test(ItemStack candidate) {
            return input != null && acceptedInPlace(input, at, candidate);
        }
    }

    private boolean acceptedInPlace(CraftingInput input, int at, ItemStack candidate) {
        if (recipe == null || level == null || !mayUse(recipe.value(), candidate)) {
            return false;
        }
        NonNullList<ItemStack> items = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); ++i) {
            items.set(i, i == at ? candidate.copyWithCount(1) : input.getItem(i));
        }
        return recipe.value().matches(CraftingInput.of(input.width(), input.height(), items), level);
    }

    private static boolean mayUse(CraftingRecipe recipe, ItemStack candidate) {
        if (recipe.getClass() != ShapedRecipe.class && recipe.getClass() != ShapelessRecipe.class) {
            return true;
        }
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0, n = ingredients.size(); i < n; ++i) {
            if (ingredients.get(i).test(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void store(Player player, ItemStack stack) {
        ItemStack rest = Config.get().remaindersToStorage() ? stockFor(player).insert(stack) : stack;
        if (!rest.isEmpty() && !player.getInventory().add(rest)) {
            player.drop(rest, false);
        }
    }

    public void balance() {
        if (Config.get().balanceButton()) {
            Stacks.balance(matrix);
        }
    }

    public void clearMatrix() {
        clearMatrix(null);
    }

    public void clearMatrix(@Nullable Player player) {
        if (level == null || level.isClientSide || !Config.get().clearMatrix()) {
            return;
        }
        Stock into = player == null ? stockFor(null) : Stock.chain(stockFor(player), Stock.playerPockets(player.getInventory()));
        moveMatrix(() -> GridFill.clear(matrix, into));
    }

    public void fillMatrix() {
        fillMatrix(null);
    }

    public void fillMatrix(@Nullable Player player) {
        Blueprint bp = blueprint();
        if (level == null || level.isClientSide || bp == null || !Config.get().autoFill()) {
            return;
        }
        moveMatrix(() -> GridFill.fill(matrix, bp.pattern(), stockFor(player)));
    }

    private void moveMatrix(BooleanSupplier move) {
        updating = true;
        boolean moved;
        try {
            moved = move.getAsBoolean();
        } finally {
            updating = false;
        }
        if (moved) {
            contentsChanged();
        }
    }

    public void cycleShareMode() {
        setShareMode(shareMode().next());
    }

    public void setShareMode(ShareMode mode) {
        share = mode;
        setChanged();
        ProjectBenchBlockEntity other = partner();
        if (other != null) {
            other.share = mode;
            other.setChanged();
        }
    }

    public AutoMode autoMode() {
        Config.Settings s = Config.get();
        return !s.keepLayout() ? AutoMode.OFF : s.autoButton() ? autoMode : s.autoDefault();
    }

    public void cycleAutoMode() {
        if (!Config.get().autoButton()) {
            return;
        }
        autoMode = autoMode.next();
        setChanged();
    }

    public boolean activeSecond() {
        return first().activeSecond;
    }

    public void toggleActive() {
        ProjectBenchBlockEntity first = first();
        first.activeSecond = !first.activeSecond;
        first.setChanged();
    }

    public void toggleShowcase() {
        showcase = !showcase;
        setChanged();
        display.send(true);
    }

    public Showcase display() {
        return display;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return display.updateTag(registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        display.receive(tag, registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean record() {
        if (!blankBlueprint() || CostContainer.missing(cost.getItem(0)) > 0 || recipe == null || result.getItem(0).isEmpty()) {
            return false;
        }
        write(new Blueprint(matrix.getItems(), recipe.id(), result.getItem(0)));
        return true;
    }

    public boolean record(RecipeHolder<CraftingRecipe> holder) {
        if (!blankBlueprint() || CostContainer.missing(cost.getItem(0)) > 0 || level == null) {
            return false;
        }
        Blueprint recorded = Blueprint.of(holder, level.registryAccess());
        if (recorded == null) {
            return false;
        }
        write(recorded);
        return true;
    }

    public boolean blankBlueprint() {
        return BlueprintItem.blank(blueprint.getItem(0));
    }

    private void write(Blueprint recorded) {
        ItemStack written = blueprint.getItem(0).copy();
        written.set(Content.Components.BLUEPRINT.get(), recorded);
        blueprint.setItem(0, written);
        cost.charge();
    }

    public List<BlueprintArchiveBlockEntity> archives() {
        List<BlueprintArchiveBlockEntity> out = new ArrayList<>(3);
        if (level == null || !Feature.ARCHIVE.enabled() || !Config.get().benchPanel()) {
            return out;
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof BlueprintArchiveBlockEntity archive && !archive.isRemoved()) {
                List<BlueprintArchiveBlockEntity> column = archive.column();
                if (!out.contains(column.get(0))) {
                    out.addAll(column);
                }
            }
        }
        return out;
    }

    public List<ExternalInventories.Panel> externalPanels() {
        return Config.get().external() ? external.panels(Config.get().externalSlots()) : List.of();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updateResult();
        }
    }

    @Override
    public void setRemoved() {
        external.forget();
        super.setRemoved();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (keeping()) {
            List<ItemStack> all = new ArrayList<>(MATRIX + STORAGE + 2);
            for (SimpleContainer container : containers) {
                all.addAll(container.getItems());
            }
            components.set(Content.Components.CONTENTS.get(), ItemContainerContents.fromItems(all));
            if (storage.anyLock()) {
                components.set(Content.Components.SLOT_LOCKS.get(), storage.locksAsContents());
            }
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemContainerContents locks = input.get(Content.Components.SLOT_LOCKS);
        if (locks != null) {
            storage.setLocks(locks);
        }
        ItemContainerContents contents = input.get(Content.Components.CONTENTS);
        if (contents == null) {
            return;
        }
        NonNullList<ItemStack> all = NonNullList.withSize(MATRIX + STORAGE + 2, ItemStack.EMPTY);
        contents.copyInto(all);
        int at = 0;
        for (SimpleContainer container : containers) {
            List<ItemStack> items = container.getItems();
            for (int i = 0; i < items.size(); ++i) {
                items.set(i, all.get(at++));
            }
        }
        forgetLayout();
        if (level != null && !level.isClientSide) {
            updateResult();
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (SimpleContainer container : containers) {
            Containers.dropContents(level, pos, container);
        }
        result.clearContent();
    }

    @Override
    public void clearContent() {
        updating = true;
        try {
            for (SimpleContainer container : containers) {
                container.clearContent();
            }
            storage.clearLocks();
            result.clearContent();
        } finally {
            updating = false;
        }
        recipe = null;
        recipes = List.of();
        choice = -1;
        forgetLayout();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < containers.length; ++i) {
            tag.put(CONTAINER_KEYS[i], ContainerHelper.saveAllItems(new CompoundTag(), containers[i].getItems(), registries));
        }
        if (storage.anyLock()) {
            tag.put("Locks", ContainerHelper.saveAllItems(new CompoundTag(), storage.locks(), registries));
        }
        if (chosen != null) {
            tag.putString("Recipe", chosen.toString());
        }
        tag.putByte("Share", (byte) share.ordinal());
        tag.putBoolean("Showcase", showcase);
        tag.putBoolean("ActiveSecond", activeSecond);
        tag.putBoolean("Autocraft", autocraft);
        tag.putInt("Auto", autoMode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < containers.length; ++i) {
            load(containers[i], tag, CONTAINER_KEYS[i], registries);
        }
        storage.clearLocks();
        ContainerHelper.loadAllItems(tag.getCompound("Locks"), storage.locks(), registries);
        if (tag.contains("Share")) {
            share = ShareMode.of(tag.getByte("Share"));
        }
        activeSecond = tag.getBoolean("ActiveSecond");
        if (tag.contains("Autocraft")) {
            autocraft = tag.getBoolean("Autocraft");
        }
        if (tag.contains("Auto")) {
            autoMode = AutoMode.of(tag.getInt("Auto"));
        }
        if (tag.contains("Showcase")) {
            showcase = tag.getBoolean("Showcase");
        }
        chosen = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        forgetLayout();

        if (level != null && !level.isClientSide) {
            updateResult();
        }
    }

    public static final class Storage extends SlotLockedContainer {
        public Storage(int size) {
            super(size);
        }

        @Override
        protected boolean locksApply() {
            return Config.get().slotLocks();
        }
    }

    private static final class CraftingGrid extends SimpleContainer implements CraftingContainer {
        CraftingGrid() {
            super(ProjectBenchBlockEntity.MATRIX);
        }

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }
    }
}
