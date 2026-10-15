package com.darkona.projectbench.compat.mousetweaks;

import com.darkona.projectbench.config.ShareMode;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

final class WheelRoute {
    enum Group { PLAYER, STORAGE, MATRIX, NEIGHBOUR, RESULT, OTHER }

    private static final Group[] FROM_PLAYER = {Group.STORAGE, Group.MATRIX};
    private static final Group[] FROM_STORAGE = {Group.MATRIX, Group.NEIGHBOUR, Group.PLAYER};
    private static final Group[] FROM_STORAGE_ALONE = {Group.MATRIX, Group.PLAYER};
    private static final Group[] FROM_MATRIX = {Group.STORAGE, Group.PLAYER};
    private static final Group[] TO_PLAYER = {Group.PLAYER};
    private static final Group[] INTO_PLAYER = {Group.STORAGE, Group.NEIGHBOUR, Group.RESULT, Group.MATRIX};
    private static final Group[] INTO_PLAYER_ALONE = {Group.STORAGE, Group.RESULT, Group.MATRIX};

    private WheelRoute() {
    }

    static Group[] route(Group from, boolean push, ShareMode share) {
        boolean shares = share != ShareMode.NONE;
        if (!push) {
            return from != Group.PLAYER ? TO_PLAYER : shares ? INTO_PLAYER : INTO_PLAYER_ALONE;
        }
        return switch (from) {
            case PLAYER -> FROM_PLAYER;
            case STORAGE -> shares ? FROM_STORAGE : FROM_STORAGE_ALONE;
            case MATRIX -> FROM_MATRIX;
            default -> TO_PLAYER;
        };
    }

    static Group[] reach(Group from, ShareMode share) {
        return route(from, from != Group.PLAYER, share);
    }

    @Nullable
    static Group open(Group[] route, Predicate<Group> can) {
        for (Group group : route) {
            if (can.test(group)) {
                return group;
            }
        }
        return null;
    }

    static boolean pushes(double scrollY, boolean inverted, boolean positionAware, boolean otherAbove) {
        boolean push = scrollY < 0;
        if (positionAware && otherAbove) {
            push = !push;
        }
        return inverted != push;
    }
}
