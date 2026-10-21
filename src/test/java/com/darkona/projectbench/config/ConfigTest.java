package com.darkona.projectbench.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import dev.darkona.jdlib.api.world.RelativeSide;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void alwaysShowKeepsOnlyKnownIds() {
        Map<ResourceLocation, String> known = Map.of(ResourceLocation.parse("storagedrawers:controller"), "controller",
                ResourceLocation.parse("minecraft:chest"), "chest");
        assertEquals(Set.of("controller", "chest"), Config.resolve(List.of("storagedrawers:controller", "nosuchmod:thing", "Not An Id!", "chest"),
                id -> Optional.ofNullable(known.get(id))), "a mod that is not installed and a bad id are left out; no namespace means minecraft");
        assertEquals(Set.of(), Config.resolve(List.of(), id -> Optional.ofNullable(known.get(id))));
    }

    @Test
    void theServersCopyWinsUntilTheClientLeaves() {
        Config.Settings file = Config.get();
        Config.Settings edited = file.toBuilder().slotLocks(!file.slotLocks()).build();
        try {
            Config.SYNC.receive(Config.SYNC.snapshot());
            assertTrue(Config.CONFIG.fromServer());
            assertFalse(Config.CONFIG.adopt(edited), "connected to another machine's server: its copy is kept");
            assertEquals(file, Config.get());
        } finally {
            Config.SYNC.forgetServer();
        }
        assertFalse(Config.CONFIG.fromServer());
        assertEquals(file, Config.get(), "leaving the server goes back to the file");
        assertTrue(Config.CONFIG.adopt(edited), "with no server's copy the file is taken");
        assertFalse(Config.CONFIG.adopt(edited), "the same Settings again: nothing changed, nothing to send");
        Config.CONFIG.adopt(file);
    }

    @Test
    void sidesAreReadByName() {
        assertEquals(0, Config.sides(List.of()));
        assertEquals(1 << RelativeSide.DOWN.ordinal() | 1 << RelativeSide.LEFT.ordinal(), Config.sides(List.of("down", "Left", "north")),
                "relative names only: north is not one");
        assertEquals(0b111111, Config.sides(List.of("front", "back", "left", "right", "up", "down")));
    }

    @Test
    void sidesTurnWithTheFront() {
        assertEquals(RelativeSide.FRONT, RelativeSide.of(Direction.NORTH, Direction.NORTH));
        assertEquals(RelativeSide.BACK, RelativeSide.of(Direction.NORTH, Direction.SOUTH));
        assertEquals(RelativeSide.LEFT, RelativeSide.of(Direction.NORTH, Direction.EAST), "facing north, your left is east");
        assertEquals(RelativeSide.RIGHT, RelativeSide.of(Direction.NORTH, Direction.WEST));
        assertEquals(RelativeSide.FRONT, RelativeSide.of(Direction.EAST, Direction.EAST));
        assertEquals(RelativeSide.LEFT, RelativeSide.of(Direction.EAST, Direction.SOUTH), "facing east, your left is south");
        assertEquals(RelativeSide.BACK, RelativeSide.of(Direction.WEST, Direction.EAST));
        assertEquals(RelativeSide.RIGHT, RelativeSide.of(Direction.SOUTH, Direction.EAST));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            assertEquals(RelativeSide.UP, RelativeSide.of(facing, Direction.UP), "up does not turn");
            assertEquals(RelativeSide.DOWN, RelativeSide.of(facing, Direction.DOWN), "nor does down");
        }
    }

    @Test
    void defaultSides() {
        Config.Settings d = Config.DEFAULTS;
        assertEquals(SideMode.BLUEPRINT, d.benchSide(RelativeSide.FRONT));
        assertEquals(SideMode.GRID, d.benchSide(RelativeSide.BACK));
        assertEquals(SideMode.GRID, d.benchSide(RelativeSide.UP));
        assertEquals(SideMode.INVENTORY, d.benchSide(RelativeSide.LEFT));
        assertEquals(SideMode.INVENTORY, d.benchSide(RelativeSide.RIGHT));
        assertEquals(SideMode.INVENTORY, d.benchSide(RelativeSide.DOWN));
        assertEquals(SideMode.BLUEPRINT, d.autoSide(RelativeSide.FRONT), "the automatic bench: the blueprint in front");
        assertEquals(SideMode.GRID, d.autoSide(RelativeSide.UP), "the matrix on top");
        assertEquals(SideMode.RESULT, d.autoSide(RelativeSide.DOWN), "the result below");
        for (RelativeSide side : new RelativeSide[]{RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT}) {
            assertEquals(SideMode.INVENTORY, d.autoSide(side), "the inventory on the " + side);
        }
        assertTrue(d.autocraft() && d.autocraftDefault(), "autocraft on, and a new automatic bench's button too");
        assertTrue(d.recordCost() && d.recordCostItem().equals(ResourceLocation.withDefaultNamespace("white_dye")) && d.recordCostCount() == 1,
                "recording costs one white dye");
        for (RelativeSide side : RelativeSide.values()) {
            assertTrue(d.archiveInserts(side), "the archive takes from " + side);
            assertEquals(side == RelativeSide.DOWN, d.archiveExtracts(side), "the archive gives only below: " + side);
        }
        assertTrue(d.archiveExtracts(null), "a caller that names no side may do what any side may");
        assertTrue(d.processors() && !d.partialTabs(), "furnaces and brewing stands get their own tab, sided machines none");
        assertTrue(Config.SPEC.isLoaded(), "the junit game loads a fresh common file");
        assertEquals(d, Config.CONFIG.fromFile(), "the spec's defaults, written to a fresh file and read back, are DEFAULTS");
    }

    @Test
    void settingsSurviveTheTrip() {
        Config.Settings s = varied().build();
        ByteBuf buf = Unpooled.buffer();
        Config.Settings.STREAM_CODEC.encode(buf, s);
        assertEquals(s, Config.Settings.STREAM_CODEC.decode(buf));
        Config.Settings slabOnly = Config.Settings.builder().benchSides(0).archiveInsert(0).archiveExtract(0).slabShare(false)
                .autoButton(false).autocraftDefault(false).autoSides(0).build();
        Config.Settings.STREAM_CODEC.encode(buf, slabOnly);
        assertEquals(slabOnly, Config.Settings.STREAM_CODEC.decode(buf), "the slab's and the buttons' switches are the highest flags: they survive too");
        assertEquals(s, s.toBuilder().build(), "toBuilder copies every field");
        assertEquals(slabOnly, slabOnly.toBuilder().build(), "both ways");
        Config.Settings noButtons = Config.Settings.builder().balanceButton(false).clearMatrix(false).autoFill(false).autoButton(false).build();
        assertTrue(!noButtons.balanceButton() && !noButtons.clearMatrix() && !noButtons.autoFill() && !noButtons.autoButton() && noButtons.slab(),
                "the builder sets the four buttons and nothing else");
        assertEquals(ShareMode.OPEN, s.shareMode(), "the share default survives the trip, a byte of its own");
        assertEquals(SideMode.BLUEPRINT, s.benchSide(RelativeSide.BACK), "every side's mode survives the trip");
        assertEquals(SideMode.GRID, s.benchSide(RelativeSide.DOWN));
        assertEquals(SideMode.NONE, slabOnly.benchSide(RelativeSide.FRONT), "a closed side too");
        assertEquals(SideMode.RESULT, s.autoSide(RelativeSide.FRONT), "the automatic bench's sides survive too, RESULT included");
        assertEquals(SideMode.RESULT, s.autoSide(RelativeSide.DOWN), "the last side, at the top of the bits");
        assertTrue(!s.autocraft() && s.autocraftDefault() && slabOnly.autocraft() && !slabOnly.autocraftDefault(),
                "autocraft and its default are the highest flags: they survive the trip");
        Config.Settings off = Config.Settings.builder().autocraft(false).autocraftDefault(false).autoSides(0).build();
        assertTrue(!off.autocraft() && !off.autocraftDefault() && off.autoSide(RelativeSide.DOWN) == SideMode.NONE && off.slab()
                && off.benchSides() == Config.DEFAULTS.benchSides(), "the builder sets those three and nothing else");
        Config.Settings closed = Config.Settings.builder().benchSides(0).archiveInsert(0).archiveExtract(0b000011).build();
        assertTrue(closed.archiveExtracts(RelativeSide.BACK) && !closed.archiveInserts(RelativeSide.BACK) && closed.slab(),
                "the builder sets the sides and nothing else");
        assertFalse(closed.archiveInserts(null), "no side may insert: nor may an unnamed caller");
        assertTrue(!s.recordCost() && s.recordCostItem().equals(ResourceLocation.withDefaultNamespace("stick")) && s.recordCostCount() == 7,
                "the record cost survives the trip: its switch (the highest flag), its item and its count");
        assertTrue(slabOnly.recordCost() && slabOnly.recordCostCount() == 1, "the record cost's switch on survives too");
        assertTrue(!s.processors() && s.partialTabs() && slabOnly.processors() && !slabOnly.partialTabs(),
                "processors and partialTabs are the highest flags: they survive the trip either way");
        Config.Settings tabs = Config.Settings.builder().processors(false).partialTabs(true).build();
        assertTrue(!tabs.processors() && tabs.partialTabs() && tabs.protectVaults() && tabs.slab(), "the builder sets those two and nothing else");
        Config.Settings cost = Config.Settings.builder().recordCost(false).recordCostItem(ResourceLocation.withDefaultNamespace("paper"))
                .recordCostCount(64).build();
        Config.Settings.STREAM_CODEC.encode(buf, cost);
        assertEquals(cost, Config.Settings.STREAM_CODEC.decode(buf), "64, the most a byte carries here, survives");
        assertTrue(!cost.recordCost() && cost.recordCostCount() == 64 && cost.slab() && cost.slotLocks(), "the builder sets those three and nothing else");
    }

    @Test
    void settingsBytesStayTheSame() {
        Config.Settings s = varied().benchOnBreak(BreakMode.DROP_ALL).build();
        assertEquals("01ff97ff0000a2530002224b403f2003006c01020101136d696e6563726166743a77686974655f64796501", hex(Config.DEFAULTS));
        assertEquals("0215323500010298000214c4102003020036020100020f6d696e6563726166743a737469636b07", hex(s));
    }

    @Test
    void eachFlagHasItsOwnBit() {
        ByteBuf defaults = Unpooled.buffer();
        Config.Settings.STREAM_CODEC.encode(defaults, Config.DEFAULTS);
        for (int bit = 0; bit < Config.Settings.FLAGS.size(); ++bit) {
            if (Config.Settings.FLAGS.get(bit) == null) {
                continue;
            }
            ByteBuf buf = defaults.copy();
            buf.setInt(0, 1 << bit);
            Config.Settings one = Config.Settings.STREAM_CODEC.decode(buf);
            for (int other = 0; other < Config.Settings.FLAGS.size(); ++other) {
                if (Config.Settings.FLAGS.get(other) != null) {
                    assertEquals(other == bit, Config.Settings.FLAGS.get(other).test(one), "bit " + bit + " read back, flag " + other);
                }
            }
            ByteBuf again = Unpooled.buffer();
            Config.Settings.STREAM_CODEC.encode(again, one);
            assertEquals(1 << bit, again.getInt(0), "bit " + bit + " written back");
        }
    }

    private static Config.Settings.Builder varied() {
        return Config.Settings.builder().blueprint(false).keepLayout(false)
                .benchSides(Config.Settings.packSides(SideMode.NONE, SideMode.BLUEPRINT, SideMode.GRID, SideMode.INVENTORY, SideMode.NONE, SideMode.GRID))
                .tooltipIngredients(false).archiveStack(16).benchPanel(false).archiveInsert(0b100000).archiveExtract(0b000011).archiveHeight(2)
                .clearMatrix(false).external(false).shareMode(ShareMode.OPEN).externalSlots(54).showcaseDefault(true).autoDefault(AutoMode.TAGS)
                .archiveOnBreak(BreakMode.ALWAYS_KEEP).slab(false).balanceButton(false).autocraft(false)
                .autoSides(Config.Settings.packSides(SideMode.RESULT, SideMode.NONE, SideMode.BLUEPRINT, SideMode.GRID, SideMode.INVENTORY, SideMode.RESULT))
                .slotLocks(false).recordCost(false).recordCostItem(ResourceLocation.withDefaultNamespace("stick")).recordCostCount(7)
                .protectVaults(false).processors(false).partialTabs(true);
    }

    private static String hex(Config.Settings s) {
        ByteBuf buf = Unpooled.buffer();
        Config.Settings.STREAM_CODEC.encode(buf, s);
        return ByteBufUtil.hexDump(buf);
    }
}
