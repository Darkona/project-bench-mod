package com.darkona.projectbench;

import dev.darkona.jdlib.testkit.SourceContracts;
import org.junit.jupiter.api.Test;

class SourceContractTest {

    @Test
    void noCommentsInThePublicRepository() {
        SourceContracts.of("src/main/java", "src/test/java", "src/gametest/java").assertNoComments();
    }
}
