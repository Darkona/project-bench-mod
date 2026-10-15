package com.darkona.projectbench.compat.mousetweaks;

import com.darkona.projectbench.compat.mousetweaks.WheelRoute.Group;
import com.darkona.projectbench.config.ShareMode;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WheelRouteTest {
    @Test
    void fromThePlayerTheInternalInventoryThenTheMatrix() {
        for (ShareMode share : ShareMode.values()) {
            assertEquals(List.of(Group.STORAGE, Group.MATRIX), List.of(WheelRoute.route(Group.PLAYER, true, share)), share.name());
        }
    }

    @Test
    void fromTheInternalInventoryTheMatrixThenTheNeighbourShareSaysThenThePlayer() {
        assertEquals(List.of(Group.MATRIX, Group.NEIGHBOUR, Group.PLAYER), List.of(WheelRoute.route(Group.STORAGE, true, ShareMode.OPEN)));
        assertEquals(List.of(Group.MATRIX, Group.NEIGHBOUR, Group.PLAYER), List.of(WheelRoute.route(Group.STORAGE, true, ShareMode.ALL)));
        assertEquals(List.of(Group.MATRIX, Group.PLAYER), List.of(WheelRoute.route(Group.STORAGE, true, ShareMode.NONE)));
    }

    @Test
    void fromTheMatrixTheInternalInventoryThenThePlayerAndAnythingElseToThePlayer() {
        assertEquals(List.of(Group.STORAGE, Group.PLAYER), List.of(WheelRoute.route(Group.MATRIX, true, ShareMode.ALL)));
        for (Group from : List.of(Group.RESULT, Group.NEIGHBOUR, Group.OTHER)) {
            assertEquals(List.of(Group.PLAYER), List.of(WheelRoute.route(from, true, ShareMode.ALL)), from.name());
        }
    }

    @Test
    void pullingIntoThePlayerTheMatrixLast() {
        assertEquals(List.of(Group.STORAGE, Group.NEIGHBOUR, Group.RESULT, Group.MATRIX), List.of(WheelRoute.route(Group.PLAYER, false, ShareMode.OPEN)));
        assertEquals(List.of(Group.STORAGE, Group.RESULT, Group.MATRIX), List.of(WheelRoute.route(Group.PLAYER, false, ShareMode.NONE)));
        for (Group into : List.of(Group.STORAGE, Group.MATRIX, Group.RESULT, Group.OTHER)) {
            assertEquals(List.of(Group.PLAYER), List.of(WheelRoute.route(into, false, ShareMode.ALL)), into.name());
        }
    }

    @Test
    void theListHoldsBothWays() {
        for (Group from : Group.values()) {
            for (ShareMode share : ShareMode.values()) {
                Set<Group> reach = EnumSet.noneOf(Group.class);
                reach.addAll(List.of(WheelRoute.reach(from, share)));
                assertTrue(reach.containsAll(List.of(WheelRoute.route(from, true, share))), from + " pushing, " + share);
                assertTrue(reach.containsAll(List.of(WheelRoute.route(from, false, share))), from + " pulling, " + share);
            }
        }
    }

    @Test
    void onlyTheFirstGroupWithRoomIsOpen() {
        Group[] route = WheelRoute.route(Group.STORAGE, true, ShareMode.OPEN);
        assertEquals(Group.MATRIX, WheelRoute.open(route, g -> true));
        assertEquals(Group.NEIGHBOUR, WheelRoute.open(route, g -> g != Group.MATRIX));
        assertEquals(Group.PLAYER, WheelRoute.open(route, g -> g == Group.PLAYER));
        assertNull(WheelRoute.open(route, g -> false));
    }

    @Test
    void theDirectionIsMouseTweaks() {
        assertTrue(WheelRoute.pushes(-1, false, false, false), "down pushes");
        assertFalse(WheelRoute.pushes(1, false, false, false), "up pulls");
        assertFalse(WheelRoute.pushes(-1, true, false, false), "inverted");
        assertFalse(WheelRoute.pushes(-1, false, true, true), "the other side above");
        assertTrue(WheelRoute.pushes(-1, false, true, false), "the other side below");
        assertTrue(WheelRoute.pushes(-1, true, true, true), "both");
    }
}
