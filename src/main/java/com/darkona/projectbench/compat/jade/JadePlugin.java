package com.darkona.projectbench.compat.jade;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Feature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public final class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(RecipeProvider.INSTANCE, ProjectBenchBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(RecipeProvider.INSTANCE, ProjectBenchBlock.class);
    }

    private enum RecipeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ProjectBench.id("recipe");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!Feature.BENCH.enabled() || !(accessor.getBlockEntity() instanceof ProjectBenchBlockEntity bench)) {
                return;
            }
            if (bench.autocraftButton() != 0) {
                data.putBoolean(AUTOCRAFT, bench.autocraft());
            }
            ProjectBenchBlock.Pair pair = ProjectBenchBlock.pair(bench.getBlockState(), accessor.getLevel(), bench.getBlockPos());
            if (pair == null) {
                put(data, "", bench, accessor);
            } else {
                put(data, LEFT, pair.first(), accessor);
                put(data, RIGHT, pair.second(), accessor);
            }
        }

        private static final String LEFT = "Left";
        private static final String RIGHT = "Right";
        private static final String AUTOCRAFT = "Autocraft";

        private static void put(CompoundTag data, String prefix, ProjectBenchBlockEntity bench, BlockAccessor accessor) {
            ItemStack made = bench.result().getItem(0);
            if (bench.recipe() == null || made.isEmpty()) {
                return;
            }
            data.put(prefix + "Made", made.save(accessor.getLevel().registryAccess()));
            data.putInt(prefix + "Choice", bench.choice() + 1);
            data.putInt(prefix + "Recipes", bench.recipeCount());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!ClientConfig.jade()) {
                return;
            }
            CompoundTag data = accessor.getServerData();
            line(tooltip, accessor, data, 0, "", "jade.projectbench.recipe");
            line(tooltip, accessor, data, 1, LEFT, "jade.projectbench.recipe.left");
            line(tooltip, accessor, data, 2, RIGHT, "jade.projectbench.recipe.right");
            if (data.contains(AUTOCRAFT)) {
                tooltip.add(Component.translatable(data.getBoolean(AUTOCRAFT) ? "jade.projectbench.autocraft.on" : "jade.projectbench.autocraft.off"));
            }
        }

        private final CompoundTag[] parsedFrom = new CompoundTag[3];
        private final ItemStack[] parsed = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

        private void line(ITooltip tooltip, BlockAccessor accessor, CompoundTag data, int index, String prefix, String key) {
            if (!data.contains(prefix + "Made")) {
                return;
            }
            CompoundTag tag = data.getCompound(prefix + "Made");
            if (tag != parsedFrom[index]) {
                parsedFrom[index] = tag;
                parsed[index] = ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag);
            }
            ItemStack made = parsed[index];
            if (made.isEmpty()) {
                return;
            }
            tooltip.add(IElementHelper.get().smallItem(made));
            tooltip.append(Component.translatable(key, made.getCount(), made.getHoverName()));
            if (data.getInt(prefix + "Recipes") > 1) {
                tooltip.append(Component.translatable("jade.projectbench.recipe.of", data.getInt(prefix + "Choice"), data.getInt(prefix + "Recipes")));
            }
        }
    }
}
