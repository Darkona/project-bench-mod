package com.darkona.projectbench;

import dev.darkona.jdlib.testkit.LangCoverage;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

class LangCoverageTest {

    @Test
    void everyItemAndBlockHasAnEnglishName() {
        Set<String> names = new TreeSet<>();
        names.addAll(LangCoverage.names(BuiltInRegistries.ITEM, ProjectBench.MOD_ID, Item::getDescriptionId));
        names.addAll(LangCoverage.names(BuiltInRegistries.BLOCK, ProjectBench.MOD_ID, Block::getDescriptionId));
        LangCoverage.assertEnglishHas(ProjectBench.MOD_ID, names);
    }

    @Test
    void everyLanguageHasTheEnglishKeys() {
        LangCoverage.assertSameKeysAsEnglish(ProjectBench.MOD_ID);
    }
}
