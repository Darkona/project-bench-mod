package com.darkona.projectbench.client;

import com.darkona.projectbench.config.ClientConfig;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.IntBinaryOperator;

public final class BlockTint {
    public static final int PLAIN = -1;
    public static final int USUAL_TEXT = ClientConfig.TintLine.USUAL_TEXT;
    public static final long NONE = pack(PLAIN, USUAL_TEXT);
    private static final long UNKNOWN = Long.MIN_VALUE;

    private static final Reference2LongOpenHashMap<Item> BY_ITEM = new Reference2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<ResourceLocation> LISTED = new Object2LongOpenHashMap<>();
    private static int seenLoads = -1;

    static {
        BY_ITEM.defaultReturnValue(UNKNOWN);
        LISTED.defaultReturnValue(UNKNOWN);
    }

    private BlockTint() {
    }

    public static long of(Item icon) {
        if (!ClientConfig.tintWindows()) {
            return NONE;
        }
        int loads = ClientConfig.fileLoads();
        if (loads != seenLoads) {
            seenLoads = loads;
            BY_ITEM.clear();
            listed(ClientConfig.tints(), LISTED);
        }
        long tint = BY_ITEM.getLong(icon);
        if (tint == UNKNOWN) {
            tint = resolve(icon);
            BY_ITEM.put(icon, tint);
        }
        return tint;
    }

    public static int window(long tint) {
        return (int) tint;
    }

    public static int text(long tint) {
        return (int) (tint >>> 32);
    }

    static long pack(int window, int text) {
        return (long) text << 32 | window & 0xFFFFFFFFL;
    }

    public static void forget() {
        BY_ITEM.clear();
    }

    static void listed(List<? extends String> lines, Object2LongOpenHashMap<ResourceLocation> into) {
        into.clear();
        for (String line : lines) {
            ClientConfig.TintLine parsed = ClientConfig.TintLine.parse(line);
            if (parsed != null) {
                into.put(parsed.block(), pack(parsed.window(), parsed.text()));
            }
        }
    }

    private static long resolve(Item icon) {
        if (!(icon instanceof BlockItem item)) {
            return NONE;
        }
        Block block = item.getBlock();
        long listed = LISTED.getLong(BuiltInRegistries.BLOCK.getKey(block));
        if (listed != UNKNOWN) {
            return listed;
        }
        return ClientConfig.tintFromTexture() ? fromTexture(block) : NONE;
    }

    private static long fromTexture(Block block) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState());
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return NONE;
        }
        int rgb;
        try {
            rgb = average(sprite.contents().width(), sprite.contents().height(), (x, y) -> sprite.getPixelRGBA(0, x, y));
        } catch (IllegalArgumentException notRgba) {
            return NONE;
        }
        return rgb == PLAIN ? NONE : pack(soften(rgb), USUAL_TEXT);
    }

    static int average(int width, int height, IntBinaryOperator abgr) {
        long red = 0;
        long green = 0;
        long blue = 0;
        int count = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel = abgr.applyAsInt(x, y);
                if (ABGR32.alpha(pixel) >= 128) {
                    red += ABGR32.red(pixel);
                    green += ABGR32.green(pixel);
                    blue += ABGR32.blue(pixel);
                    ++count;
                }
            }
        }
        if (count == 0) {
            return PLAIN;
        }
        return (int) (red / count) << 16 | (int) (green / count) << 8 | (int) (blue / count);
    }

    static int soften(int rgb) {
        return ARGB32.average(rgb, 0xFFFFFF);
    }
}
