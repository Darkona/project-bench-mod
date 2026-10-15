package com.darkona.projectbench.block.archive;

import net.minecraft.util.StringRepresentable;

public enum ColumnPart implements StringRepresentable {
    SINGLE("single"),
    BOTTOM("bottom"),
    MIDDLE("middle"),
    TOP("top");

    private final String id;

    ColumnPart(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static ColumnPart of(boolean below, boolean above) {
        return below ? (above ? MIDDLE : TOP) : (above ? BOTTOM : SINGLE);
    }
}
