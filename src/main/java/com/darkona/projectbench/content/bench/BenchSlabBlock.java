package com.darkona.projectbench.content.bench;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

public class BenchSlabBlock extends ProjectBenchBlock {
    public static final MapCodec<BenchSlabBlock> CODEC = simpleCodec(BenchSlabBlock::new);

    public static final BooleanProperty LEGS_NORTH = BooleanProperty.create("legs_north");
    public static final BooleanProperty LEGS_SOUTH = BooleanProperty.create("legs_south");
    public static final BooleanProperty LEGS_WEST = BooleanProperty.create("legs_west");
    public static final BooleanProperty LEGS_EAST = BooleanProperty.create("legs_east");
    private static final BooleanProperty[] LEGS = {LEGS_SOUTH, LEGS_WEST, LEGS_NORTH, LEGS_EAST};
    private static final Direction[] SIDES = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        VoxelShape board = Block.box(0, 8, 0, 16, 16, 16);
        int south = 1;
        int west = 2;
        int north = 4;
        int east = 8;
        for (int bits = 0; bits < SHAPES.length; bits++) {
            VoxelShape shape = board;
            shape = corner(shape, bits, north | west, 0, 0);
            shape = corner(shape, bits, north | east, 14, 0);
            shape = corner(shape, bits, south | west, 0, 14);
            shape = corner(shape, bits, south | east, 14, 14);
            SHAPES[bits] = shape.optimize();
        }
    }

    public BenchSlabBlock(Properties properties) {
        super(properties, Kind.SLAB);
        registerDefaultState(defaultBlockState().setValue(LEGS_NORTH, true).setValue(LEGS_SOUTH, true)
                .setValue(LEGS_WEST, true).setValue(LEGS_EAST, true));
    }

    private static VoxelShape corner(VoxelShape shape, int bits, int sides, int x, int z) {
        return (bits & sides) == sides ? Shapes.or(shape, Block.box(x, 0, z, x + 2, 8, z + 2)) : shape;
    }

    public static BooleanProperty legs(Direction side) {
        return LEGS[side.get2DDataValue()];
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEGS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int bits = 0;
        for (int i = 0; i < LEGS.length; i++) {
            if (state.getValue(LEGS[i])) {
                bits |= 1 << i;
            }
        }
        return SHAPES[bits];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withLegs(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        BlockState updated = super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        return facing.getAxis().isHorizontal() ? updated.setValue(legs(facing), hasLegs(updated, facing, facingState, level, facingPos)) : updated;
    }

    private static BlockState withLegs(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Direction side : SIDES) {
            at.setWithOffset(pos, side);
            state = state.setValue(legs(side), hasLegs(state, side, level.getBlockState(at), level, at));
        }
        return state;
    }

    private static boolean hasLegs(BlockState state, Direction side, BlockState neighbour, BlockGetter level, BlockPos at) {
        boolean partner = state.getValue(TYPE) != ChestType.SINGLE && ChestBlock.getConnectedDirection(state) == side;
        return !partner && !leansOn(neighbour, level, at, side);
    }

    private static boolean leansOn(BlockState neighbour, BlockGetter level, BlockPos at, Direction side) {
        return neighbour.isFaceSturdy(level, at, side.getOpposite()) || neighbour.is(Tags.Blocks.CHESTS);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        for (Direction side : SIDES) {
            rotated = rotated.setValue(legs(rotation.rotate(side)), state.getValue(legs(side)));
        }
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        for (Direction side : SIDES) {
            mirrored = mirrored.setValue(legs(mirror.mirror(side)), state.getValue(legs(side)));
        }
        return mirrored;
    }
}
