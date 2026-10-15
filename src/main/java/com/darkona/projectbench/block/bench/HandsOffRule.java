package com.darkona.projectbench.block.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface HandsOffRule {
    boolean handsOff(Level level, BlockPos pos, BlockState state);
}
