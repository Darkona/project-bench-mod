package com.darkona.projectbench.data.recipe;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.content.bench.BenchUpgrade;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.Feature.Condition;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

public final class BenchStandardRecipeGen extends RecipeProvider {
    public BenchStandardRecipeGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        super(out, lookup);
    }

    private static RecipeOutput upgrades(RecipeOutput out) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                Recipe<?> upgrade = switch (recipe) {
                    case ShapedRecipe shaped -> new BenchUpgrade.Shaped(shaped);
                    case ShapelessRecipe shapeless -> new BenchUpgrade.Shapeless(shapeless);
                    default -> throw new IllegalArgumentException("not a bench upgrade: " + id);
                };
                out.accept(id, upgrade, advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return out.advancement();
            }
        };
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        RecipeOutput bench = out.withConditions(new Condition(Feature.BENCH));
        RecipeOutput slab = out.withConditions(new Condition(Feature.SLAB_BENCH));
        RecipeOutput blueprint = out.withConditions(new Condition(Feature.BLUEPRINT));
        RecipeOutput archive = out.withConditions(new Condition(Feature.ARCHIVE));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Content.Items.PROJECT_BENCH.get())
                .pattern("RGR").pattern("SBS").pattern("PCP")
                .define('R', Tags.Items.DUSTS_REDSTONE).define('G', Tags.Items.INGOTS_GOLD).define('S', Items.SMOOTH_STONE)
                .define('B', Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES).define('P', ItemTags.PLANKS).define('C', Tags.Items.CHESTS_WOODEN)
                .unlockedBy("has_crafting_table", has(Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)).save(bench);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Content.Items.SLAB_PROJECT_BENCH.get(), 2)
                           .pattern(" B ").pattern("III")
                           .define('B', Content.Items.PROJECT_BENCH.get()).define('I', Tags.Items.INGOTS_IRON)
                           .unlockedBy("has_project_bench", has(Content.Items.PROJECT_BENCH.get())).save(slab);
        RecipeOutput autocraft = upgrades(out.withConditions(new Condition(Feature.AUTOCRAFT)));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Content.Items.AUTO_PROJECT_BENCH.get())
                .pattern("IBI").pattern("RCR").pattern("PLP")
                .define('I', Tags.Items.INGOTS_IRON).define('B', Content.Items.PROJECT_BENCH.get()).define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Items.CRAFTER).define('P', Items.PISTON).define('L', Items.LEVER)
                .unlockedBy("has_project_bench", has(Content.Items.PROJECT_BENCH.get())).save(autocraft);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, Content.Items.EJECTING_AUTO_PROJECT_BENCH.get())
                .requires(Content.Items.AUTO_PROJECT_BENCH.get()).requires(Items.DROPPER)
                .unlockedBy("has_auto_project_bench", has(Content.Items.AUTO_PROJECT_BENCH.get())).save(autocraft);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Content.Items.BLUEPRINT.get(), 8)
                .requires(Tags.Items.GEMS_LAPIS).requires(Items.PAPER, 8)
                .unlockedBy("has_paper", has(Items.PAPER)).save(blueprint);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Content.Items.BLUEPRINT.get())
                              .requires(Content.Items.BLUEPRINT.get())
                              .unlockedBy("has_blueprint", has(Content.Items.BLUEPRINT.get())).save(blueprint, Content.id("blueprint_clear"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Content.Items.BLUEPRINT_ARCHIVE.get())
                           .pattern("LIL").pattern("SCS").pattern("PPP")
                           .define('L', Tags.Items.GEMS_LAPIS).define('I', Tags.Items.INGOTS_IRON).define('S', Items.SMOOTH_STONE)
                           .define('C', Tags.Items.CHESTS_WOODEN).define('P', ItemTags.PLANKS)
                           .unlockedBy("has_blueprint", has(Content.Items.BLUEPRINT.get())).save(archive);
    }
}
