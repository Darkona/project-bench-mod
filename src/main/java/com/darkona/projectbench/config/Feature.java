package com.darkona.projectbench.config;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.foundation.BenchBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

public enum Feature {
    BENCH(BenchConfig.Settings::bench, Content.Items.PROJECT_BENCH),
    SLAB_BENCH(s -> s.bench() && s.slab(), Content.Items.SLAB_PROJECT_BENCH),
    AUTOCRAFT(s -> s.bench() && s.autocraft(), Content.Items.AUTO_PROJECT_BENCH),
    EJECTING(s -> s.bench() && s.autocraft(), Content.Items.EJECTING_AUTO_PROJECT_BENCH),
    BLUEPRINT(s -> s.bench() && s.blueprint(), Content.Items.BLUEPRINT),
    ARCHIVE(s -> s.bench() && s.blueprint() && s.archive(), Content.Items.BLUEPRINT_ARCHIVE);

    private static final Feature[] VALUES = values();

    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private final Predicate<BenchConfig.Settings> test;
    private final Supplier<? extends ItemLike> item;

    Feature(Predicate<BenchConfig.Settings> test, Supplier<? extends ItemLike> item) {
        this.test = test;
        this.item = item;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean enabled() {
        return test.test(BenchConfig.get());
    }

    public boolean configured() {
        return test.test(BenchConfig.CONFIG.fromFile());
    }

    public Item item() {
        return item.get().asItem();
    }

    @Nullable
    public static Feature byKey(String key) {
        for (Feature f : VALUES) {
            if (f.key().equals(key)) {
                return f;
            }
        }
        return null;
    }

    @Nullable
    public static Feature of(Item item) {
        for (Feature f : VALUES) {
            if (f.item() == item) {
                return f;
            }
        }
        return null;
    }

    public static boolean allows(Item item) {
        Feature f = of(item);
        return f == null || f.enabled();
    }

    public static boolean allows(Block block) {
        return !(block instanceof BenchBlock<?> bench) || bench.feature().enabled();
    }

    public static void refuse(@Nullable Player player, ItemLike thing) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.projectbench.feature_disabled", thing.asItem().getDescription()), true);
        }
    }

    public static void listen(Runnable listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void unlisten(Runnable listener) {
        LISTENERS.remove(listener);
    }

    static void changed() {
        for (Runnable l : LISTENERS) {
            l.run();
        }
    }

    public static void guardUse() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Feature::onPlace);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Feature::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Feature::onRightClickItem);
    }

    private static void deny(ICancellableEvent event, @Nullable Player player, ItemLike thing) {
        event.setCanceled(true);
        refuse(player, thing);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getPlacedBlock();
        if (!Feature.allows(placed.getBlock()) || !Feature.allows(placed.getBlock().asItem())) {
            deny(event, event.getEntity() instanceof Player player ? player : null, placed.getBlock());
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !Feature.allows(held.getItem())) {
            event.setCancellationResult(InteractionResult.FAIL);
            deny(event, event.getEntity(), held.getItem());
            return;
        }
        BlockState clicked = event.getLevel().getBlockState(event.getPos());
        if (!Feature.allows(clicked.getBlock())) {
            event.setCancellationResult(InteractionResult.FAIL);
            deny(event, event.getEntity(), clicked.getBlock());
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !Feature.allows(held.getItem())) {
            event.setCancellationResult(InteractionResult.FAIL);
            deny(event, event.getEntity(), held.getItem());
        }
    }

    public record Condition(String feature) implements ICondition {
        public static final MapCodec<Condition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("feature").forGetter(Condition::feature)
        ).apply(i, Condition::new));

        public Condition(Feature feature) {
            this(feature.key());
        }

        @Override
        public boolean test(IContext context) {
            Feature f = Feature.byKey(feature);
            return f != null && f.configured();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
