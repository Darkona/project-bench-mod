package com.darkona.projectbench.config;

public enum SideMode {
    NONE, INVENTORY, GRID, BLUEPRINT, RESULT;

    private static final SideMode[] VALUES = values();

    public static SideMode of(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }
}
