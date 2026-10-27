package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.content.bench.BenchSlotMap;
import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import dev.darkona.jdlib.api.menu.MoveRoutes;
import dev.darkona.jdlib.api.menu.SlotGroup;
import dev.darkona.jdlib.api.menu.SlotMap;
import dev.darkona.jdlib.api.menu.SlotRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BenchSlotMapTest {
    private static final int OUTSIDE = ProjectBenchMenu.ARCHIVE_START + 12;
    private static final int SECOND = OUTSIDE + ProjectBenchMenu.OUTSIDE_VISIBLE;

    private static SlotMap single() {
        return BenchSlotMap.build(new int[] {0}, new int[] {ProjectBenchMenu.RESULT}, new int[] {ProjectBenchMenu.STORAGE_START}, OUTSIDE);
    }

    private static SlotMap station() {
        return BenchSlotMap.build(new int[] {0, SECOND}, new int[] {ProjectBenchMenu.RESULT, SECOND + ProjectBenchMenu.RESULT},
                new int[] {ProjectBenchMenu.STORAGE_START, SECOND + ProjectBenchMenu.STORAGE_START}, OUTSIDE);
    }

    private static List<String> to(SlotMap map, String from) {
        MoveRoutes.Route route = map.routes().route(from);
        assertNotNull(route, from);
        return route.to();
    }

    @Test
    void aBenchHasOneOfEachGroupAndTheStationTwo() {
        assertEquals(List.of("storage0", "neighbour", "result0", "matrix0", "main", "hotbar"), single().groups().stream().map(SlotGroup::name).toList());
        assertEquals(List.of("storage0", "storage1", "neighbour", "result0", "result1", "matrix0", "matrix1", "main", "hotbar"),
                station().groups().stream().map(SlotGroup::name).toList());
    }

    @Test
    void theGroupsCoverTheSlotsOfTheMenu() {
        SlotMap map = station();
        assertEquals(SlotRole.INPUT, map.roleOf(0));
        assertEquals(SlotRole.OUTPUT, map.roleOf(ProjectBenchMenu.RESULT));
        assertEquals(SlotRole.STORAGE, map.roleOf(ProjectBenchMenu.STORAGE_START + 17));
        assertNull(map.roleOf(ProjectBenchMenu.BLUEPRINT));
        assertEquals(SlotRole.PLAYER_MAIN, map.roleOf(ProjectBenchMenu.PLAYER_START));
        assertEquals(SlotRole.PLAYER_HOTBAR, map.roleOf(ProjectBenchMenu.PLAYER_END - 1));
        assertEquals(SlotRole.OTHER, map.roleOf(OUTSIDE));
        assertEquals(SlotRole.INPUT, map.roleOf(SECOND));
        assertEquals(SlotRole.OUTPUT, map.roleOf(SECOND + ProjectBenchMenu.RESULT));
        assertEquals(27, map.group("main").size());
        assertEquals(9, map.group("hotbar").size());
    }

    @Test
    void theWheelRoutesAreTheOldTable() {
        SlotMap map = single();
        assertEquals(List.of("storage0", "matrix0"), to(map, "main"));
        assertEquals(List.of("storage0", "matrix0"), to(map, "hotbar"));
        assertEquals(List.of("matrix0", "neighbour", "main", "hotbar"), to(map, "storage0"));
        assertEquals(List.of("storage0", "main", "hotbar"), to(map, "matrix0"));
        assertEquals(List.of("main", "hotbar"), to(map, "result0"));
        assertEquals(List.of("main", "hotbar"), to(map, "neighbour"));
    }

    @Test
    void aStationReachesBothStorages() {
        SlotMap map = station();
        assertEquals(List.of("storage0", "storage1", "matrix0", "matrix1"), to(map, "main"));
        assertEquals(List.of("matrix0", "matrix1", "neighbour", "main", "hotbar"), to(map, "storage1"));
        assertEquals(List.of("storage0", "storage1", "main", "hotbar"), to(map, "matrix1"));
    }
}
