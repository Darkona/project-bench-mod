package com.darkona.projectbench;

import com.darkona.projectbench.index.BenchBlockEntityTypes;
import com.darkona.projectbench.index.BenchDataComponents;
import com.darkona.projectbench.index.BenchItems;
import com.darkona.projectbench.index.BenchMenuTypes;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
import com.darkona.projectbench.content.bench.BlockTint;
import com.darkona.projectbench.content.archive.BlueprintArchiveView;
import com.darkona.projectbench.content.bench.ProjectBenchRenderer;
import com.darkona.projectbench.content.bench.ProjectBenchView;
import com.darkona.projectbench.compat.polymorph.PolymorphCompat;
import com.darkona.projectbench.compat.xei.BenchXei;
import dev.darkona.jdlib.api.ui.client.JDMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import dev.darkona.jdlib.api.compat.Bridges;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ProjectBench.MOD_ID, dist = Dist.CLIENT)
public final class ProjectBenchClient {
    public ProjectBenchClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            JDMenus.register(e, BenchMenuTypes.PROJECT_BENCH, new ProjectBenchView());
            JDMenus.register(e, BenchMenuTypes.BLUEPRINT_ARCHIVE, new BlueprintArchiveView());
        });
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ProjectBenchView.SWITCH_MATRIX));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerBlockEntityRenderer(BenchBlockEntityTypes.PROJECT_BENCH.get(), ProjectBenchRenderer::new));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> ItemProperties.register(BenchItems.BLUEPRINT.get(),
                ProjectBench.id("recorded"), (stack, level, entity, seed) -> BlueprintItem.blueprint(stack) == null ? 0F : 1F)));
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) manager -> BlockTint.forget()));

        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> {
            ItemContainerContents contents = e.getItemStack().get(BenchDataComponents.CONTENTS);
            if (contents != null) {
                e.getToolTip().add(Component.translatable("tooltip." + ProjectBench.MOD_ID + ".contents", contents.nonEmptyStream().count()).withStyle(ChatFormatting.GRAY));
            }
        });

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            BlockTint.forget();
        });

        BenchXei.register();

        Bridges.ifLoaded(PolymorphCompat.MOD_ID, () -> PolymorphCompat.init(modBus));
    }
}
