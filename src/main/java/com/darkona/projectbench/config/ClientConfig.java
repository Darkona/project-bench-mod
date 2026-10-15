package com.darkona.projectbench.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ClientConfig {
    private static final String LEFT_TINT_DEFAULT = "#d4a017";
    private static final String RIGHT_TINT_DEFAULT = "#800020";
    private static final List<String> TINTS_DEFAULT = defaultTints();

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue JEI;
    private static final ModConfigSpec.BooleanValue EMI;
    private static final ModConfigSpec.BooleanValue JADE;
    private static final ModConfigSpec.BooleanValue POLYMORPH;
    private static final ModConfigSpec.BooleanValue MOUSE_TWEAKS;
    private static final ModConfigSpec.ConfigValue<String> LEFT_TINT;
    private static final ModConfigSpec.ConfigValue<String> RIGHT_TINT;
    private static final ModConfigSpec.BooleanValue TINT_WINDOWS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TINTS;
    private static final ModConfigSpec.BooleanValue TINT_FROM_TEXTURE;
    private static final ModConfigSpec.BooleanValue SHOWCASE;
    private static final ModConfigSpec.EnumValue<ShowcaseParts> SHOWCASE_PARTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Other mods, when you have them installed. This is your own file: the server's rules are in projectbench-common.toml.")
                .push("compat");
        JEI = b.comment("JEI: the bench shows up as a crafting station, JEI's \"+\" lays a recipe out in the bench's grid, and a click on the arrow",
                "shows the crafting recipes. JEI also hides whatever the server switched off. Off, the plugin does nothing.").define("jei", true);
        EMI = b.comment("EMI: the bench shows up as a crafting station, EMI's fill button lays a recipe out in the grid, and EMI's lists",
                "stay off the window's panels. Off, the plugin does nothing.").define("emi", true);
        JADE = b.comment("Jade: look at a bench and the tooltip says which recipe it is set to make, never what it holds.",
                "Off, Jade's tooltip gets nothing added.").define("jade", true);
        POLYMORPH = b.comment("Polymorph: its recipe selector lists the recipes the bench's grid can make, instead of the window's own selector.",
                "Off, the bench keeps its own selector. Read once, when the game starts.").define("polymorph", true);
        MOUSE_TWEAKS = b.comment("Mouse Tweaks: its wheel follows the bench's order: from your inventory to the bench's inventory, then the grid",
                "(only slots that hold the item or whose blueprint ghost wants it); from the bench's inventory to the grid, the",
                "inventory beside the bench that Share points to, then you. Off, the wheel goes wherever Mouse Tweaks picks.")
                .define("mouseTweaks", true);
        b.pop();

        b.comment("The colours of the Project Station's window (the double bench).").push("station");
        LEFT_TINT = b.comment("The left half's grid and inventory rows get a see-through wash of this colour, as CSS hex (\"#rrggbb\").",
                "\"#ffffff\" or \"\" for none.").define("leftTint", LEFT_TINT_DEFAULT, ClientConfig::isTint);
        RIGHT_TINT = b.comment("The same for the right half.").define("rightTint", RIGHT_TINT_DEFAULT, ClientConfig::isTint);
        b.pop();

        b.comment("The colours of the windows and tabs of the inventories next to the bench (the panel on the right of its window).")
                .push("tints");
        TINT_WINDOWS = b.comment("Each inventory next to the bench gets its window and tab tinted: by the list below, and anything",
                "not in it by its own texture (fromTexture). Off, all plain.").define("windows", true);
        TINTS = b.comment("Blocks next to the bench and the colour of their window and tab, one per line: \"modid:block=#rrggbb\",",
                "or \"modid:block=#rrggbb,#rrggbb\" to pick the colour of the window's title too (with only one, the title stays the",
                "usual dark grey, which you can't read on a dark colour). Every shulker box and Iron Chests' chests come filled in;",
                "add any block of any mod you like, say \"quark:spruce_chest=#735531\" or \"quark:dark_oak_chest=#4f3218,#f0f0f0\".",
                "A block in this list wins over everything else; a line that is not written right is dropped.")
                .defineListAllowEmpty("list", TINTS_DEFAULT, () -> "modid:block=#rrggbb", ClientConfig::isTintLine);
        TINT_FROM_TEXTURE = b.comment("A block next to the bench that is not in the list takes a soft tint from its own texture,",
                "so a spruce chest from Quark looks spruce and a barrel looks like a barrel. Off, those stay plain.")
                .define("fromTexture", true);
        b.pop();

        b.comment("What you see on top of a bench whose window button has the showcase on (the server's rules are in bench.showcase).")
                .push("showcase");
        SHOWCASE = b.comment("Draw anything on top of the benches. Off, you see nothing there, whatever each bench's button says.")
                .define("enabled", true);
        SHOWCASE_PARTS = b.comment("What is drawn: BOTH, RESULT (only the floating result) or ITEMS (only the grid's items on the bench).")
                .defineEnum("show", ShowcaseParts.BOTH);
        b.pop();
        SPEC = b.build();
    }

    public enum ShowcaseParts {
        BOTH, RESULT, ITEMS
    }

    private static volatile int fileLoads;

    private ClientConfig() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((ModConfigEvent.Loading e) -> reload(e.getConfig().getSpec()));
        modBus.addListener((ModConfigEvent.Reloading e) -> reload(e.getConfig().getSpec()));
    }

    private static void reload(Object spec) {
        if (spec == SPEC) {
            leftArgb = tint(LEFT_TINT.get(), LEFT_TINT_DEFAULT);
            rightArgb = tint(RIGHT_TINT.get(), RIGHT_TINT_DEFAULT);
            ++fileLoads;
        }
    }

    public static int fileLoads() {
        return fileLoads;
    }

    public static boolean jei() {
        return !SPEC.isLoaded() || JEI.get();
    }

    public static boolean emi() {
        return !SPEC.isLoaded() || EMI.get();
    }

    public static boolean jade() {
        return !SPEC.isLoaded() || JADE.get();
    }

    public static boolean mouseTweaks() {
        return !SPEC.isLoaded() || MOUSE_TWEAKS.get();
    }

    public static boolean polymorph() {
        return !SPEC.isLoaded() || POLYMORPH.get();
    }

    public static boolean showcaseItems() {
        return !SPEC.isLoaded() || SHOWCASE.get() && SHOWCASE_PARTS.get() != ShowcaseParts.RESULT;
    }

    public static boolean showcaseResult() {
        return !SPEC.isLoaded() || SHOWCASE.get() && SHOWCASE_PARTS.get() != ShowcaseParts.ITEMS;
    }

    public static boolean tintWindows() {
        return !SPEC.isLoaded() || TINT_WINDOWS.get();
    }

    public static boolean tintFromTexture() {
        return !SPEC.isLoaded() || TINT_FROM_TEXTURE.get();
    }

    public static List<? extends String> tints() {
        return SPEC.isLoaded() ? TINTS.get() : TINTS_DEFAULT;
    }

    public record TintLine(ResourceLocation block, int window, int text) {
        public static final int USUAL_TEXT = 0;

        @Nullable
        public static TintLine parse(String line) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                return null;
            }
            String id = line.substring(0, eq).trim();
            ResourceLocation block = id.indexOf(':') > 0 ? ResourceLocation.tryParse(id) : null;
            if (block == null) {
                return null;
            }
            String colours = line.substring(eq + 1).trim();
            int comma = colours.indexOf(',');
            String window = comma < 0 ? colours : colours.substring(0, comma).trim();
            String text = comma < 0 ? null : colours.substring(comma + 1).trim();
            if (!isColour(window) || text != null && !isColour(text)) {
                return null;
            }
            return new TintLine(block, rgb(window), text == null ? USUAL_TEXT : 0xFF000000 | rgb(text));
        }

        private static boolean isColour(String s) {
            return !s.isEmpty() && isTint(s);
        }

        private static int rgb(String hex) {
            return Integer.parseInt(hex.substring(1), 16);
        }
    }

    private static final String LIGHT_TITLE = ",#f0f0f0";

    private static List<String> defaultTints() {
        Set<DyeColor> darkDyes = EnumSet.of(DyeColor.BLACK, DyeColor.GRAY, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.BROWN, DyeColor.GREEN,
                DyeColor.RED, DyeColor.CYAN);
        List<String> lines = new ArrayList<>(31);
        lines.add("minecraft:shulker_box=#976797" + LIGHT_TITLE);
        for (DyeColor dye : DyeColor.values()) {
            int rgb = towardsWhite(dye.getTextureDiffuseColor(), 35);
            lines.add(String.format(Locale.ROOT, "minecraft:%s_shulker_box=#%06x%s", dye.getSerializedName(), rgb, darkDyes.contains(dye) ? LIGHT_TITLE : ""));
        }
        String[][] ironChests = {{"iron", "#b4bac4"}, {"gold", "#ffd13b"}, {"diamond", "#4fd8d0"}, {"copper", "#d9794a" + LIGHT_TITLE},
                {"crystal", "#a8d8f5"}, {"obsidian", "#5a4a7a" + LIGHT_TITLE}, {"dirt", "#8a6a4a" + LIGHT_TITLE}};
        for (String[] tier : ironChests) {
            lines.add("ironchest:" + tier[0] + "_chest=" + tier[1]);
            lines.add("ironchest:trapped_" + tier[0] + "_chest=" + tier[1]);
        }
        return List.copyOf(lines);
    }

    public static int towardsWhite(int rgb, int percent) {
        return channelTowardsWhite(ARGB32.red(rgb), percent) << 16 | channelTowardsWhite(ARGB32.green(rgb), percent) << 8 | channelTowardsWhite(ARGB32.blue(rgb), percent);
    }

    private static int channelTowardsWhite(int channel, int percent) {
        return channel + (0xFF - channel) * percent / 100;
    }

    private static boolean isTintLine(Object o) {
        return o instanceof String s && TintLine.parse(s) != null;
    }

    private static final int TINT_ALPHA = 0x30;
    private static volatile int leftArgb = tint(LEFT_TINT_DEFAULT, LEFT_TINT_DEFAULT);
    private static volatile int rightArgb = tint(RIGHT_TINT_DEFAULT, RIGHT_TINT_DEFAULT);

    public static int leftTint() {
        return leftArgb;
    }

    public static int rightTint() {
        return rightArgb;
    }

    static int tint(String css, String fallback) {
        String hex = isTint(css) ? css : fallback;
        if (hex.isEmpty()) {
            return 0;
        }
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return rgb == 0xFFFFFF ? 0 : TINT_ALPHA << 24 | rgb;
    }

    static boolean isTint(Object o) {
        if (!(o instanceof String s)) {
            return false;
        }
        if (s.isEmpty()) {
            return true;
        }
        if (s.length() != 7 || s.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < 7; ++i) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
