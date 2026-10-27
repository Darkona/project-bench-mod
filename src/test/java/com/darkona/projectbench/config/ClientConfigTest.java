package com.darkona.projectbench.config;

import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigTest {
    @Test
    void whatEachPlayerSeesLivesInTheClientFile() {
        for (String key : List.of("compat.jei", "compat.emi", "compat.jade", "compat.polymorph", "compat.mouseTweaks", "station.leftTint", "station.rightTint",
                "tints.windows", "tints.list", "tints.fromTexture")) {
            assertTrue(ClientConfig.SPEC.getSpec().contains(key), "the client file has " + key);
        }
        for (String key : List.of("compat", "bench.slab.leftTint", "bench.slab.rightTint", "bench.external.tintWindows",
                "bench.external.tints", "bench.external.tintFromTexture")) {
            assertFalse(BenchConfig.SPEC.getSpec().contains(key), "the common file no longer has " + key);
        }
    }

    @Test
    void beforeTheFileLoadsEverythingIsOn() {
        assertTrue(ClientConfig.jei() && ClientConfig.emi() && ClientConfig.jade() && ClientConfig.polymorph() && ClientConfig.mouseTweaks(), "every compat on");
        assertTrue(ClientConfig.tintWindows() && ClientConfig.tintFromTexture(), "tinted windows, by the list and by texture");
        assertEquals(31, ClientConfig.tints().size(), "17 shulker boxes and 14 Iron Chests' chests");
        assertEquals(0x30D4A017, ClientConfig.leftTint(), "the left half gold");
        assertEquals(0x30800020, ClientConfig.rightTint(), "the right half burgundy");
    }

    @Test
    void stationTints() {
        assertEquals(0x30123456, ClientConfig.tint("#123456", "#d4a017"));
        assertEquals(0, ClientConfig.tint("#ffffff", "#d4a017"), "white is no tint");
        assertEquals(0, ClientConfig.tint("", "#d4a017"), "empty is no tint");
        assertEquals(0x30D4A017, ClientConfig.tint("gold", "#d4a017"), "a colour not written right falls back");
        assertFalse(ClientConfig.isTint("#12345"));
        assertFalse(ClientConfig.isTint("#12345g"));
        assertTrue(ClientConfig.isTint("#ABCdef"));
    }

    @Test
    void towardsWhiteKeepsTheOldInts() {
        for (int c = 0; c < 256; ++c) {
            assertEquals((c * 65 + 0xFF * 35) / 100 * 0x010101, ClientConfig.towardsWhite(c * 0x010101, 35), "35 %, the default tints: " + c);
            assertEquals((c + (0xFF - c) * 40 / 100) * 0x010101, ClientConfig.towardsWhite(0xFF000000 | c * 0x010101, 40),
                    "40 %, a tinted panel's slots (opaque, the alpha dropped): " + c);
        }
        for (DyeColor dye : DyeColor.values()) {
            int diffuse = dye.getTextureDiffuseColor();
            int old = oldTowardsWhite(diffuse >> 16 & 0xFF) << 16 | oldTowardsWhite(diffuse >> 8 & 0xFF) << 8
                    | oldTowardsWhite(diffuse & 0xFF);
            String line = String.format(Locale.ROOT, "minecraft:%s_shulker_box=#%06x", dye.getSerializedName(), old);
            assertTrue(ClientConfig.tints().stream().anyMatch(l -> l.startsWith(line)), "the default line of " + dye + ": " + line);
        }
    }

    private static int oldTowardsWhite(int channel) {
        return (channel * 65 + 0xFF * 35) / 100;
    }
}
