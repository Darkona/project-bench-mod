package com.darkona.projectbench.block.bench;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum Processor {
    FURNACE(() -> BlockEntityType.FURNACE, RecipeType.SMELTING, 0, 3),
    BLAST_FURNACE(() -> BlockEntityType.BLAST_FURNACE, RecipeType.BLASTING, 0, 3),
    SMOKER(() -> BlockEntityType.SMOKER, RecipeType.SMOKING, 0, 3),
    BREWING_STAND(() -> BlockEntityType.BREWING_STAND, null, 3, 5);

    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int RESULT = 2;
    public static final int INGREDIENT = 3;
    public static final int BREWING_FUEL = 4;
    public static final int BOTTLES = 5;
    public static final int DATA = 4;

    private static final Processor[] VALUES = values();

    private final Supplier<BlockEntityType<?>> type;
    @Nullable
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final int firstSlot;
    private final int slots;

    Processor(Supplier<BlockEntityType<?>> type, @Nullable RecipeType<? extends AbstractCookingRecipe> recipeType, int firstSlot, int slots) {
        this.type = type;
        this.recipeType = recipeType;
        this.firstSlot = firstSlot;
        this.slots = slots;
    }

    @Nullable
    public static Processor of(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        BlockEntityType<?> type = blockEntity.getType();
        for (Processor processor : VALUES) {
            if (processor.type.get() == type) {
                return processor;
            }
        }
        return null;
    }

    public boolean brewing() {
        return this == BREWING_STAND;
    }

    public int firstSlot() {
        return firstSlot;
    }

    public int slots() {
        return slots;
    }

    @Nullable
    public static ContainerData data(@Nullable BlockEntity blockEntity) {
        return switch (blockEntity) {
            case AbstractFurnaceBlockEntity furnace -> furnace.dataAccess;
            case BrewingStandBlockEntity stand -> stand.dataAccess;
            case null, default -> null;
        };
    }

    public int shiftTarget(ItemStack stack, Level level) {
        if (brewing()) {
            if (stack.is(Items.BLAZE_POWDER)) {
                return BREWING_FUEL;
            }
            if (level.potionBrewing().isIngredient(stack)) {
                return INGREDIENT;
            }
            return level.potionBrewing().isInput(stack) || stack.is(Items.GLASS_BOTTLE) ? BOTTLES : -1;
        }
        if (smelts(stack, level)) {
            return INPUT;
        }
        return stack.getBurnTime(recipeType) > 0 ? FUEL : -1;
    }

    @SuppressWarnings("unchecked")
    private boolean smelts(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor((RecipeType<AbstractCookingRecipe>) recipeType, new SingleRecipeInput(stack), level).isPresent();
    }

    public boolean predictPlace(int slot, ItemStack stack, ItemStack present, Level level) {
        if (brewing()) {
            return switch (slot) {
                case INGREDIENT -> level.potionBrewing().isIngredient(stack);
                case BREWING_FUEL -> stack.is(Items.BLAZE_POWDER);
                default -> (level.potionBrewing().isInput(stack) || stack.is(Items.GLASS_BOTTLE)) && present.isEmpty();
            };
        }
        return switch (slot) {
            case INPUT -> true;
            case FUEL -> stack.getBurnTime(recipeType) > 0 || stack.is(Items.BUCKET) && !present.is(Items.BUCKET);
            default -> false;
        };
    }

    public boolean resultSlot(int slot) {
        return !brewing() && slot == RESULT;
    }

    public boolean bottleSlot(int slot) {
        return brewing() && slot < 3;
    }
}
