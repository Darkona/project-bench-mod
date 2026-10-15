package com.darkona.projectbench.config;

import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public enum Feature {
    BENCH(s -> s.bench(), ModItems.PROJECT_BENCH),
    SLAB_BENCH(s -> s.bench() && s.slab(), ModItems.SLAB_PROJECT_BENCH),
    AUTOCRAFT(s -> s.bench() && s.autocraft(), ModItems.AUTO_PROJECT_BENCH),
    EJECTING(s -> s.bench() && s.autocraft(), ModItems.EJECTING_AUTO_PROJECT_BENCH),
    BLUEPRINT(s -> s.bench() && s.blueprint(), ModItems.BLUEPRINT),
    ARCHIVE(s -> s.bench() && s.blueprint() && s.archive(), ModItems.BLUEPRINT_ARCHIVE);

    private static final Feature[] VALUES = values();

    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private final Predicate<Config.Settings> test;
    private final Supplier<? extends ItemLike> item;

    Feature(Predicate<Config.Settings> test, Supplier<? extends ItemLike> item) {
        this.test = test;
        this.item = item;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean enabled() {
        return test.test(Config.get());
    }

    public boolean configured() {
        return test.test(Config.fromFile());
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
        if (block instanceof ProjectBenchBlock bench && bench.kind().automatic()) {
            return BENCH.enabled();
        }
        return !(block instanceof ProjectBenchBlock) && block != ModBlocks.BLUEPRINT_ARCHIVE.get() || allows(block.asItem());
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
}
