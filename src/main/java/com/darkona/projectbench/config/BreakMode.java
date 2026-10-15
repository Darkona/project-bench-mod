package com.darkona.projectbench.config;

public enum BreakMode {
    DROP_ALL,
    SILK_TOUCH,
    ALWAYS_KEEP;

    private static final BreakMode[] VALUES = values();

    public static BreakMode of(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : DROP_ALL;
    }
}
