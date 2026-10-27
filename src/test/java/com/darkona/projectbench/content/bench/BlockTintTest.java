package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.content.bench.BlockTint;
import com.darkona.projectbench.config.ClientConfig;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockTintTest {
    @Test
    void aLineIsABlockAndOneOrTwoColours() {
        ClientConfig.TintLine spruce = ClientConfig.TintLine.parse("quark:spruce_chest=#735531");
        assertNotNull(spruce);
        assertEquals(ResourceLocation.fromNamespaceAndPath("quark", "spruce_chest"), spruce.block());
        assertEquals(0x735531, spruce.window());
        assertEquals(BlockTint.USUAL_TEXT, spruce.text(), "no second colour: the usual title");
        ClientConfig.TintLine dark = ClientConfig.TintLine.parse(" minecraft:black_shulker_box = #262626 , #F0F0F0 ");
        assertNotNull(dark, "spaces and capital hex digits are fine");
        assertEquals(0x262626, dark.window());
        assertEquals(0xFFF0F0F0, dark.text(), "the title's colour, opaque");
    }

    @Test
    void aLineNotWrittenRightIsNone() {
        for (String bad : new String[]{"", "quark:spruce_chest", "spruce_chest=#735531", "quark:spruce_chest=735531", "quark:spruce_chest=#73553",
                "quark:spruce_chest=#73553g", "quark:Spruce_Chest=#735531", "quark:spruce_chest=#735531,", "quark:spruce_chest=#735531,#zzzzzz",
                "quark:spruce_chest=#735531,#f0f0f0,#000000", "quark:spruce_chest="}) {
            assertNull(ClientConfig.TintLine.parse(bad), bad);
        }
    }

    @Test
    void ironChestsComeListedTrappedOrNotAndBadLinesAreLeftOut() {
        Object2LongOpenHashMap<ResourceLocation> listed = new Object2LongOpenHashMap<>();
        BlockTint.listed(ClientConfig.tints(), listed);
        assertEquals(31, listed.size(), "seventeen shulker boxes, and seven Iron Chests tiers each with its trapped twin");
        for (String tier : new String[]{"iron", "gold", "diamond", "copper", "crystal", "obsidian", "dirt"}) {
            long plain = listed.getLong(ResourceLocation.fromNamespaceAndPath("ironchest", tier + "_chest"));
            assertNotEquals(BlockTint.PLAIN, BlockTint.window(plain), tier + " is tinted");
            assertEquals(plain, listed.getLong(ResourceLocation.fromNamespaceAndPath("ironchest", "trapped_" + tier + "_chest")), "trapped " + tier);
        }
        long obsidian = listed.getLong(ResourceLocation.fromNamespaceAndPath("ironchest", "obsidian_chest"));
        long gold = listed.getLong(ResourceLocation.fromNamespaceAndPath("ironchest", "gold_chest"));
        assertEquals(0x5A4A7A, BlockTint.window(obsidian));
        assertNotEquals(BlockTint.USUAL_TEXT, BlockTint.text(obsidian), "a dark chest has a light title");
        assertEquals(BlockTint.USUAL_TEXT, BlockTint.text(gold), "a light one keeps the usual");
        BlockTint.listed(List.of("quark:spruce_chest=#735531", "nonsense", "quark:oak_chest=#zzzzzz"), listed);
        assertEquals(1, listed.size(), "only the good line, and the list before it is gone");
    }

    @Test
    void softeningGoesHalfwayToWhite() {
        assertEquals(0x7F7F7F, BlockTint.soften(0x000000));
        assertEquals(0xFFFFFF, BlockTint.soften(0xFFFFFF));
        assertEquals(0xBF997F, BlockTint.soften(0x7F3300));
    }

    @Test
    void fastColorKeepsTheOldInts() {
        for (int rgb = 0; rgb <= 0xFFFFFF; ++rgb) {
            int old = ((rgb >> 16 & 0xFF) + 0xFF) / 2 << 16 | ((rgb >> 8 & 0xFF) + 0xFF) / 2 << 8 | ((rgb & 0xFF) + 0xFF) / 2;
            if (BlockTint.soften(rgb) != old) {
                assertEquals(old, BlockTint.soften(rgb), "soften " + Integer.toHexString(rgb));
            }
        }
    }

    @Test
    void everyShulkerBoxComesListedByItsDyeAndADarkOneHasALightTitle() {
        Object2LongOpenHashMap<ResourceLocation> listed = new Object2LongOpenHashMap<>();
        BlockTint.listed(ClientConfig.tints(), listed);
        for (DyeColor dye : DyeColor.values()) {
            assertTrue(listed.containsKey(ResourceLocation.withDefaultNamespace(dye.getSerializedName() + "_shulker_box")), dye + " is listed");
        }
        int red = BlockTint.window(listed.getLong(ResourceLocation.withDefaultNamespace("red_shulker_box")));
        assertTrue((red >> 16 & 0xFF) > (red >> 8 & 0xFF) + 40 && (red >> 16 & 0xFF) > (red & 0xFF) + 40,
                "a red box's window is still red, though softened: " + Integer.toHexString(red));
        long black = listed.getLong(ResourceLocation.withDefaultNamespace("black_shulker_box"));
        int window = BlockTint.window(black);
        assertTrue((window >> 16 & 0xFF) >= 38 && (window >> 8 & 0xFF) >= 38 && (window & 0xFF) >= 38, "never black: " + Integer.toHexString(window));
        assertEquals(0xFFF0F0F0, BlockTint.text(black), "a black box has a light title");
        assertEquals(BlockTint.USUAL_TEXT, BlockTint.text(listed.getLong(ResourceLocation.withDefaultNamespace("white_shulker_box"))));
        assertEquals(0x976797, BlockTint.window(listed.getLong(ResourceLocation.withDefaultNamespace("shulker_box"))), "an undyed box is its own purple");
    }

    @Test
    void packingKeepsBothColours() {
        assertEquals(BlockTint.PLAIN, BlockTint.window(BlockTint.NONE));
        assertEquals(BlockTint.USUAL_TEXT, BlockTint.text(BlockTint.NONE));
        long both = BlockTint.pack(0xABCDEF, 0xFF102030);
        assertEquals(0xABCDEF, BlockTint.window(both));
        assertEquals(0xFF102030, BlockTint.text(both));
        assertFalse(BlockTint.pack(BlockTint.PLAIN, 0xFFF0F0F0) == BlockTint.NONE);
    }

    @Test
    void aPlainWindowLeavesItsSlotsAloneAndATintedOneLightensThem() {
        assertEquals(0, BlockTint.slotFill(BlockTint.PLAIN), "no paint: the vanilla slot");
        assertEquals(0xFFDCDCDC, BlockTint.slotFill(0xFFFFFF), "white: the panel grey, 40 % towards white");
        int red = BlockTint.slotFill(0xFF0000);
        assertEquals(0xFF, red >>> 24, "opaque");
        assertTrue((red >> 16 & 0xFF) > (red >> 8 & 0xFF), "keeps the hue");
    }

    @Test
    void aBlocksPaletteNameKeepsItsNamespace() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("quark", "projectbench_window/spruce_chest"),
                BlockTint.paletteKey(ResourceLocation.fromNamespaceAndPath("quark", "spruce_chest")));
    }
}
