package com.darkona.projectbench.client;

import com.darkona.projectbench.config.ClientConfig;
import dev.darkona.jdlib.api.render.client.JDColors;
import dev.darkona.jdlib.api.ui.JDPalette;
import dev.darkona.jdlib.api.ui.JDTint;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class BlockTint {
    public static final int PLAIN = JDTint.NONE;
    public static final int USUAL_TEXT = ClientConfig.TintLine.USUAL_TEXT;
    public static final long NONE = pack(PLAIN, USUAL_TEXT);
    private static final long UNKNOWN = Long.MIN_VALUE;

    private static final Reference2LongOpenHashMap<Item> BY_ITEM = new Reference2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<ResourceLocation> LISTED = new Object2LongOpenHashMap<>();
    private static int seenLoads = -1;
    private static int seenPalette = -1;

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
        int palette = JDPalette.generation();
        if (loads != seenLoads || palette != seenPalette) {
            seenLoads = loads;
            seenPalette = palette;
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
        int named = JDPalette.get(paletteKey(BuiltInRegistries.BLOCK.getKey(block)), JDTint.NONE);
        if (named != JDTint.NONE) {
            return pack(named, USUAL_TEXT);
        }
        return ClientConfig.tintFromTexture() ? fromTexture(block) : NONE;
    }

    static ResourceLocation paletteKey(ResourceLocation block) {
        return ResourceLocation.fromNamespaceAndPath(block.getNamespace(), "projectbench_window/" + block.getPath());
    }

    private static long fromTexture(Block block) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState());
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return NONE;
        }
        int rgb = JDColors.average(sprite, PLAIN);
        return rgb == PLAIN ? NONE : pack(soften(rgb), USUAL_TEXT);
    }

    static final int SLOT_LIGHTEN = 40;

    public static int slotFill(int rgb) {
        return JDTint.slotInside(rgb, SLOT_LIGHTEN);
    }

    static int soften(int rgb) {
        return ARGB32.average(rgb, 0xFFFFFF);
    }
}
