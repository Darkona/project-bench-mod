package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.config.BenchConfig;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class HandsOff {
    private static volatile Rule[] rules = new Rule[0];

    private HandsOff() {
    }

    public static synchronized void register(Rule rule) {
        Rule[] more = Arrays.copyOf(rules, rules.length + 1);
        more[rules.length] = rule;
        rules = more;
    }

    public static boolean test(Level level, BlockPos pos, BlockState state) {
        if (!BenchConfig.get().protectVaults()) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.handsOff(level, pos, state)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface Rule {
        boolean handsOff(Level level, BlockPos pos, BlockState state);
    }
}
