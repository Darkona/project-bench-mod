package com.darkona.projectbench.content.archive;

import com.darkona.projectbench.foundation.BenchBlock;
import com.darkona.projectbench.config.BreakMode;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class BlueprintArchiveBlock extends BenchBlock<BlueprintArchiveBlockEntity> {
    public static final MapCodec<BlueprintArchiveBlock> CODEC = simpleCodec(BlueprintArchiveBlock::new);
    public static final EnumProperty<ColumnPart> PART = EnumProperty.create("part", ColumnPart.class);

    public BlueprintArchiveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, ColumnPart.SINGLE));
    }

    @Override
    public Feature feature() {
        return Feature.ARCHIVE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintArchiveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    protected BlueprintArchiveBlockEntity entity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlueprintArchiveBlockEntity archive ? archive : null;
    }

    @Override
    protected BreakMode breakMode() {
        return Config.get().archiveOnBreak();
    }

    @Override
    protected void open(BlueprintArchiveBlockEntity entity, ServerPlayer player) {
        player.openMenu(entity, buf -> buf.writeVarInt(entity.column().size()));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (state == null) {
            return null;
        }
        if (level.getBlockState(pos.below()).is(this)) {
            state = state.setValue(FACING, level.getBlockState(pos.below()).getValue(FACING));
        } else if (level.getBlockState(pos.above()).is(this)) {
            state = state.setValue(FACING, level.getBlockState(pos.above()).getValue(FACING));
        }
        return state.setValue(PART, partAt(level, pos));
    }

    private int run(LevelReader level, BlockPos pos, Direction d) {
        int n = 0;
        BlockPos.MutableBlockPos p = pos.mutable();
        while (n < 64 && level.getBlockState(p.move(d)).is(this)) {
            ++n;
        }
        return n;
    }

    private ColumnPart partAt(LevelReader level, BlockPos pos) {
        int height = Config.get().archiveHeight();
        int index = run(level, pos, Direction.DOWN) % height;
        int above = Math.min(run(level, pos, Direction.UP), height - 1 - index);
        return ColumnPart.of(index > 0, above > 0);
    }

    private void reconnectRun(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dir = -1; dir <= 1; dir += 2) {
            p.set(pos);
            if (dir > 0) {
                p.move(Direction.UP);
            }
            for (int i = 0; i < 64 && level.getBlockState(p).is(this); ++i) {
                BlockState now = level.getBlockState(p);
                ColumnPart part = partAt(level, p);
                if (now.getValue(PART) != part) {
                    level.setBlock(p, now.setValue(PART, part), Block.UPDATE_CLIENTS);
                }
                p.move(dir > 0 ? Direction.UP : Direction.DOWN);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            reconnectRun(level, pos);
            BlueprintArchiveBlockEntity.forgetRun(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            reconnectRun(level, pos.above());
            reconnectRun(level, pos.below());
            BlueprintArchiveBlockEntity.forgetRun(level, pos.above());
            BlueprintArchiveBlockEntity.forgetRun(level, pos.below());
        }
    }

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
}
