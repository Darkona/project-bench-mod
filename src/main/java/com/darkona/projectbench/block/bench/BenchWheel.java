package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.config.ShareMode;

public final class BenchWheel {
    private BenchWheel() {
    }

    public static boolean shareAllows(ShareMode share, boolean neighbour, boolean player) {
        return !neighbour || player || share != ShareMode.NONE;
    }
}
