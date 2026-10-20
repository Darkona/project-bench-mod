package com.darkona.projectbench.block.bench;

import dev.darkona.jdlib.api.menu.MoveRoutes;
import dev.darkona.jdlib.api.menu.SlotMap;
import dev.darkona.jdlib.api.menu.SlotRole;

final class BenchSlotMap {
    private BenchSlotMap() {
    }

    static SlotMap build(int[] matrixStart, int[] resultIndex, int[] storageStart, int outsideStart) {
        int halves = matrixStart.length;
        SlotMap.Builder map = SlotMap.builder();
        for (int h = 0; h < halves; ++h) {
            map.group("storage" + h, SlotRole.STORAGE, storageStart[h], ProjectBenchBlockEntity.STORAGE);
        }
        map.group("neighbour", SlotRole.OTHER, outsideStart, ProjectBenchMenu.OUTSIDE_VISIBLE);
        for (int h = 0; h < halves; ++h) {
            map.group("result" + h, SlotRole.OUTPUT, resultIndex[h], 1);
        }
        for (int h = 0; h < halves; ++h) {
            map.group("matrix" + h, SlotRole.INPUT, matrixStart[h], ProjectBenchBlockEntity.MATRIX);
        }
        map.group("main", SlotRole.PLAYER_MAIN, ProjectBenchMenu.PLAYER_START, ProjectBenchMenu.PLAYER_SLOTS - 9);
        map.group("hotbar", SlotRole.PLAYER_HOTBAR, ProjectBenchMenu.PLAYER_END - 9, 9);

        String[] storage = names("storage", halves);
        String[] matrix = names("matrix", halves);
        MoveRoutes.Builder routes = MoveRoutes.builder();
        for (String player : new String[] {"main", "hotbar"}) {
            routes.from(player).to(concat(storage, matrix));
        }
        for (String from : storage) {
            routes.from(from).to(concat(matrix, new String[] {"neighbour", "main", "hotbar"}));
        }
        for (String from : matrix) {
            routes.from(from).to(concat(storage, new String[] {"main", "hotbar"}));
        }
        for (String from : concat(names("result", halves), new String[] {"neighbour"})) {
            routes.from(from).to("main", "hotbar");
        }
        return map.routes(routes.build()).build();
    }

    private static String[] names(String prefix, int halves) {
        String[] out = new String[halves];
        for (int h = 0; h < halves; ++h) {
            out[h] = prefix + h;
        }
        return out;
    }

    private static String[] concat(String[] a, String[] b) {
        String[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
