package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.blueprint.BlueprintContainer;
import com.darkona.projectbench.config.Config;
import dev.darkona.jdlib.api.inventory.ConcatContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Opening(ProjectBenchBlockEntity first, @Nullable ProjectBenchBlockEntity second,
                      List<BlueprintArchiveBlockEntity> archives, List<ExternalInventories.Panel> outside) {
    public static final String STATION_TITLE = "gui.projectbench.project_bench.station";

    public static Opening of(ProjectBenchBlockEntity bench) {
        Level level = bench.getLevel();
        ProjectBenchBlock.Pair pair = level == null ? null : ProjectBenchBlock.pair(bench.getBlockState(), level, bench.getBlockPos());
        if (pair == null) {
            return new Opening(bench, null, bench.archives(), bench.externalPanels());
        }
        ProjectBenchBlockEntity first = pair.first();
        ProjectBenchBlockEntity second = pair.second();
        List<BlueprintArchiveBlockEntity> archives = first.archives();
        for (BlueprintArchiveBlockEntity archive : second.archives()) {
            if (!archives.contains(archive)) {
                archives.add(archive);
            }
        }
        List<ExternalInventories.Panel> outside = new ArrayList<>(first.externalPanels());
        for (ExternalInventories.Panel panel : second.externalPanels()) {
            if (!second.external().leftToPartner(panel.sideIndex())) {
                outside.add(panel.ofSecond());
            }
        }
        return new Opening(first, second, archives, outside);
    }

    public ProjectBenchMenu menu(int id, Inventory inventory) {
        return new ProjectBenchMenu(id, inventory, first, second, archives, outside);
    }

    public void open(ServerPlayer player, ProjectBenchBlockEntity clicked) {
        Component title = second == null ? clicked.getBlockState().getBlock().getName() : Component.translatable(STATION_TITLE);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> menu(id, inventory), title), buf -> {
            buf.writeBlockPos(first.getBlockPos());
            buf.writeVarInt(archives.size());
            buf.writeVarInt(outside.size());
            for (ExternalInventories.Panel panel : outside) {
                buf.writeVarInt(panel.slots());
                ComponentSerialization.STREAM_CODEC.encode(buf, panel.title());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, panel.icon());
                buf.writeBoolean(panel.handsOff());
                buf.writeByte(panel.processor() == null ? 0 : panel.processor().ordinal() + 1);
            }
            buf.writeByte(second == null ? 0 : 1);
            if (second != null) {
                buf.writeBlockPos(second.getBlockPos());
            }
            first.storage().writeLocks(buf);
            if (second != null) {
                second.storage().writeLocks(buf);
            }
        });
    }

    record Received(ProjectBenchBlockEntity first, @Nullable ProjectBenchBlockEntity second, ConcatContainer archives,
                    List<ExternalInventories.Panel> panels) {
        static Received read(Inventory inventory, RegistryFriendlyByteBuf buf) {
            ProjectBenchBlockEntity first = (ProjectBenchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos());
            ConcatContainer archives = clientArchives(buf.readVarInt());
            List<ExternalInventories.Panel> panels = readPanels(buf);
            int station = buf.readByte();
            ProjectBenchBlockEntity second = station == 0 ? null : (ProjectBenchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos());
            first.storage().readLocks(buf);
            if (second != null) {
                second.storage().readLocks(buf);
            }
            return new Received(first, second, archives, panels);
        }

        private static ConcatContainer clientArchives(int blocks) {
            List<Container> out = new ArrayList<>(blocks);
            for (int i = 0; i < blocks; ++i) {
                out.add(new BlueprintContainer(BlueprintArchiveBlockEntity.SLOTS, () -> Config.get().archiveStack()));
            }
            return new ConcatContainer(BlueprintArchiveBlockEntity.SLOTS, out);
        }

        private static List<ExternalInventories.Panel> readPanels(RegistryFriendlyByteBuf buf) {
            int panelCount = buf.readVarInt();
            List<ExternalInventories.Panel> panels = new ArrayList<>(panelCount);
            for (int i = 0; i < panelCount; ++i) {
                int slots = buf.readVarInt();
                Component title = ComponentSerialization.STREAM_CODEC.decode(buf);
                ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                boolean handsOff = buf.readBoolean();
                int processor = buf.readByte();
                panels.add(new ExternalInventories.Panel(-1, slots, title, icon, handsOff, processor == 0 ? null : Processor.values()[processor - 1]));
            }
            return panels;
        }
    }
}
