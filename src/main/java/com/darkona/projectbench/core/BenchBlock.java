package com.darkona.projectbench.core;

import com.darkona.projectbench.config.BreakMode;
import com.darkona.projectbench.config.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class BenchBlock<T extends BlockEntity> extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected BenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    protected abstract T entity(Level level, BlockPos pos);

    protected abstract void open(T entity, ServerPlayer player);

    protected abstract BreakMode breakMode();

    public abstract Feature feature();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            T entity = entity(level, pos);
            if (entity != null) {
                open(entity, server);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BenchBlockEntity keeps && keeps(level, state, player)) {
            keeps.keepContents(level);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private boolean keeps(Level level, BlockState state, Player player) {
        BreakMode mode = breakMode();
        if (mode == BreakMode.DROP_ALL || player.isCreative() || !player.hasCorrectToolForDrops(state)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return false;
        }
        return mode == BreakMode.ALWAYS_KEEP || EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().holderOrThrow(Enchantments.SILK_TOUCH), player.getMainHandItem()) > 0;
    }

    protected boolean sidesMayDiffer(BlockState state, BlockState now) {
        return state != now;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BenchBlockEntity contents) {
            if (!contents.keeping()) {
                contents.dropContents(level, pos);
            }
            level.updateNeighbourForOutputSignal(pos, this);
        } else if (state.is(newState.getBlock()) && sidesMayDiffer(state, newState) && !level.isClientSide) {
            level.invalidateCapabilities(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
