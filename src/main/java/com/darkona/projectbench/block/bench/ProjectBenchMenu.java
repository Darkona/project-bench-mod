package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.Content;
import dev.darkona.jdlib.api.inventory.GridFill;
import dev.darkona.jdlib.api.inventory.Stacks;
import dev.darkona.jdlib.api.inventory.Stock;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.block.BenchMenu;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import dev.darkona.jdlib.api.inventory.ConcatContainer;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.blueprint.BlueprintItem;
import com.darkona.projectbench.blueprint.BlueprintContainer;
import com.darkona.projectbench.blueprint.CostContainer;
import com.darkona.projectbench.block.ContainerSlot;
import com.darkona.projectbench.block.bench.BenchSlots.ArchiveSlot;
import com.darkona.projectbench.block.bench.BenchSlots.OutsideSlot;
import com.darkona.projectbench.block.bench.BenchSlots.ProcessorSlot;
import com.darkona.projectbench.block.bench.BenchSlots.RealOutsideSlot;
import com.darkona.projectbench.block.bench.BenchSlots.RealProcessorSlot;
import com.darkona.projectbench.block.bench.BenchSlots.ResultSlot;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import dev.darkona.jdlib.api.menu.SlotMap;
import dev.darkona.jdlib.api.menu.SlotMapped;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.anti_ad.mc.ipn.api.IPNSlotsIgnoreForInventoryTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

@IPNSlotsIgnoreForInventoryTypes(value = {}, ignoreCraftingSlots = true)
public class ProjectBenchMenu extends BenchMenu implements SlotMapped {
    public static final int MATRIX_START = 0;
    public static final int RESULT = 9;
    public static final int STORAGE_START = 10;
    public static final int BLUEPRINT = STORAGE_START + ProjectBenchBlockEntity.STORAGE;
    public static final int BENCH_SLOTS = BLUEPRINT + 1;
    public static final int PLAYER_START = BLUEPRINT + 1;
    public static final int PLAYER_END = PLAYER_START + PLAYER_SLOTS;

    public static final int ARCHIVE_START = PLAYER_END;

    public static final int BUTTON_RECORD = 0;
    public static final int BUTTON_BALANCE = 1;

    public static final int BUTTON_CLEAR = 2;

    public static final int BUTTON_FILL = 3;

    public static final int BUTTON_SHARE = 4;

    public static final int BUTTON_SHOWCASE = 5;

    public static final int BUTTON_TAB = 1000;

    public static final int BUTTON_SCROLL = 1100;
    public static final int MAX_SCROLL = 100;

    public static final int BUTTON_AUTO = 1201;

    public static final int BUTTON_ACTIVE = 1202;
    public static final int BUTTON_AUTO_SECOND = 1203;
    public static final int BUTTON_RECIPE_SECOND = 1300;
    public static final int BUTTON_AUTOCRAFT = 1204;
    public static final int BUTTON_BALANCE_SECOND = 1205;
    public static final int BUTTON_CLEAR_SECOND = 1206;

    public static final int BUTTON_RECIPE = 6;

    public static final int MAX_RECIPES = 64;

    public static final int BUTTON_ARCHIVE = BUTTON_RECIPE + MAX_RECIPES;

    public static final int OUTSIDE_COLUMNS = 9;
    public static final int OUTSIDE_ROWS = 9;
    public static final int OUTSIDE_VISIBLE = OUTSIDE_COLUMNS * OUTSIDE_ROWS;
    public static final int OUTSIDE_X = 181;
    public static final int PROCESSOR_SLOTS = 8;
    public static final int OUTSIDE_Y = 45;

    public static final int MATRIX_X = 48;
    public static final int MATRIX_Y = 18;
    public static final int RESULT_X = 143;
    public static final int RESULT_Y = 36;
    public static final int BLUEPRINT_X = 17;
    public static final int BLUEPRINT_Y = 36;
    public static final int COST_X = BLUEPRINT_X;
    public static final int COST_Y = BLUEPRINT_Y - 18;
    public static final int STORAGE_Y = 90;
    public static final int PLAYER_Y = 140;
    public static final int WIDTH = 176;

    public static final int MATRIX_WIDTH = 2 * 18 + 16;

    public static final int STATION_OFFSET = 160;
    public static final int STATION_WIDTH = WIDTH + STATION_OFFSET;
    public static final int STATION_HEIGHT = 222 + 36;
    public static final int STATION_STORAGE_X = (STATION_WIDTH - 9 * 18) / 2 + 1;
    public static final int STATION_PLAYER_Y = PLAYER_Y + 36;

