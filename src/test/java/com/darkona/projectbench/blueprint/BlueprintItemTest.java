package com.darkona.projectbench.blueprint;

import com.darkona.projectbench.registry.ModComponents;
import com.darkona.projectbench.registry.ModItems;
import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintItemTest {
    private static Blueprint sticks() {
        List<ItemStack> pattern = new ArrayList<>(9);
        for (int i = 0; i < 9; ++i) {
            pattern.add(i == 1 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY);
        }
        return new Blueprint(pattern, ResourceLocation.withDefaultNamespace("stick"), new ItemStack(Items.STICK, 4));
    }

    @Test
    void equalRecipesStack() {
        ItemStack a = new ItemStack(ModItems.BLUEPRINT.get());
        ItemStack b = new ItemStack(ModItems.BLUEPRINT.get());
        assertNull(BlueprintItem.blueprint(a), "a new blueprint is blank");
        assertFalse(BlueprintItem.recorded(a));
        a.set(ModComponents.BLUEPRINT.get(), sticks());
        b.set(ModComponents.BLUEPRINT.get(), sticks());
        assertTrue(BlueprintItem.recorded(a));
        assertTrue(ItemStack.isSameItemSameComponents(a, b), "two blueprints of the same recipe are the same item");
        assertEquals(1, a.getMaxStackSize(), "a recorded blueprint does not stack");
        assertFalse(BlueprintItem.recorded(new ItemStack(Items.PAPER)));
    }
}
