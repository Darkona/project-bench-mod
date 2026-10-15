package com.darkona.projectbench.config;

public enum AutoMode {
    OFF,
    ITEM,
    TAGS;

    private static final AutoMode[] VALUES = values();

    public AutoMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static AutoMode of(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ITEM;
    }
}