    private final ProjectBenchBlockEntity bench;
    @Nullable
    private final ProjectBenchBlockEntity second;
    final Player player;

    private final ConcatContainer archives;

    private final List<ExternalInventories.Panel> outsidePanels;

    private final int outsideStart;
    private final int secondStart;
    private final int costStart;
    private final int processorStart;
    private final SlotMap slotMap;

    private int openTab;
    private int outsideScroll;

    private final DataSlot[] choice;
    private final DataSlot[] auto;
    private final DataSlot[] craftable;

    private final DataSlot share;
    private final DataSlot showcase;

    private final Stock[] windowStocks;
    private final List<List<Slot>> matrixSlots;
    private final List<List<Slot>> stockSlots;

    private final DataSlot[] processorData = new DataSlot[Processor.DATA];
    private final DataSlot autocraft;

    @Nullable
    private final DataSlot active;

    public ProjectBenchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ClientOpening.read(inventory, buf));
    }

    private ProjectBenchMenu(int id, Inventory inventory, ClientOpening o) {
        this(id, inventory, o.first, o.second, o.archives, o.panels);
    }

    private record ClientOpening(ProjectBenchBlockEntity first, @Nullable ProjectBenchBlockEntity second, ConcatContainer archives,
                                 List<ExternalInventories.Panel> panels) {
        static ClientOpening read(Inventory inventory, RegistryFriendlyByteBuf buf) {
            ProjectBenchBlockEntity first = (ProjectBenchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos());
            ConcatContainer archives = clientArchives(buf.readVarInt());
            List<ExternalInventories.Panel> panels = readPanels(buf);
            int station = buf.readByte();
            ProjectBenchBlockEntity second = station == 0 ? null : (ProjectBenchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos());
            first.storage().readLocks(buf);
            if (second != null) {
                second.storage().readLocks(buf);
            }
            return new ClientOpening(first, second, archives, panels);
        }
    }

    private static List<ExternalInventories.Panel> readPanels(RegistryFriendlyByteBuf buf) {
        int panelCount = buf.readVarInt();
        List<ExternalInventories.Panel> panels = new ArrayList<>(panelCount);
        for (int i = 0; i < panelCount; ++i) {
            int slots = buf.readVarInt();
            Component title = ComponentSerialization.STREAM_CODEC.decode(buf);
            ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            boolean handsOff = buf.readBoolean();
            int processor = buf.readByte();
            panels.add(new ExternalInventories.Panel(-1, slots, title, icon, handsOff, processor == 0 ? null : Processor.values()[processor - 1]));
        }
        return panels;
    }

    public ProjectBenchMenu(int id, Inventory inventory, ProjectBenchBlockEntity bench, List<? extends BlueprintArchiveBlockEntity> archives,
                            List<ExternalInventories.Panel> outside) {
        this(id, inventory, bench, null, containers(archives), outside);
    }

    public ProjectBenchMenu(int id, Inventory inventory, ProjectBenchBlockEntity first, @Nullable ProjectBenchBlockEntity second,
                            List<? extends BlueprintArchiveBlockEntity> archives, List<ExternalInventories.Panel> outside) {
        this(id, inventory, first, second, containers(archives), outside);
    }

    private ProjectBenchMenu(int id, Inventory inventory, ProjectBenchBlockEntity bench, @Nullable ProjectBenchBlockEntity second,
                             ConcatContainer archives, List<ExternalInventories.Panel> panels) {
        super(Content.Menus.PROJECT_BENCH.get(), id);
        this.bench = bench;
        this.second = second;
        this.player = inventory.player;
        this.archives = archives;
        this.outsidePanels = List.copyOf(panels);
        this.outsideStart = ARCHIVE_START + archives.getContainerSize();
        this.secondStart = second == null ? -1 : outsideStart + OUTSIDE_VISIBLE;
        boolean station = second != null;
        boolean client = player.level().isClientSide;
        addBench(bench, station ? 0 : -1);
        addPlayerInventory(inventory, station ? STATION_STORAGE_X : 8, station ? STATION_PLAYER_Y : PLAYER_Y);
        for (int i = 0, n = archives.getContainerSize(); i < n; ++i) {
            addSlot(new ArchiveSlot(archives, i));
        }
        SimpleContainer shown = new SimpleContainer(OUTSIDE_VISIBLE);
        int outsideX = (station ? STATION_WIDTH : WIDTH) + OUTSIDE_X - WIDTH;
        for (int i = 0; i < OUTSIDE_VISIBLE; ++i) {
            int x = outsideX + i % OUTSIDE_COLUMNS * 18;
            int y = OUTSIDE_Y + i / OUTSIDE_COLUMNS * 18;
            addSlot(client ? new OutsideSlot(this, shown, i, x, y) : new RealOutsideSlot(this, shown, i, x, y));
        }
        if (second != null) {
            addBench(second, 2);
        }
        costStart = slots.size();
        addSlot(new CostContainer.CostSlot(bench.costSlot(), halfX(0, COST_X, 16), COST_Y, true));
        if (second != null) {
            addSlot(new CostContainer.CostSlot(second.costSlot(), halfX(1, COST_X, 16), COST_Y, true));
        }
        processorStart = slots.size();
        SimpleContainer machine = new SimpleContainer(PROCESSOR_SLOTS);
        int[][] at = {{48, 0}, {48, 36}, {108, 18}, {48, 34}, {71, 41}, {94, 34}, {71, 0}, {9, 0}};
        for (int i = 0; i < PROCESSOR_SLOTS; ++i) {
            boolean brewing = i >= Processor.BREWING_STAND.firstSlot();
            int machineSlot = brewing ? i - Processor.BREWING_STAND.firstSlot() : i;
            int x = outsideX + at[i][0];
            int y = OUTSIDE_Y + at[i][1];
            addSlot(client ? new ProcessorSlot(this, machine, i, brewing, machineSlot, x, y) : new RealProcessorSlot(this, machine, i, brewing, machineSlot, x, y));
        }
        int benchHalves = second == null ? 1 : 2;
        int[] matrixStarts = new int[benchHalves];
        int[] resultIndexes = new int[benchHalves];
        int[] storageStarts = new int[benchHalves];
        for (int which = 0; which < benchHalves; ++which) {
            matrixStarts[which] = matrixStart(which);
            resultIndexes[which] = resultIndex(which);
            storageStarts[which] = storageStart(which);
        }
        slotMap = BenchSlotMap.build(matrixStarts, resultIndexes, storageStarts, outsideStart);
        windowStocks = second == null ? new Stock[]{openStock(bench)} : new Stock[]{openStock(bench), openStock(second)};
        int halves = second == null ? 1 : 2;
        List<List<Slot>> matrices = new ArrayList<>(halves);
        List<List<Slot>> stocks = new ArrayList<>(halves);
        for (int which = 0; which < halves; ++which) {
            matrices.add(List.copyOf(slots.subList(matrixStart(which), matrixStart(which) + ProjectBenchBlockEntity.MATRIX)));
            stocks.add(List.copyOf(stockSlots(second == null ? new int[]{0} : new int[]{which, 1 - which})));
        }
        matrixSlots = List.copyOf(matrices);
        stockSlots = List.copyOf(stocks);
        choice = new DataSlot[halves];
        auto = new DataSlot[halves];
        craftable = new DataSlot[halves];
        choice[0] = addDataSlot(serverValue(bench::choice));
        share = addDataSlot(serverValue(() -> bench.shareMode().ordinal(), ShareMode.ALL.ordinal()));
        showcase = addDataSlot(serverValue(() -> activeBench().showcase() ? 1 : 0));
        auto[0] = addDataSlot(serverValue(() -> bench.autoMode().ordinal(), AutoMode.ITEM.ordinal()));
        craftable[0] = addDataSlot(serverValue(() -> mayCraft(bench), 1));
        autocraft = addDataSlot(serverValue(bench::autocraftButton));
        for (int i = 0; i < Processor.DATA; ++i) {
            int value = i;
            processorData[i] = addDataSlot(serverValue(() -> processorValue(value)));
        }
        if (second == null) {
            active = null;
            return;
        }
        active = addDataSlot(serverValue(() -> bench.activeSecond() ? 1 : 0));
        choice[1] = addDataSlot(serverValue(second::choice));
        auto[1] = addDataSlot(serverValue(() -> second.autoMode().ordinal(), AutoMode.ITEM.ordinal()));
        craftable[1] = addDataSlot(serverValue(() -> mayCraft(second), 1));
    }

    private Stock openStock(ProjectBenchBlockEntity owner) {
        return Stock.chain(owner.ownStock(), Stock.lazyHandler(() -> {
            ExternalInventories.Panel panel = selectedPanel();
            if (panel == null || player.level().isClientSide) {
                return null;
            }
            return externalOf(panel) == owner.external() || owner.pairs() ? handlerOf(panel) : null;
        }));
    }

    @Nullable
    public Stock windowStock(ProjectBenchBlockEntity owner) {
        return owner == bench ? windowStocks[0] : owner == second ? windowStocks[1] : null;
    }

    public static int halfX(int which, int x, int width) {
        return which == 0 ? x : STATION_WIDTH - x - width;
    }

    private int mayCraft(ProjectBenchBlockEntity owner) {
        return !(player instanceof ServerPlayer serverPlayer) || owner.mayCraft(serverPlayer) ? 1 : 0;
    }

    private void addBench(ProjectBenchBlockEntity owner, int storageRow) {
        int which = owner == bench ? 0 : 1;
        int matrixX = halfX(which, MATRIX_X, MATRIX_WIDTH);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 3; ++column) {
                addSlot(new Slot(owner.matrix(), column + row * 3, matrixX + column * 18, MATRIX_Y + row * 18));
            }
        }
        addSlot(new ResultSlot(this, owner, which, halfX(which, RESULT_X, 16), RESULT_Y));
        int storageX = storageRow < 0 ? 8 : STATION_STORAGE_X;
        int storageY = STORAGE_Y + Math.max(0, storageRow) * 18;
        for (int row = 0; row < 2; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new ContainerSlot(owner.storage(), column + row * 9, storageX + column * 18, storageY + row * 18));
            }
        }
        addSlot(new BlueprintContainer.BlueprintSlot(owner.blueprintSlot(), 0, halfX(which, BLUEPRINT_X, 16), BLUEPRINT_Y, true));
    }

    private DataSlot serverValue(IntSupplier value) {
        return serverValue(value, 0);
    }

    private DataSlot serverValue(IntSupplier value, int initial) {
        if (player.level().isClientSide) {
            DataSlot slot = DataSlot.standalone();
            slot.set(initial);
            return slot;
        }
        return new DataSlot() {
            @Override
            public int get() {
                return value.getAsInt();
            }

            @Override
            public void set(int ignored) {
            }
        };
    }

    private static ConcatContainer clientArchives(int blocks) {
        List<Container> out = new ArrayList<>(blocks);
        for (int i = 0; i < blocks; ++i) {
            out.add(new BlueprintContainer(BlueprintArchiveBlockEntity.SLOTS, () -> Config.get().archiveStack()));
        }
        return new ConcatContainer(BlueprintArchiveBlockEntity.SLOTS, out);
    }

    private static ConcatContainer containers(List<? extends BlueprintArchiveBlockEntity> archives) {
        List<Container> out = new ArrayList<>(archives.size());
        for (BlueprintArchiveBlockEntity archive : archives) {
            out.add(archive.blueprints());
        }
        return new ConcatContainer(BlueprintArchiveBlockEntity.SLOTS, out);
    }

    @Override
    public SlotMap slotMap() {
        return slotMap;
    }

    public boolean station() {
        return second != null;
    }

    public int active() {
        return active == null ? 0 : active.get();
    }

    public ProjectBenchBlockEntity benchOf(int which) {
        return which == 1 && second != null ? second : bench;
    }

    public ProjectBenchBlockEntity activeBench() {
        return benchOf(active());
    }

    private int base(int which) {
        return which == 1 && second != null ? secondStart : 0;
    }

    public int matrixStart(int which) {
        return base(which) + MATRIX_START;
    }

    public int resultIndex(int which) {
        return base(which) + RESULT;
    }

    public int storageStart(int which) {
        return base(which) + STORAGE_START;
    }

    public int blueprintIndex(int which) {
        return base(which) + BLUEPRINT;
    }

    public int costIndex(int which) {
        return costStart + (which == 1 && second != null ? 1 : 0);
    }

    public int missingCost() {
        return CostContainer.missing(slots.get(costIndex(active())).getItem());
    }

    public boolean shows(ProjectBenchBlockEntity owner) {
        return bench == owner || second == owner;
    }

    public int storageHalf(int index) {
        int half = halfOf(index);
        int local = half < 0 ? -1 : index - base(half);
        return local >= STORAGE_START && local < BLUEPRINT ? half : -1;
    }

    public ItemStack lock(int which, int slot) {
        return benchOf(which).storage().lock(slot);
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        if (index >= 0 && index < slots.size() && slots.get(index) instanceof OutsideSlot outside && outsideClick(outside, button, type, player)) {
            return;
        }
        if (type == ClickType.CLONE && Config.get().slotLocks()) {
            int which = storageHalf(index);
            if (which >= 0) {
                if (getCarried().isEmpty()) {
                    benchOf(which).toggleLock(index - storageStart(which));
                }
                return;
            }
        }
        super.clicked(index, button, type, player);
    }

    private boolean outsideClick(OutsideSlot slot, int button, ClickType type, Player player) {
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
                quickMoveStack(player, slot.index);
            }
            return true;
        }
        if (type == ClickType.PICKUP) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                slot.tryRemove(button == 0 ? stack : (stack + 1) / 2, Integer.MAX_VALUE, player).ifPresent(taken -> {
                    setCarried(taken);
                    slot.onTake(player, taken);
                });
                return true;
            }
            return true;
        }
        return false;
    }

    public int halfOf(int index) {
        if (index >= 0 && index < BENCH_SLOTS) {
            return 0;
        }
        return second != null && index >= secondStart && index < secondStart + BENCH_SLOTS ? 1 : -1;
    }

    public List<Slot> activeMatrix() {
        return matrixSlots.get(active());
    }

    public List<Slot> stockSlots() {
        return stockSlots.get(active());
    }

    private List<Slot> stockSlots(int[] halves) {
        List<Slot> out = new ArrayList<>(2 * ProjectBenchBlockEntity.STORAGE + PLAYER_SLOTS);
        for (int which : halves) {
            int start = storageStart(which);
            for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
                out.add(slots.get(start + i));
            }
        }
        for (int i = PLAYER_START; i < PLAYER_END; ++i) {
            out.add(slots.get(i));
        }
        return out;
    }

    public int archiveSlots() {
        return archives.getContainerSize();
    }

    public int outsideSlots() {
        return total(outsidePanels);
    }

    public boolean anyShared() {
        for (ExternalInventories.Panel panel : outsidePanels) {
            if (panel.processor() == null) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Processor openProcessor() {
        ExternalInventories.Panel panel = selectedPanel();
        return panel == null ? null : panel.processor();
    }

    public int processorIndex(Processor processor, int machineSlot) {
        return processorStart + processor.firstSlot() + machineSlot;
    }

    public int processorData(int i) {
        return processorData[i].get();
    }

    private int processorValue(int i) {
        ContainerData data = Processor.data(openMachine());
        return data != null && i < data.getCount() ? data.get(i) : 0;
    }

    @Nullable
    BlockEntity openMachine() {
        ExternalInventories.Panel panel = selectedPanel();
        if (panel == null || panel.processor() == null || player.level().isClientSide) {
            return null;
        }
        BlockEntity machine = externalOf(panel).processor(sideOf(panel));
        return Processor.of(machine) == panel.processor() ? machine : null;
    }

    public List<ExternalInventories.Panel> outsidePanels() {
        return outsidePanels;
    }

    private static int total(List<ExternalInventories.Panel> panels) {
        int n = 0;
        for (ExternalInventories.Panel p : panels) {
            n += p.slots();
        }
        return n;
    }

    public int selectedTab() {
        return openTab;
    }

    public int outsideScroll() {
        return outsideScroll;
    }

    @Nullable
    ExternalInventories.Panel selectedPanel() {
        return openTab >= 0 && openTab < outsidePanels.size() ? outsidePanels.get(openTab) : null;
    }

    static boolean handsOff(@Nullable ExternalInventories.Panel panel) {
        return panel != null && panel.handsOff() && Config.get().protectVaults();
    }

    public boolean handsOff(Slot slot) {
        return slot instanceof OutsideSlot && handsOff(selectedPanel());
    }

    public int outsideIndex(int i) {
        return outsideStart + i;
    }

    public ItemStack outsideItem(int i) {
        return slots.get(outsideStart + i).getItem();
    }

    public AutoMode autoMode(int which) {
        return AutoMode.of(auto[half(which)].get());
    }

    private int half(int which) {
        return which == 1 && second != null ? 1 : 0;
    }

    public ShareMode shareMode() {
        return ShareMode.of(share.get());
    }

    public boolean craftable(int which) {
        return craftable[half(which)].get() != 0;
    }

    public boolean showcase() {
        return showcase.get() != 0;
    }

    public int autocraft() {
        return autocraft.get();
    }

    public ItemStack archiveItem(int i) {
        return slots.get(ARCHIVE_START + i).getItem();
    }

    public int choice(int which) {
        return choice[half(which)].get();
    }

    public static int recipeButton(int i) {
        return BUTTON_RECIPE + i;
    }

    public static int recipeButton(int which, int i) {
        return (which == 1 ? BUTTON_RECIPE_SECOND : BUTTON_RECIPE) + i;
    }

    public static int autoButton(int which) {
        return which == 1 ? BUTTON_AUTO_SECOND : BUTTON_AUTO;
    }

    public static int balanceButton(int which) {
        return which == 1 ? BUTTON_BALANCE_SECOND : BUTTON_BALANCE;
    }

    public static int clearButton(int which) {
        return which == 1 ? BUTTON_CLEAR_SECOND : BUTTON_CLEAR;
    }

    public static int archiveButton(int i) {
        return BUTTON_ARCHIVE + i;
    }

    public boolean canRecord() {
        return canRecordRecipe() && !slots.get(resultIndex(active())).getItem().isEmpty();
    }

    public boolean canRecordRecipe() {
        return blankBlueprint() && missingCost() == 0;
    }

    public boolean blankBlueprint() {
        return BlueprintItem.blank(slots.get(blueprintIndex(active())).getItem());
    }

    public boolean recordRecipe(RecipeHolder<CraftingRecipe> holder, boolean fill) {
        ProjectBenchBlockEntity target = activeBench();
        if (!target.record(holder)) {
            return false;
        }
        Blueprint recorded = target.blueprint();
        List<Ingredient> grid = Blueprint.grid(holder.value());
        if (!fill || recorded == null || grid == null) {
            return true;
        }
        List<Slot> stock = stockSlots();
        int start = matrixStart(active());
        for (int i = 0; i < ProjectBenchBlockEntity.MATRIX; ++i) {
            Slot at = slots.get(start + i);
            Ingredient want = grid.get(i);
            ItemStack held = at.getItem();
            if (!held.isEmpty() && !want.test(held)) {
                ItemStack rest = held.copy();
                intoStorage(rest, active());
                if (!rest.isEmpty()) {
                    moveItemStackTo(rest, PLAYER_START, PLAYER_END, false);
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
            if (s.isEmpty() || !want.test(s) || !slot.mayPickup(player)) {
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        boolean server = !player.level().isClientSide;
        ProjectBenchBlockEntity target = activeBench();
        if (id == BUTTON_RECORD) {
            return !server || target.record();
        }
        if (id == BUTTON_BALANCE || id == BUTTON_CLEAR || (id == BUTTON_BALANCE_SECOND || id == BUTTON_CLEAR_SECOND) && second != null) {
            if (server) {
                ProjectBenchBlockEntity half = benchOf(id == BUTTON_BALANCE || id == BUTTON_CLEAR ? 0 : 1);
                if (id == BUTTON_BALANCE || id == BUTTON_BALANCE_SECOND) {
                    half.balance();
                } else {
                    half.clearMatrix(player);
                }
            }
            return true;
        }
        if (id == BUTTON_FILL || id == BUTTON_SHARE || id == BUTTON_SHOWCASE) {
            if (server) {
                switch (id) {
                    case BUTTON_FILL -> target.fillMatrix(player);
                    case BUTTON_SHARE -> bench.cycleShareMode();
                    default -> target.toggleShowcase();
                }
            }
            return true;
        }
        if (id == BUTTON_AUTO || id == BUTTON_AUTO_SECOND && second != null) {
            if (server) {
                benchOf(id == BUTTON_AUTO ? 0 : 1).cycleAutoMode();
            }
            return true;
        }
        if (id == BUTTON_AUTOCRAFT) {
            if (server) {
                bench.toggleAutocraft();
            }
            return true;
        }
        if (id == BUTTON_ACTIVE) {
            if (second == null) {
                return false;
            }
            if (server) {
                bench.toggleActive();
            }
            return true;
        }
        if (id >= BUTTON_RECIPE_SECOND && id < BUTTON_RECIPE_SECOND + MAX_RECIPES) {
            if (second == null) {
                return false;
            }
            if (server) {
                second.selectRecipe(id - BUTTON_RECIPE_SECOND);
            }
            return true;
        }
        if (id >= BUTTON_TAB && id < BUTTON_TAB + 16) {
            if (id - BUTTON_TAB < outsidePanels.size()) {
                openTab = id - BUTTON_TAB;
                outsideScroll = 0;
            }
            return true;
        }
        if (id >= BUTTON_SCROLL && id < BUTTON_SCROLL + MAX_SCROLL) {
            outsideScroll = id - BUTTON_SCROLL;
            return true;
        }
        if (id >= BUTTON_RECIPE && id < BUTTON_ARCHIVE) {
            if (server) {
                bench.selectRecipe(id - BUTTON_RECIPE);
            }
            return true;
        }
        int slot = id - BUTTON_ARCHIVE;
        if (slot < 0 || slot >= archiveSlots()) {
            return false;
        }
        return !server || (getCarried().isEmpty() ? swap(slot) : store(slot));
    }

    @Nullable
    private Container archiveOf(int slot) {
        Container archive = archives.partOf(slot);
        return archive.stillValid(player) ? archive : null;
    }

    private boolean store(int slot) {
        ItemStack carried = getCarried();
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
            setCarried(carried);
            return true;
        }
        if (carried.getCount() > max) {
            return false;
        }
        archive.setItem(slotInArchive, carried);
        setCarried(stored);
        return true;
    }

    private boolean intoCost(ItemStack stack) {
        int at = costIndex(active());
        return slots.get(at).mayPlace(stack) && moveItemStackTo(stack, at, at + 1, false);
    }

    private boolean intoBlueprintSlot(ItemStack stack) {
        int at = blueprintIndex(active());
        return !slots.get(at).hasItem() && moveItemStackTo(stack, at, at + 1, false);
    }

    private boolean intoStorage(ItemStack stack, int which) {
        boolean moved = benchOf(which).storage().insertLocked(stack);
        if (second != null) {
            moved |= benchOf(1 - which).storage().insertLocked(stack);
        }
        int start = storageStart(which);
        moved |= !stack.isEmpty() && moveItemStackTo(stack, start, start + ProjectBenchBlockEntity.STORAGE, false);
        if (second != null && !stack.isEmpty()) {
            int other = storageStart(1 - which);
            moved |= moveItemStackTo(stack, other, other + ProjectBenchBlockEntity.STORAGE, false);
        }
        return moved;
    }

    private boolean intoArchives(ItemStack stack) {
        if (!BlueprintItem.recorded(stack) || !archives.stillValid(player)) {
            return false;
        }
        int rest = Stacks.insert(archives, stack).getCount();
        if (rest == stack.getCount()) {
            return false;
        }
        stack.setCount(rest);
        return true;
    }

    private boolean swap(int slot) {
        Container archive = archiveOf(slot);
        int slotInArchive = slot % BlueprintArchiveBlockEntity.SLOTS;
        if (archive == null) {
            return false;
        }
        ItemStack stored = archive.getItem(slotInArchive);
        BlueprintContainer target = activeBench().blueprintSlot();
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot instanceof OutsideSlot outside) {
            return fromOutside(outside);
        }
        if (slot instanceof ProcessorSlot machineSlot) {
            return fromProcessor(machineSlot);
        }
        int half = halfOf(index);
        boolean fromCost = index >= costStart;
        if (!slot.hasItem() || half < 0 && !fromCost && (index < PLAYER_START || index >= PLAYER_END)) {
            return ItemStack.EMPTY;
        }
        int local = half < 0 ? -1 : index - base(half);
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (local == RESULT) {
            if (!fits(stack) || !moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (fromCost) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0 && local < STORAGE_START) {
            if (!intoStorage(stack, half) && !moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0 && local < BLUEPRINT) {
            boolean moved = intoMatrix(stack);
            moved |= !stack.isEmpty() && intoNeighbours(stack);
            if (!stack.isEmpty() && !moveItemStackTo(stack, PLAYER_START, PLAYER_END, false) && !moved) {
                return ItemStack.EMPTY;
            }
        } else if (half >= 0) {
            if (!(local == BLUEPRINT && intoArchives(stack)) && !moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
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
            boolean stored = !stack.isEmpty() && intoStorage(stack, active());
            stored |= !stack.isEmpty() && intoNeighbours(stack);
            if (!stack.isEmpty() && !stored) {
                boolean hotbar = index >= PLAYER_END - 9;
                if (!moveItemStackTo(stack, hotbar ? PLAYER_START : PLAYER_END - 9, hotbar ? PLAYER_END - 9 : PLAYER_END, false) && !moved) {
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
        if (player.level().isClientSide || !slot.isActive() || !slot.mayPickup(player)) {
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
        if (bench.shareMode() != ShareMode.NONE) {
            intoMatrix(taken);
            if (!taken.isEmpty()) {
                intoStorage(taken, active());
            }
        }
        if (!taken.isEmpty()) {
            moveItemStackTo(taken, PLAYER_START, PLAYER_END, true);
        }
        if (!taken.isEmpty()) {
            ItemStack rest = slot.putBack(taken);
            if (!rest.isEmpty()) {
                player.drop(rest, false);
            }
        }
        int moved = original.getCount() - taken.getCount();
        return moved > 0 ? original.copyWithCount(moved) : ItemStack.EMPTY;
    }

    private ItemStack fromProcessor(ProcessorSlot slot) {
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean result = slot.result();
        if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, result || slot.brewing)) {
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
        slot.onTake(player, stack);
        return original;
    }

    private boolean intoProcessor(ItemStack stack) {
        Processor processor = openProcessor();
        if (processor == null) {
            return false;
        }
        int target = processor.shiftTarget(stack, player.level());
        if (target < 0) {
            return false;
        }
        if (processor.brewing() && target == Processor.BOTTLES) {
            int first = processorIndex(processor, 0);
            return moveItemStackTo(stack, first, first + 3, false);
        }
        int at = processorIndex(processor, target);
        if (moveItemStackTo(stack, at, at + 1, false)) {
            return true;
        }
        int ingredient = processorIndex(processor, Processor.INGREDIENT);
        return processor.brewing() && target == Processor.BREWING_FUEL && moveItemStackTo(stack, ingredient, ingredient + 1, false);
    }

    private boolean intoNeighbours(ItemStack stack) {
        if (player.level().isClientSide) {
            return false;
        }
        return switch (bench.shareMode()) {
            case NONE -> false;
            case OPEN -> intoOpenTab(stack);
            case ALL -> intoEveryTab(stack);
        };
    }

    private boolean intoEveryTab(ItemStack stack) {
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); ++pass) {
            for (int i = 0, n = outsidePanels.size(); i < n && !stack.isEmpty(); ++i) {
                ExternalInventories.Panel panel = outsidePanels.get(i);
                IItemHandler inventory = playerTarget(panel);
                if (inventory != null && (pass == 1 || ExternalInventories.holds(inventory, stack))) {
                    stack.setCount(ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false).getCount());
                }
            }
        }
        return stack.getCount() != before;
    }

    private boolean intoOpenTab(ItemStack stack) {
        IItemHandler inventory = playerTarget(selectedPanel());
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

    private ExternalInventories externalOf(ExternalInventories.Panel panel) {
        return second != null && panel.sideIndex() >= ExternalInventories.Panel.SECOND ? second.external() : bench.external();
    }

    private static int sideOf(ExternalInventories.Panel panel) {
        return panel.sideIndex() % ExternalInventories.Panel.SECOND;
    }

    @Nullable
    IItemHandler handlerOf(ExternalInventories.Panel panel) {
        return externalOf(panel).handler(sideOf(panel));
    }

    @Nullable
    private IItemHandler playerTarget(@Nullable ExternalInventories.Panel panel) {
        return panel == null || handsOff(panel) ? null : handlerOf(panel);
    }

    private boolean intoMatrix(ItemStack stack) {
        boolean moved = ghosted(stack) && intoGhosts(stack) > 0;
        return !stack.isEmpty() && BenchStacks.topUp(activeBench().matrix(), stack) > 0 || moved;
    }

    private int intoGhosts(ItemStack stack) {
        ProjectBenchBlockEntity target = activeBench();
        Blueprint blueprint = target.blueprint();
        return blueprint == null ? 0 : GridFill.spread(target.matrix(), blueprint.pattern(), stack);
    }

    private boolean ghosted(ItemStack stack) {
        Blueprint blueprint = fillsGhosts();
        return blueprint != null && GridFill.acceptsAny(blueprint.pattern(), stack);
    }

    public boolean ghostWants(int matrixSlot, ItemStack stack) {
        Blueprint blueprint = fillsGhosts();
        return blueprint != null && GridFill.accepts(blueprint.ghost(matrixSlot), stack);
    }

    @Nullable
    private Blueprint fillsGhosts() {
        Blueprint blueprint = activeBench().blueprint();
        return blueprint != null && Config.get().shiftClickFillsGhosts() ? blueprint : null;
    }

    private boolean fits(ItemStack stack) {
        int room = 0;
        for (int i = PLAYER_START; i < PLAYER_END && room < stack.getCount(); ++i) {
            ItemStack present = slots.get(i).getItem();
            if (present.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(present, stack)) {
                room += present.getMaxStackSize() - present.getCount();
            }
        }
        return room >= stack.getCount();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !slot.isFake() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return near(player, bench) && (second == null || bench.partner() == second && near(player, second));
    }
}
