package com.darkona.projectbench.config;

public enum ShareMode {
    NONE,
    OPEN,
    ALL;

    private static final ShareMode[] VALUES = values();

    public ShareMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public boolean allows(boolean neighbour, boolean player) {
        return !neighbour || player || this != NONE;
    }

    public static ShareMode of(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ALL;
    }
}
