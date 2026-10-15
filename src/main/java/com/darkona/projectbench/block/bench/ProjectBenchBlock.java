package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.block.BenchBlock;
import com.darkona.projectbench.config.BreakMode;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class ProjectBenchBlock extends BenchBlock<ProjectBenchBlockEntity> {
    public static final MapCodec<ProjectBenchBlock> CODEC = simpleCodec(ProjectBenchBlock::new);

    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;

    public enum Kind {
        BENCH, SLAB, AUTO, EJECTING;

        public boolean slab() {
            return this == SLAB;
        }

        public boolean automatic() {
            return this == AUTO || this == EJECTING;
        }

        public boolean joins() {
            return !automatic();
        }

        public boolean ejecting() {
            return this == EJECTING;
        }
    }

    private final Kind kind;

    public ProjectBenchBlock(Properties properties) {
        this(properties, Kind.BENCH);
    }

    public ProjectBenchBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(TYPE, ChestType.SINGLE));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE);
    }

    @Nullable
    public static Direction partnerSide(BlockState state) {
        return state.getValue(TYPE) != ChestType.SINGLE && Feature.SLAB_BENCH.enabled() && pairable(state.getBlock())
                ? ChestBlock.getConnectedDirection(state) : null;
    }

    private boolean pairs(Block other) {
        return other instanceof ProjectBenchBlock bench && (kind.slab() || bench.kind.slab()) && kind.joins() && bench.kind.joins();
    }

    private static boolean pairable(Block block) {
        return block instanceof ProjectBenchBlock bench && bench.kind.joins();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ChestType type = ChestType.SINGLE;
        Direction facing = context.getHorizontalDirection().getOpposite();
        boolean sneaking = context.isSecondaryUseActive();
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal() && sneaking) {
            Direction partner = candidatePartnerFacing(context, clicked.getOpposite());
            if (partner != null && partner.getAxis() != clicked.getAxis()) {
                facing = partner;
                type = partner.getCounterClockWise() == clicked.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        }
        if (type == ChestType.SINGLE && !sneaking) {
            if (facing == candidatePartnerFacing(context, facing.getClockWise())) {
                type = ChestType.LEFT;
            } else if (facing == candidatePartnerFacing(context, facing.getCounterClockWise())) {
                type = ChestType.RIGHT;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(TYPE, type);
    }

    @Nullable
    private Direction candidatePartnerFacing(BlockPlaceContext context, Direction side) {
        BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(side));
        return Feature.SLAB_BENCH.enabled() && pairs(neighbour.getBlock()) && neighbour.getValue(TYPE) == ChestType.SINGLE
                ? neighbour.getValue(FACING) : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing.getAxis().isHorizontal()) {
            if (pairs(facingState.getBlock())) {
                ChestType theirs = facingState.getValue(TYPE);
                if (state.getValue(TYPE) == ChestType.SINGLE && theirs != ChestType.SINGLE && Feature.SLAB_BENCH.enabled()
                        && state.getValue(FACING) == facingState.getValue(FACING) && ChestBlock.getConnectedDirection(facingState) == facing.getOpposite()) {
                    return state.setValue(TYPE, theirs.getOpposite());
                }
            } else if (state.getValue(TYPE) != ChestType.SINGLE && ChestBlock.getConnectedDirection(state) == facing) {
                return state.setValue(TYPE, ChestType.SINGLE);
            }
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    public static boolean joined(BlockState state, BlockState other) {
        ChestType type = state.getValue(TYPE);
        return type != ChestType.SINGLE && pairable(state.getBlock()) && pairable(other.getBlock()) && other.getValue(TYPE) == type.getOpposite()
                && other.getValue(FACING) == state.getValue(FACING);
    }

    public record Pair(ProjectBenchBlockEntity first, ProjectBenchBlockEntity second) {
    }

    public static boolean secondHalf(BlockState state) {
        return state.getValue(TYPE) == ChestType.LEFT;
    }

    @Nullable
    public static Pair pair(BlockState state, LevelAccessor level, BlockPos pos) {
        ChestType type = state.getValue(TYPE);
        if (type == ChestType.SINGLE || !Feature.SLAB_BENCH.enabled() || !(level.getBlockEntity(pos) instanceof ProjectBenchBlockEntity self)) {
            return null;
        }
        BlockPos at = pos.relative(ChestBlock.getConnectedDirection(state));
        if (joined(state, level.getBlockState(at)) && level.getBlockEntity(at) instanceof ProjectBenchBlockEntity partner) {
            return secondHalf(state) ? new Pair(partner, self) : new Pair(self, partner);
        }
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProjectBenchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    protected ProjectBenchBlockEntity entity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ProjectBenchBlockEntity bench ? bench : null;
    }

    @Override
    protected BreakMode breakMode() {
        return Config.get().benchOnBreak();
    }

    @Override
    protected void open(ProjectBenchBlockEntity entity, ServerPlayer player) {
        entity.open(player);
    }
}
