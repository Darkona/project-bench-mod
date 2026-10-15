package com.darkona.projectbench.block;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum RelativeSide {
    FRONT, BACK, LEFT, RIGHT, UP, DOWN;

    private static final RelativeSide[] VALUES = values();
    private static final RelativeSide[] TABLE = new RelativeSide[4 * 6];

    static {
        for (int f = 0; f < 4; ++f) {
            Direction facing = Direction.from2DDataValue(f);
            for (Direction side : Direction.values()) {
                TABLE[f * 6 + side.get3DDataValue()] = side == Direction.UP ? UP : side == Direction.DOWN ? DOWN
                        : side == facing ? FRONT : side == facing.getOpposite() ? BACK : side == facing.getClockWise() ? LEFT : RIGHT;
            }
        }
    }

    public static RelativeSide of(Direction facing, Direction side) {
        return TABLE[facing.get2DDataValue() * 6 + side.get3DDataValue()];
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static RelativeSide byKey(String key) {
        for (RelativeSide side : VALUES) {
            if (side.key().equalsIgnoreCase(key)) {
                return side;
            }
        }
        return null;
    }
}
