package com.darkona.projectbench.block.bench;

import org.junit.jupiter.api.Test;

import static com.darkona.projectbench.block.bench.ExternalInventories.NOTHING;
import static com.darkona.projectbench.block.bench.ExternalInventories.NO_TAB;
import static com.darkona.projectbench.block.bench.ExternalInventories.PROCESSOR;
import static com.darkona.projectbench.block.bench.ExternalInventories.SIDED;
import static com.darkona.projectbench.block.bench.ExternalInventories.STORAGE;
import static com.darkona.projectbench.block.bench.ExternalInventories.TAB_PROCESSOR;
import static com.darkona.projectbench.block.bench.ExternalInventories.TAB_SLOTS;
import static com.darkona.projectbench.block.bench.ExternalInventories.kindOf;
import static com.darkona.projectbench.block.bench.ExternalInventories.tab;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalTabsTest {
    @Test
    void theSameSlotsThroughEveryFaceIsStorage() {
        assertEquals(STORAGE, kindOf(new int[]{27, 27, 27, 27, 27, 27}), "a chest");
        assertEquals(STORAGE, kindOf(new int[]{0, 0, 0, 0, 0, 0}), "an empty drawer controller still looks the same everywhere");
        assertEquals(NOTHING, kindOf(new int[]{-1, -1, -1, -1, -1, -1}), "no handler on any face");
    }

    @Test
    void differentSlotsThroughDifferentFacesIsSided() {
        assertEquals(SIDED, kindOf(new int[]{2, 1, 1, 1, 1, 1}), "a furnace: output and fuel below, input on top, fuel on the sides");
        assertEquals(SIDED, kindOf(new int[]{9, -1, 9, 9, 9, 9}), "a face with no handler at all is a difference too");
        assertEquals(SIDED, kindOf(new int[]{-1, 1, -1, -1, -1, -1}), "a hopper-like input only on top");
    }

    @Test
    void storageAlwaysGetsItsTab() {
        for (boolean processors : new boolean[]{false, true}) {
            for (boolean partial : new boolean[]{false, true}) {
                assertEquals(TAB_SLOTS, tab(STORAGE, processors, partial));
                assertEquals(NO_TAB, tab(NOTHING, processors, partial));
            }
        }
    }

    @Test
    void aSidedBlockGetsATabOnlyWithPartialTabs() {
        assertEquals(NO_TAB, tab(SIDED, true, false));
        assertEquals(TAB_SLOTS, tab(SIDED, true, true));
    }

    @Test
    void aProcessorGetsItsOwnWindowOrFallsBackToSided() {
        for (Processor processor : Processor.values()) {
            int kind = PROCESSOR + processor.ordinal();
            assertEquals(TAB_PROCESSOR, tab(kind, true, false), processor + " with processors on");
            assertEquals(TAB_PROCESSOR, tab(kind, true, true), processor + ": partialTabs does not matter");
            assertEquals(NO_TAB, tab(kind, false, false), processor + " off: like any sided block");
            assertEquals(TAB_SLOTS, tab(kind, false, true), processor + " off with partialTabs: the old tab");
        }
    }
}
