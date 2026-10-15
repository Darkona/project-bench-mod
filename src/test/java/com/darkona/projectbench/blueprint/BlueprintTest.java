package com.darkona.projectbench.blueprint;

import com.darkona.projectbench.registry.ModComponents;
import com.darkona.projectbench.registry.ModItems;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.MapCloningRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintTest {
    private static final RegistryAccess ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static Blueprint sticks() {
        List<ItemStack> pattern = new ArrayList<>(9);
        for (int i = 0; i < 9; ++i) {
            pattern.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS, 5) : ItemStack.EMPTY);
        }
        ItemStack named = new ItemStack(Items.STICK, 4);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("sticks"));
        return new Blueprint(pattern, ResourceLocation.withDefaultNamespace("stick"), named);
    }

    @Test
    void ghostsAreOneItemEach() {
        Blueprint bp = sticks();
        assertEquals(1, bp.ghost(1).getCount(), "a ghost is one item, whatever the matrix held");
        assertTrue(bp.ghost(0).isEmpty());
        assertEquals(4, bp.result().getCount());
        assertThrows(IllegalArgumentException.class, () -> new Blueprint(List.of(), bp.recipe(), bp.result()));
    }

    @Test
    void codecRoundTrip() {
        Blueprint bp = sticks();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, ACCESS);
        JsonElement json = Blueprint.CODEC.encodeStart(ops, bp).getOrThrow();
        Blueprint back = Blueprint.CODEC.parse(ops, json).getOrThrow();
        assertEquals(bp, back);
        assertEquals(bp.hashCode(), back.hashCode());
    }

    @Test
    void streamCodecRoundTrip() {
        Blueprint bp = sticks();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), ACCESS);
        Blueprint.STREAM_CODEC.encode(buf, bp);
        Blueprint back = Blueprint.STREAM_CODEC.decode(buf);
        assertEquals(bp, back);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void equalRecipesStack() {
        ItemStack a = new ItemStack(ModItems.BLUEPRINT.get());
        ItemStack b = new ItemStack(ModItems.BLUEPRINT.get());
        assertNull(Blueprint.of(a), "a new blueprint is blank");
        a.set(ModComponents.BLUEPRINT.get(), sticks());
        b.set(ModComponents.BLUEPRINT.get(), sticks());
        assertTrue(ItemStack.isSameItemSameComponents(a, b), "two blueprints of the same recipe are the same item");
        List<ItemStack> other = new ArrayList<>(sticks().pattern());
        other.set(0, new ItemStack(Items.BIRCH_PLANKS));
        assertNotEquals(sticks(), new Blueprint(other, sticks().recipe(), sticks().result()));
    }

    @Test
    void aShapedRecipeIsRecordedFromTheTopLeft() {
        Ingredient planks = Ingredient.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS);
        ShapedRecipe shaped = new ShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(Map.of('#', planks, 'S', Ingredient.of(Items.STICK)), "#S", " #"), new ItemStack(Items.LADDER, 3));
        ResourceLocation id = ResourceLocation.withDefaultNamespace("test_shaped");
        Blueprint bp = Blueprint.of(new RecipeHolder<CraftingRecipe>(id, shaped), ACCESS);
        assertNotNull(bp);
        assertEquals(id, bp.recipe());
        assertTrue(ItemStack.matches(new ItemStack(Items.LADDER, 3), bp.result()));
        Item[] expected = {Items.OAK_PLANKS, Items.STICK, null, null, Items.OAK_PLANKS, null, null, null, null};
        for (int i = 0; i < Blueprint.SIZE; ++i) {
            if (expected[i] == null) {
                assertTrue(bp.ghost(i).isEmpty(), "slot " + i + " is empty");
            } else {
                assertTrue(bp.ghost(i).is(expected[i]), "slot " + i + " is the ingredient's first item");
            }
        }
    }

    @Test
    void aShapelessRecipeTakesOneSlotPerIngredientInOrder() {
        ShapelessRecipe shapeless = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.MUSHROOM_STEW),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.BOWL), Ingredient.of(Items.RED_MUSHROOM), Ingredient.of(Items.BROWN_MUSHROOM)));
        Blueprint bp = Blueprint.of(new RecipeHolder<CraftingRecipe>(ResourceLocation.withDefaultNamespace("test_shapeless"), shapeless), ACCESS);
        assertNotNull(bp);
        assertTrue(bp.ghost(0).is(Items.BOWL));
        assertTrue(bp.ghost(1).is(Items.RED_MUSHROOM));
        assertTrue(bp.ghost(2).is(Items.BROWN_MUSHROOM));
        for (int i = 3; i < Blueprint.SIZE; ++i) {
            assertTrue(bp.ghost(i).isEmpty());
        }
    }

    @Test
    void recipesWithoutAFixedLayoutAreNotRecorded() {
        ShapedRecipe wide = new ShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.STONE)), "####"), new ItemStack(Items.STONE_BRICKS));
        assertFalse(Blueprint.recordable(wide), "four wide does not fit the matrix");
        assertNull(Blueprint.of(new RecipeHolder<CraftingRecipe>(ResourceLocation.withDefaultNamespace("wide"), wide), ACCESS));
        CraftingRecipe special = new MapCloningRecipe(CraftingBookCategory.MISC);
        assertFalse(Blueprint.recordable(special), "a special recipe has no layout");
        assertNull(Blueprint.of(new RecipeHolder<>(ResourceLocation.withDefaultNamespace("map_cloning"), special), ACCESS));
    }
}
