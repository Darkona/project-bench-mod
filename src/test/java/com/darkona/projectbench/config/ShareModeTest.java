package com.darkona.projectbench.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareModeTest {
    @Test
    void shareOffKeepsTheChestTabAwayFromTheBenchButNotFromYou() {
        assertFalse(ShareMode.NONE.allows(true, false));
        assertTrue(ShareMode.NONE.allows(true, true));
        assertTrue(ShareMode.NONE.allows(false, false));
        assertTrue(ShareMode.OPEN.allows(true, false));
        assertTrue(ShareMode.ALL.allows(true, false));
    }
}
