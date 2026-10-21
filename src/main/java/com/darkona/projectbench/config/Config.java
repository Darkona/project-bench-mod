package com.darkona.projectbench.config;

import dev.darkona.jdlib.api.world.RelativeSide;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Config {
    private static final List<String> ALL_SIDES = List.of("front", "back", "left", "right", "up", "down");

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue BENCH;
    private static final ModConfigSpec.BooleanValue BLUEPRINT;
    private static final ModConfigSpec.BooleanValue ARCHIVE;
    private static final ModConfigSpec.BooleanValue AUTOCRAFT;
    private static final ModConfigSpec.BooleanValue AUTOCRAFT_DEFAULT;
    private static final List<ModConfigSpec.EnumValue<SideMode>> AUTO_SIDES;
    private static final List<ModConfigSpec.EnumValue<SideMode>> BENCH_SIDES;
    private static final ModConfigSpec.BooleanValue KEEP_LAYOUT;
    private static final ModConfigSpec.EnumValue<AutoMode> AUTO_DEFAULT;
    private static final ModConfigSpec.BooleanValue REMAINDERS_TO_STORAGE;
    private static final ModConfigSpec.BooleanValue SHIFT_CLICK_FILLS_GHOSTS;
    private static final ModConfigSpec.BooleanValue TOOLTIP_INGREDIENTS;
    private static final ModConfigSpec.IntValue ARCHIVE_STACK;
    private static final ModConfigSpec.BooleanValue BENCH_PANEL;
    private static final ModConfigSpec.IntValue ARCHIVE_HEIGHT;
    private static final ModConfigSpec.BooleanValue CLEAR_MATRIX;
    private static final ModConfigSpec.BooleanValue AUTO_FILL;
    private static final ModConfigSpec.BooleanValue BALANCE_BUTTON;
    private static final ModConfigSpec.BooleanValue AUTO_BUTTON;
    private static final ModConfigSpec.BooleanValue EXTERNAL;
    private static final ModConfigSpec.EnumValue<ShareMode> SHARE_DEFAULT;
    private static final ModConfigSpec.IntValue EXTERNAL_SLOTS;
    private static final ModConfigSpec.BooleanValue PROTECT_VAULTS;
    private static final ModConfigSpec.BooleanValue PROCESSORS;
    private static final ModConfigSpec.BooleanValue PARTIAL_TABS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_SHOW;
    private static final ModConfigSpec.EnumValue<BreakMode> BENCH_ON_BREAK;
    private static final ModConfigSpec.EnumValue<BreakMode> ARCHIVE_ON_BREAK;
    private static final ModConfigSpec.BooleanValue SHOWCASE;
    private static final ModConfigSpec.BooleanValue SHOWCASE_DEFAULT;
    private static final ModConfigSpec.BooleanValue SLAB;
    private static final ModConfigSpec.BooleanValue SLAB_SHARE;
    private static final ModConfigSpec.BooleanValue SLOT_LOCKS;
    private static final ModConfigSpec.BooleanValue RECORD_COST;
    private static final ModConfigSpec.ConfigValue<String> RECORD_COST_ITEM;
    private static final ModConfigSpec.IntValue RECORD_COST_COUNT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ARCHIVE_INSERT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ARCHIVE_EXTRACT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("One switch per system. Turn one off and its blocks and items are gone from the game: no recipe, not in the creative tab",
                "or JEI, you cannot place or use them, and the ones already in a world stop working (no window, no hoppers).",
                "Nothing is deleted: turn it back on and it all works again.",
                "The blueprint and the archive go off with the bench. On a server, the server's file decides for its players.").push("enabled");
        BENCH = b.comment("The Project Bench itself. Off, the blueprint and the archive go off too.").define("bench", true);
        BLUEPRINT = b.comment("The whole blueprint thing: the blueprint item and its recipe, recording, the ghosts on the bench, the blueprint slot,",
                "the Record and Fill buttons, and the Blueprint Archive. Needs the bench.",
                "Off, the bench is just a crafting bench with its own inventory. Good if you lay recipes out with JEI or EMI.").define("blueprint", true);
        ARCHIVE = b.comment("The Blueprint Archive and its panel in the bench's window. Needs the bench and the blueprint.").define("archive", true);
        AUTOCRAFT = b.comment("The automatic benches: a pulse of redstone crafts once, the result goes to the chest below (or flies out the back",
                "with the ejecting one), and a hopper or pipe below can pull results out too. Off: no recipe, you cannot place them,",
                "and the ones already placed behave as plain benches. Needs the bench.").define("autocraft", true);
        b.pop();

        b.comment("The Project Bench.").push("bench");
        KEEP_LAYOUT = b.comment("When a craft would empty a grid slot, the bench takes the same item from the stack in its own inventory instead,",
                "so your recipe stays laid out and you can keep crafting. Off, the grid pays for every craft and empties",
                "like a crafting table does.").define("keepLayout", true);
        AUTO_DEFAULT = b.comment("What a bench you just placed does when a craft is about to use the last item of a grid slot.",
                "The A button in the window changes it for each bench. The values:",
                "OFF: takes nothing, the slot empties.",
                "ITEM: takes one of the very same item from the stock.",
                "TAGS: the same item first, then anything else the recipe accepts in that slot (planks of another wood, say).",
                "Needs keepLayout.").defineEnum("autoDefault", AutoMode.ITEM);
        REMAINDERS_TO_STORAGE = b.comment("Things a craft leaves behind (the empty bucket from a milk bucket) go to the bench's inventory first,",
                "and to you after that. Off, they go straight to you.").define("remaindersToStorage", true);
        b.comment("The buttons of the bench's window, one switch each. Turn one off and the button is gone, and so is what it did.",
                "Record goes with the blueprint switch, Share with external.enabled, Showcase with showcase.enabled.").push("buttons");
        BALANCE_BUTTON = b.comment("Balance: spreads each item evenly over the grid slots it is already in. Handy when you dumped nine iron",
                "in one slot and the recipe wants three of them.").define("balance", true);
        CLEAR_MATRIX = b.comment("Clear: sends everything in the grid back to the bench's own inventory.",
                "If it is full, the rest goes to the chests beside it that Share allows, and then to the inventory of the player who pressed it (hotbar and main slots only).").define("clear", true);
        AUTO_FILL = b.comment("Fill: with a recorded blueprint in the bench, lays the recipe out from the bench's inventory (and the chests beside it,",
                "the ones Share allows) with as many items as the recipe can use.").define("fill", true);
        AUTO_BUTTON = b.comment("The A, the button with three colours: it picks how a bench refills a grid slot a craft has just emptied.",
                "Turn it off and there is no button: every bench does what autoDefault says. Needs keepLayout.").define("auto", true);
        b.pop();
        b.comment("The chests and other inventories next to the bench: a chest, a barrel, a drawer, anything with an item handler,",
                "on one of the bench's four sides or below it. The blueprint archives are not these: they have their own panel.",
                "The colours of their windows and tabs are each player's, in projectbench-client.toml.").push("external");
        EXTERNAL = b.comment("The window shows the inventories next to the bench in a panel on its right, with a button to share them.",
                "Off, no panel, no button, no sharing.").define("enabled", true);
        SHARE_DEFAULT = b.comment("What a bench you just placed shares of the inventories next to it. Sharing means a craft that would empty a",
                "grid slot takes the item from them when the bench has none, Fill and Clear use them, and shift-click sends things",
                "there once the bench's own inventory is full. The window's Share button cycles it for each bench. The values:",
                "NONE: the bench keeps to itself, it only uses its own inventory.",
                "OPEN: only the inventory of the tab you have open. With nobody looking (an automatic bench crafting on its own)",
                "that means none.",
                "ALL: all of them. Shift-click picks the one that already has that item first, then the first with room, tab by tab.")
                .defineEnum("shareDefault", ShareMode.ALL);
        EXTERNAL_SLOTS = b.comment("The most slots of one inventory next to the bench that its tab shows, from 1 to 600.",
                "Sharing itself has no limit: this only trims the list on screen.")
                .defineInRange("panelSlots", 108, 1, 600);
        PROTECT_VAULTS = b.comment("Create's Item Vault shows its items in the window but cannot be taken from or put into by hand, as in Create.",
                "The bench still uses it as Share says: a craft's refill and Fill take from it, Clear and what a craft leaves behind may go into it.",
                "Off, it behaves like any other inventory beside the bench.")
                .define("protectVaults", true);
        PROCESSORS = b.comment("A furnace, blast furnace, smoker or brewing stand next to the bench gets a tab that works like its own window:",
                "put in what to smelt or brew and the fuel, watch the flame and the arrow, take the result (and the furnace's experience).",
                "The bench never takes from them nor puts into them by itself: crafting, Fill, Clear and Share leave them alone.",
                "Off, they are treated like any other block that shows different slots on different sides (see partialTabs).")
                .define("processors", true);
        PARTIAL_TABS = b.comment("A machine from another mod that shows different slots on different sides (input on top, output below...)",
                "gets no tab and the bench does not use it. Turn this on to give it a tab anyway, with only the slots of the side",
                "that touches the bench, and let the bench share it like a chest. Chests, barrels, drawers and other plain storage",
                "look the same from every side and always get their tab.")
                .define("partialTabs", false);
        ALWAYS_SHOW = b.comment("Blocks that always get a tab beside the bench, even when they show a different inventory through each side,",
                "as if partialTabs were on for them alone: the tab has the slots of the side that touches the bench, and the bench shares it.",
                "Block ids; one from a mod that is not installed does nothing. Storage Drawers' Drawer Controller is listed.")
                .defineListAllowEmpty("alwaysShow", List.of("storagedrawers:controller", "storagedrawers:controller_io",
                        "storagedrawers:framed_controller", "storagedrawers:framed_controller_io"), () -> "storagedrawers:controller", Config::isItemId);
        b.pop();
        BENCH_ON_BREAK = b.comment("What happens to what a bench holds (the grid, the inventory, the blueprint slot and the cost slot) when you break it.",
                "DROP_ALL: it all falls on the ground.",
                "SILK_TOUCH: a pickaxe with Silk Touch keeps it all inside the dropped bench, any other tool drops it.",
                "ALWAYS_KEEP: you keep it inside the item whenever you break it with the right tool.",
                "Pistons, explosions, commands, creative mode and a break without the right tool always drop it.",
                "Place the bench again and the items are back.")
                .defineEnum("onBreak", BreakMode.SILK_TOUCH);
        b.comment("The grid's items drawn on top of the bench, and what they make floating over them.").push("showcase");
        SHOWCASE = b.comment("The window's button that draws the grid's items on the bench. Off, no button and nothing drawn.").define("enabled", true);
        SHOWCASE_DEFAULT = b.comment("A bench you just placed starts with the items drawn. The window's button changes it for each bench.")
                .define("onByDefault", false);
        b.pop();
        b.comment("Locking a slot of the bench's own inventory to one item.").push("slotLocks");
        SLOT_LOCKS = b.comment("A middle click on a slot of the bench's inventory that holds something locks it to that item: from then on only",
                "that very item goes in there, whoever puts it (you, shift-click, hoppers, pipes, Clear, what a craft leaves behind),",
                "and that item goes to its locked slots first. The slot gets an orange edge and a little padlock, and shows the item",
                "faded when it is empty. A middle click on a locked slot unlocks it, empty or not. Off, no locking and no drawing,",
                "and the locks benches already have do nothing (they are kept, and work again when you turn this back on).")
                .define("enabled", true);
        b.pop();
        b.comment("The Project Bench Slab and the double bench. The colours of its window are each player's, in projectbench-client.toml.").push("slab");
        SLAB = b.comment("The Project Bench Slab: half a bench, made from a bench and three iron ingots, that joins the bench beside it.",
                "A slab next to a slab or a bench (never an automatic one), side by side and facing the same way, makes one double bench: a wider window",
                "with both matrices, and both internal inventories and the inventories beside them as one stock. Two benches never join.",
                "Off, there is no slab (no recipe, hidden, cannot be placed or used, the ones placed do nothing) and every bench",
                "works alone. Needs the bench.").define("enabled", true);
        SLAB_SHARE = b.comment("The two inventories of a double bench count as one stock: a craft on either grid refills from both",
                "(its own first), and Fill, Clear and the crafting remainders use both. With Share on ALL (one button for the pair), the",
                "inventories beside both halves count too, and a double chest touching both counts once. Off, each half only uses",
                "its own inventory and the inventories beside it, as a bench on its own.")
                .define("shareStorage", true);
        b.pop();
        b.comment("What hoppers, pipes and inserters from other mods reach on each side of the bench. The sides turn with the bench:",
                "front is the side that faces you when you place it, and left and right are yours as you look at that front.",
                "Up and down are up and down. The values:",
                "INVENTORY: puts items into the bench's own inventory, and takes them out of it first and then out of the grid.",
                "GRID: puts items into the grid in order (first the slots whose blueprint ghost wants the item, the emptiest first,",
                "then the slots that already hold it until the stack is full, and then the first empty slot, but only with no recorded",
                "blueprint in the bench), and takes them out of the grid first and then out of the inventory.",
                "BLUEPRINT: only the blueprint slot, to put a blueprint in or take it out. With blueprints switched off it works as INVENTORY.",
                "NONE: closed, nothing goes in or out.",
                "RESULT only means something on an automatic bench ([bench.autocraft.sides]): here it is NONE.",
                "The result is never reachable. A double bench has these sides on each half, except the one where the halves touch.")
                .push("automation");
        BENCH_SIDES = defineSides(b, "The front, the side with the bench's face.", SideMode.BLUEPRINT, SideMode.GRID, SideMode.INVENTORY,
                SideMode.INVENTORY, SideMode.GRID, SideMode.INVENTORY);
        b.pop();
        b.comment("The automatic benches (the Auto Project Bench and the ejecting one). They are whole benches: a slab never crafts on its own.",
                "A pulse of redstone crafts once: the result goes into the inventory below the bench (into the one behind it, or out",
                "the back onto the ground, for the ejecting one). With no inventory there, or a full one, the pulse does nothing.",
                "What a craft leaves behind (the empty bucket) goes to the bench's inventory, and to the same place as the result",
                "when that is full. A comparator reads how full the bench's inventory is.").push("autocraft");
        AUTOCRAFT_DEFAULT = b.comment("An automatic bench you just placed starts with its button on (in the bar above the window).",
                "The button turns the automation of that bench on and off: off, it is a plain bench that only crafts by hand.")
                .define("onByDefault", true);
        b.comment("What hoppers and pipes reach on each side of an automatic bench, instead of [bench.automation]. Same values,",
                "and one more: RESULT, nothing goes in and taking out crafts (a hopper below pulls one craft at a time; what it",
                "cannot take of a craft waits in the bench's inventory and comes out first next time).",
                "With the bench's button off a RESULT side is closed; with autocraft switched off the bench uses [bench.automation].")
                .push("sides");
        AUTO_SIDES = defineSides(b, "The front.", SideMode.BLUEPRINT, SideMode.INVENTORY, SideMode.INVENTORY, SideMode.INVENTORY,
                SideMode.GRID, SideMode.RESULT);
        b.pop();
        b.pop();
        b.pop();

        b.comment("Blueprints.").push("blueprint");
        SHIFT_CLICK_FILLS_GHOSTS = b.comment("With a recorded blueprint in the bench, shift-click sends the items the ghosts want onto the grid.",
                "Off, shift-click moves items as if there were no blueprint. The ghosts are still drawn.").define("shiftClickFillsGhosts", true);
        TOOLTIP_INGREDIENTS = b.comment("A recorded blueprint's tooltip lists the ingredients of its recipe under what it makes.",
                "Off, it only says what it makes.")
                .define("tooltipIngredients", true);
        b.comment("What recording a blueprint costs.").push("recordCost");
        RECORD_COST = b.comment("On, recording a blueprint (the Record button, and JEI's or EMI's transfer onto a blank blueprint) spends",
                "some of an item, and the bench's window gets a slot for it right above the blueprint slot (one per half on a",
                "double bench). Only that item goes in there, by hand, shift-click or a hopper on the blueprint side.",
                "Without enough of it, Record stays off and the viewers move the recipe as if there were no blank blueprint.",
                "Off, recording is free and the slot is gone (whatever was in it stays there, and falls out when you break the bench).")
                .define("enabled", true);
        RECORD_COST_ITEM = b.comment("The item it costs, by id. Any item works; one that does not exist makes recording free.")
                .define("item", "minecraft:white_dye", Config::isItemId);
        RECORD_COST_COUNT = b.comment("How many of it each recording spends, from 1 to 64.").defineInRange("count", 1, 1, 64);
        b.pop();
        b.pop();

        b.comment("The Blueprint Archive.").push("archive");
        ARCHIVE_STACK = b.comment("How many blank blueprints fit in one archive slot, from 1 to 64. A recorded blueprint is always one to a slot.")
                .defineInRange("stackSize", 64, 1, 64);
        ARCHIVE_HEIGHT = b.comment("Archives stack up this high and share their blueprints: a column of 1 to this many archives is one inventory.",
                "Only one over another, not side by side. A bench touching any archive of a column lists the whole column.",
                "One more archive on a full column starts the next column. From 1 to 6.").defineInRange("columnHeight", 3, 1, 6);
        BENCH_PANEL = b.comment("A bench lists the blueprints of the archives on its six sides (every block of each column) in a panel of its window.",
                "Off, no panel: the archive is just a shelf of blueprints with its own window.").define("benchPanel", true);
        ARCHIVE_ON_BREAK = b.comment("What happens to the blueprints when you break an archive. Same values as the bench's onBreak:",
                "DROP_ALL, SILK_TOUCH (an axe with Silk Touch keeps them inside the dropped archive) or ALWAYS_KEEP.")
                .defineEnum("onBreak", BreakMode.SILK_TOUCH);
        b.comment("What hoppers, pipes and inserters from other mods can do with the archive's 36 slots, by side. Only blueprints go in.",
                "The sides turn with the archive, as the bench's: front, back, left, right (yours, as you look at the front), up, down.")
                .push("automation");
        ARCHIVE_INSERT = b.comment("The sides that can put blueprints in. An empty list means none.")
                .defineListAllowEmpty("insertSides", ALL_SIDES, () -> "front", Config::isSide);
        ARCHIVE_EXTRACT = b.comment("The sides that can take blueprints out. Only the bottom by default.")
                .defineListAllowEmpty("extractSides", List.of("down"), () -> "down", Config::isSide);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private static List<ModConfigSpec.EnumValue<SideMode>> defineSides(ModConfigSpec.Builder b, String front, SideMode... defaults) {
        String[] comments = {front, "The back.", "The left, as you look at the front.", "The right, as you look at the front.", "The top.", "The bottom."};
        List<ModConfigSpec.EnumValue<SideMode>> values = new ArrayList<>(defaults.length);
        for (RelativeSide side : RelativeSide.values()) {
            values.add(b.comment(comments[side.ordinal()]).defineEnum(side.key(), defaults[side.ordinal()]));
        }
        return List.copyOf(values);
    }

    private static int packSides(List<ModConfigSpec.EnumValue<SideMode>> values, boolean defaults) {
        int bits = 0;
        for (int i = 0; i < values.size(); ++i) {
            bits |= value(values.get(i), defaults).ordinal() << 3 * i;
        }
        return bits;
    }

    private static <T> T value(ModConfigSpec.ConfigValue<T> config, boolean defaults) {
        return defaults ? config.getDefault() : config.get();
    }

    private static Settings read(boolean defaults) {
        return new Settings.Builder().bench(value(BENCH, defaults)).blueprint(value(BLUEPRINT, defaults)).archive(value(ARCHIVE, defaults))
                .keepLayout(value(KEEP_LAYOUT, defaults)).remaindersToStorage(value(REMAINDERS_TO_STORAGE, defaults))
                .benchSides(packSides(BENCH_SIDES, defaults)).shiftClickFillsGhosts(value(SHIFT_CLICK_FILLS_GHOSTS, defaults))
                .tooltipIngredients(value(TOOLTIP_INGREDIENTS, defaults)).archiveStack(value(ARCHIVE_STACK, defaults))
                .benchPanel(value(BENCH_PANEL, defaults)).archiveInsert(sides(value(ARCHIVE_INSERT, defaults)))
                .archiveExtract(sides(value(ARCHIVE_EXTRACT, defaults))).archiveHeight(value(ARCHIVE_HEIGHT, defaults))
                .clearMatrix(value(CLEAR_MATRIX, defaults)).autoFill(value(AUTO_FILL, defaults)).external(value(EXTERNAL, defaults))
                .shareMode(value(SHARE_DEFAULT, defaults)).externalSlots(value(EXTERNAL_SLOTS, defaults))
                .showcase(value(SHOWCASE, defaults)).showcaseDefault(value(SHOWCASE_DEFAULT, defaults))
                .autoDefault(value(AUTO_DEFAULT, defaults)).benchOnBreak(value(BENCH_ON_BREAK, defaults))
                .archiveOnBreak(value(ARCHIVE_ON_BREAK, defaults)).slab(value(SLAB, defaults)).slabShare(value(SLAB_SHARE, defaults))
                .balanceButton(value(BALANCE_BUTTON, defaults)).autoButton(value(AUTO_BUTTON, defaults))
                .autocraft(value(AUTOCRAFT, defaults)).autocraftDefault(value(AUTOCRAFT_DEFAULT, defaults))
                .autoSides(packSides(AUTO_SIDES, defaults)).slotLocks(value(SLOT_LOCKS, defaults)).recordCost(value(RECORD_COST, defaults))
                .recordCostItem(ResourceLocation.parse(value(RECORD_COST_ITEM, defaults))).recordCostCount(value(RECORD_COST_COUNT, defaults))
                .protectVaults(value(PROTECT_VAULTS, defaults)).processors(value(PROCESSORS, defaults))
                .partialTabs(value(PARTIAL_TABS, defaults)).build();
    }

    public record Settings(boolean bench, boolean blueprint, boolean archive,
                           boolean keepLayout, boolean remaindersToStorage, int benchSides,
                           boolean shiftClickFillsGhosts, boolean tooltipIngredients,
                           int archiveStack, boolean benchPanel, int archiveInsert, int archiveExtract,
                           int archiveHeight, boolean clearMatrix, boolean autoFill, boolean external, ShareMode shareMode,
                           int externalSlots, boolean showcase, boolean showcaseDefault, AutoMode autoDefault,
                           BreakMode benchOnBreak, BreakMode archiveOnBreak, boolean slab, boolean slabShare, boolean balanceButton, boolean autoButton,
                           boolean autocraft, boolean autocraftDefault, int autoSides, boolean slotLocks,
                           boolean recordCost, ResourceLocation recordCostItem, int recordCostCount, boolean protectVaults,
                           boolean processors, boolean partialTabs) {
        static final List<Predicate<Settings>> FLAGS = Arrays.asList(Settings::bench, Settings::blueprint, Settings::archive,
                Settings::keepLayout, Settings::remaindersToStorage, Settings::shiftClickFillsGhosts, Settings::tooltipIngredients,
                Settings::benchPanel, Settings::clearMatrix, Settings::autoFill, Settings::external, null, Settings::showcase,
                Settings::showcaseDefault, null, Settings::slab, Settings::slabShare, Settings::balanceButton, Settings::autoButton,
                Settings::autocraft, Settings::autocraftDefault, Settings::slotLocks, Settings::recordCost, Settings::protectVaults,
                Settings::processors, Settings::partialTabs);

        public static final StreamCodec<ByteBuf, Settings> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            int flags = 0;
            for (int bit = 0; bit < FLAGS.size(); ++bit) {
                Predicate<Settings> flag = FLAGS.get(bit);
                if (flag != null && flag.test(s)) {
                    flags |= 1 << bit;
                }
            }
            buf.writeInt(flags);
            buf.writeInt(s.benchSides);
            buf.writeInt(s.autoSides);
            buf.writeByte(s.archiveStack);
            buf.writeByte(s.archiveInsert);
            buf.writeByte(s.archiveExtract);
            buf.writeByte(s.archiveHeight);
            buf.writeShort(s.externalSlots);
            buf.writeByte(s.autoDefault.ordinal());
            buf.writeByte(s.shareMode.ordinal());
            buf.writeByte(s.benchOnBreak.ordinal());
            buf.writeByte(s.archiveOnBreak.ordinal());
            ResourceLocation.STREAM_CODEC.encode(buf, s.recordCostItem);
            buf.writeByte(s.recordCostCount);
        }, buf -> {
            int flags = buf.readInt();
            boolean[] on = new boolean[FLAGS.size()];
            for (int bit = 0; bit < on.length; ++bit) {
                on[bit] = (flags & 1 << bit) != 0;
            }
            return new Builder().bench(on[0]).blueprint(on[1]).archive(on[2]).keepLayout(on[3]).remaindersToStorage(on[4])
                    .shiftClickFillsGhosts(on[5]).tooltipIngredients(on[6]).benchPanel(on[7]).clearMatrix(on[8]).autoFill(on[9])
                    .external(on[10]).showcase(on[12]).showcaseDefault(on[13]).slab(on[15]).slabShare(on[16]).balanceButton(on[17])
                    .autoButton(on[18]).autocraft(on[19]).autocraftDefault(on[20]).slotLocks(on[21]).recordCost(on[22])
                    .protectVaults(on[23]).processors(on[24]).partialTabs(on[25])
                    .benchSides(buf.readInt()).autoSides(buf.readInt()).archiveStack(buf.readByte()).archiveInsert(buf.readByte())
                    .archiveExtract(buf.readByte()).archiveHeight(buf.readByte()).externalSlots(buf.readShort())
                    .autoDefault(AutoMode.of(buf.readByte())).shareMode(ShareMode.of(buf.readByte()))
                    .benchOnBreak(BreakMode.of(buf.readByte())).archiveOnBreak(BreakMode.of(buf.readByte()))
                    .recordCostItem(ResourceLocation.STREAM_CODEC.decode(buf)).recordCostCount(buf.readByte()).build();
        });

        public static Builder builder() {
            return Config.DEFAULTS.toBuilder();
        }

        public Builder toBuilder() {
            return new Builder().bench(bench).blueprint(blueprint).archive(archive).keepLayout(keepLayout)
                    .remaindersToStorage(remaindersToStorage).benchSides(benchSides).shiftClickFillsGhosts(shiftClickFillsGhosts)
                    .tooltipIngredients(tooltipIngredients).archiveStack(archiveStack).benchPanel(benchPanel).archiveInsert(archiveInsert)
                    .archiveExtract(archiveExtract).archiveHeight(archiveHeight).clearMatrix(clearMatrix).autoFill(autoFill)
                    .external(external).shareMode(shareMode).externalSlots(externalSlots).showcase(showcase)
                    .showcaseDefault(showcaseDefault).autoDefault(autoDefault).benchOnBreak(benchOnBreak).archiveOnBreak(archiveOnBreak)
                    .slab(slab).slabShare(slabShare).balanceButton(balanceButton).autoButton(autoButton).autocraft(autocraft)
                    .autocraftDefault(autocraftDefault).autoSides(autoSides).slotLocks(slotLocks).recordCost(recordCost)
                    .recordCostItem(recordCostItem).recordCostCount(recordCostCount).protectVaults(protectVaults).processors(processors)
                    .partialTabs(partialTabs);
        }

        public static final class Builder {
            private boolean bench;
            private boolean blueprint;
            private boolean archive;
            private boolean keepLayout;
            private boolean remaindersToStorage;
            private int benchSides;
            private boolean shiftClickFillsGhosts;
            private boolean tooltipIngredients;
            private int archiveStack;
            private boolean benchPanel;
            private int archiveInsert;
            private int archiveExtract;
            private int archiveHeight;
            private boolean clearMatrix;
            private boolean autoFill;
            private boolean external;
            @Nullable
            private ShareMode shareMode;
            private int externalSlots;
            private boolean showcase;
            private boolean showcaseDefault;
            @Nullable
            private AutoMode autoDefault;
            @Nullable
            private BreakMode benchOnBreak;
            @Nullable
            private BreakMode archiveOnBreak;
            private boolean slab;
            private boolean slabShare;
            private boolean balanceButton;
            private boolean autoButton;
            private boolean autocraft;
            private boolean autocraftDefault;
            private int autoSides;
            private boolean slotLocks;
            private boolean recordCost;
            @Nullable
            private ResourceLocation recordCostItem;
            private int recordCostCount;
            private boolean protectVaults;
            private boolean processors;
            private boolean partialTabs;

            private Builder() {
            }

            public Builder bench(boolean bench) {
                this.bench = bench;
                return this;
            }

            public Builder blueprint(boolean blueprint) {
                this.blueprint = blueprint;
                return this;
            }

            public Builder archive(boolean archive) {
                this.archive = archive;
                return this;
            }

            public Builder keepLayout(boolean keepLayout) {
                this.keepLayout = keepLayout;
                return this;
            }

            public Builder remaindersToStorage(boolean remaindersToStorage) {
                this.remaindersToStorage = remaindersToStorage;
                return this;
            }

            public Builder benchSides(int benchSides) {
                this.benchSides = benchSides;
                return this;
            }

            public Builder shiftClickFillsGhosts(boolean shiftClickFillsGhosts) {
                this.shiftClickFillsGhosts = shiftClickFillsGhosts;
                return this;
            }

            public Builder tooltipIngredients(boolean tooltipIngredients) {
                this.tooltipIngredients = tooltipIngredients;
                return this;
            }

            public Builder archiveStack(int archiveStack) {
                this.archiveStack = archiveStack;
                return this;
            }

            public Builder benchPanel(boolean benchPanel) {
                this.benchPanel = benchPanel;
                return this;
            }

            public Builder archiveInsert(int archiveInsert) {
                this.archiveInsert = archiveInsert;
                return this;
            }

            public Builder archiveExtract(int archiveExtract) {
                this.archiveExtract = archiveExtract;
                return this;
            }

            public Builder archiveHeight(int archiveHeight) {
                this.archiveHeight = archiveHeight;
                return this;
            }

            public Builder clearMatrix(boolean clearMatrix) {
                this.clearMatrix = clearMatrix;
                return this;
            }

            public Builder autoFill(boolean autoFill) {
                this.autoFill = autoFill;
                return this;
            }

            public Builder external(boolean external) {
                this.external = external;
                return this;
            }

            public Builder shareMode(ShareMode shareMode) {
                this.shareMode = shareMode;
                return this;
            }

            public Builder externalSlots(int externalSlots) {
                this.externalSlots = externalSlots;
                return this;
            }

            public Builder showcase(boolean showcase) {
                this.showcase = showcase;
                return this;
            }

            public Builder showcaseDefault(boolean showcaseDefault) {
                this.showcaseDefault = showcaseDefault;
                return this;
            }

            public Builder autoDefault(AutoMode autoDefault) {
                this.autoDefault = autoDefault;
                return this;
            }

            public Builder benchOnBreak(BreakMode benchOnBreak) {
                this.benchOnBreak = benchOnBreak;
                return this;
            }

            public Builder archiveOnBreak(BreakMode archiveOnBreak) {
                this.archiveOnBreak = archiveOnBreak;
                return this;
            }

            public Builder slab(boolean slab) {
                this.slab = slab;
                return this;
            }

            public Builder slabShare(boolean slabShare) {
                this.slabShare = slabShare;
                return this;
            }

            public Builder balanceButton(boolean balanceButton) {
                this.balanceButton = balanceButton;
                return this;
            }

            public Builder autoButton(boolean autoButton) {
                this.autoButton = autoButton;
                return this;
            }

            public Builder autocraft(boolean autocraft) {
                this.autocraft = autocraft;
                return this;
            }

            public Builder autocraftDefault(boolean autocraftDefault) {
                this.autocraftDefault = autocraftDefault;
                return this;
            }

            public Builder autoSides(int autoSides) {
                this.autoSides = autoSides;
                return this;
            }

            public Builder slotLocks(boolean slotLocks) {
                this.slotLocks = slotLocks;
                return this;
            }

            public Builder recordCost(boolean recordCost) {
                this.recordCost = recordCost;
                return this;
            }

            public Builder recordCostItem(ResourceLocation recordCostItem) {
                this.recordCostItem = recordCostItem;
                return this;
            }

            public Builder recordCostCount(int recordCostCount) {
                this.recordCostCount = recordCostCount;
                return this;
            }

            public Builder protectVaults(boolean protectVaults) {
                this.protectVaults = protectVaults;
                return this;
            }

            public Builder processors(boolean processors) {
                this.processors = processors;
                return this;
            }

            public Builder partialTabs(boolean partialTabs) {
                this.partialTabs = partialTabs;
                return this;
            }

            public Settings build() {
                return new Settings(bench, blueprint, archive, keepLayout, remaindersToStorage, benchSides, shiftClickFillsGhosts,
                        tooltipIngredients, archiveStack, benchPanel, archiveInsert, archiveExtract, archiveHeight, clearMatrix, autoFill,
                        external, Objects.requireNonNull(shareMode), externalSlots, showcase, showcaseDefault,
                        Objects.requireNonNull(autoDefault), Objects.requireNonNull(benchOnBreak), Objects.requireNonNull(archiveOnBreak),
                        slab, slabShare, balanceButton, autoButton, autocraft, autocraftDefault, autoSides, slotLocks, recordCost,
                        Objects.requireNonNull(recordCostItem), recordCostCount, protectVaults, processors, partialTabs);
            }
        }

        public static int packSides(SideMode... modes) {
            int bits = 0;
            for (int i = 0; i < modes.length && i < 6; ++i) {
                bits |= modes[i].ordinal() << 3 * i;
            }
            return bits;
        }

        public SideMode benchSide(RelativeSide side) {
            return SideMode.of(benchSides >> 3 * side.ordinal() & 7);
        }

        public SideMode autoSide(RelativeSide side) {
            return SideMode.of(autoSides >> 3 * side.ordinal() & 7);
        }

        public boolean archiveInserts(@Nullable RelativeSide side) {
            return allows(archiveInsert, side);
        }

        public boolean archiveExtracts(@Nullable RelativeSide side) {
            return allows(archiveExtract, side);
        }

        private static boolean allows(int sides, @Nullable RelativeSide side) {
            return side == null ? sides != 0 : (sides & 1 << side.ordinal()) != 0;
        }
    }

    public static final Settings DEFAULTS = read(true);

    private static volatile Settings current = DEFAULTS;

    private static volatile Set<Block> alwaysShow = Set.of();
    @Nullable
    private static volatile Set<Block> alwaysShowOverride;

    private static volatile boolean fromServer;
    @Nullable
    private static volatile Settings override;

    private Config() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((ModConfigEvent.Loading e) -> reload(e.getConfig().getSpec()));
        modBus.addListener((ModConfigEvent.Reloading e) -> reload(e.getConfig().getSpec()));
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> load());
    }

    private static void reload(Object spec) {
        if (spec != SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            load();
            return;
        }
        server.execute(() -> {
            resolveAlwaysShow();
            ConfigSync.reload(fromFile(), server.getPlayerList().getPlayers());
        });
    }

    public static Settings get() {
        Settings o = override;
        return o != null ? o : current;
    }

    public static Item costItem() {
        return costItem(get());
    }

    public static Item costItem(Settings s) {
        Cost c = cost;
        if (c == null || c.settings() != s) {
            c = new Cost(s, s.blueprint() && s.recordCost() ? BuiltInRegistries.ITEM.get(s.recordCostItem()) : Items.AIR);
            cost = c;
        }
        return c.item();
    }

    private record Cost(Settings settings, Item item) {
    }

    @Nullable
    private static volatile Cost cost;

    public static Settings fromFile() {
        return SPEC.isLoaded() ? read(false) : DEFAULTS;
    }

    public static void load() {
        resolveAlwaysShow();
        adopt(fromFile(), ServerLifecycleHooks.getCurrentServer() != null);
    }

    private static void resolveAlwaysShow() {
        alwaysShow = SPEC.isLoaded() ? blocks(ALWAYS_SHOW.get()) : Set.of();
    }

    static boolean adopt(Settings file, boolean serverHere) {
        if (fromServer && !serverHere) {
            return false;
        }
        Settings before = get();
        current = file;
        Feature.changed();
        return !before.equals(get());
    }

    public static void receive(Settings settings) {
        fromServer = true;
        current = settings;
        Feature.changed();
    }

    public static void forgetServer() {
        fromServer = false;
        load();
    }

    public static void override(Settings settings) {
        override = settings;
        Feature.changed();
    }

    public static void clearOverride() {
        override = null;
        Feature.changed();
    }

    public static Set<Block> alwaysShow() {
        Set<Block> o = alwaysShowOverride;
        return o != null ? o : alwaysShow;
    }

    public static void overrideAlwaysShow(List<String> ids) {
        alwaysShowOverride = blocks(ids);
    }

    public static void clearAlwaysShow() {
        alwaysShowOverride = null;
    }

    static Set<Block> blocks(List<? extends String> ids) {
        return resolve(ids, BuiltInRegistries.BLOCK::getOptional);
    }

    static <T> Set<T> resolve(List<? extends String> ids, Function<ResourceLocation, Optional<T>> lookup) {
        Set<T> found = new LinkedHashSet<>();
        for (String id : ids) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null) {
                lookup.apply(key).ifPresent(found::add);
            }
        }
        return Set.copyOf(found);
    }

    static int sides(List<? extends String> names) {
        int bits = 0;
        for (String name : names) {
            RelativeSide side = RelativeSide.byKey(name);
            if (side != null) {
                bits |= 1 << side.ordinal();
            }
        }
        return bits;
    }

    private static boolean isItemId(Object o) {
        return o instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static boolean isSide(Object o) {
        return o instanceof String s && RelativeSide.byKey(s) != null;
    }
}
