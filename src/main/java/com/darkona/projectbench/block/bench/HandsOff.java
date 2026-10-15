package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public final class HandsOff {
    private static volatile HandsOffRule[] rules = new HandsOffRule[0];

    private HandsOff() {
    }

    public static synchronized void register(HandsOffRule rule) {
        HandsOffRule[] more = Arrays.copyOf(rules, rules.length + 1);
        more[rules.length] = rule;
        rules = more;
    }

    public static boolean test(Level level, BlockPos pos, BlockState state) {
        if (!Config.get().protectVaults()) {
            return false;
        }
        for (HandsOffRule rule : rules) {
            if (rule.handsOff(level, pos, state)) {
                return true;
            }
        }
        return false;
    }
}
