package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.config.Config;
import dev.darkona.jdlib.api.inventory.SlotLockedContainer;

public final class Storage extends SlotLockedContainer {
    public Storage(int size) {
        super(size);
    }

    @Override
    protected boolean locksApply() {
        return Config.get().slotLocks();
    }
}
