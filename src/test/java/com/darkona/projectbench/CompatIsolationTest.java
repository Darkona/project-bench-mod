package com.darkona.projectbench;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatIsolationTest {
    private static final Map<String, String> MODS = Map.of(
            "mezz/jei/", "com/darkona/projectbench/compat/jei/",
            "com/illusivesoulworks/polymorph/", "com/darkona/projectbench/compat/polymorph/",
            "dev/emi/emi/", "com/darkona/projectbench/compat/emi/",
            "snownee/jade/", "com/darkona/projectbench/compat/jade/",
            "yalter/mousetweaks/", "com/darkona/projectbench/compat/mousetweaks/",
            "com/simibubi/create/", "com/darkona/projectbench/compat/create/");
    private static final String IPN = "org/anti_ad/";
    private static final String IPN_ANNOTATION = "Lorg/anti_ad/mc/ipn/api/IPNSlotsIgnoreForInventoryTypes;";

    @Test
    void onlyTheCompatPackagesNameTheOptionalMods() throws IOException {
        List<String> wrong = new ArrayList<>();
        check(Path.of(System.getProperty("projectbench.classes", "build/classes/java/main")), wrong);
        check(Path.of(System.getProperty("projectbench.gametestClasses", "build/classes/java/gametest")), wrong);
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
    }

    private static void check(Path root, List<String> wrong) throws IOException {
        assertTrue(Files.isDirectory(root), "compiled classes at " + root);
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                String name = root.relativize(file).toString().replace('\\', '/');
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                MODS.forEach((mod, zone) -> {
                    if (!name.startsWith(zone) && bytes.contains(mod)) {
                        wrong.add(name + " names " + mod);
                    }
                });
                if (bytes.replace(IPN_ANNOTATION, "").contains(IPN)) {
                    wrong.add(name + " names " + IPN + " beyond its annotation");
                }
            }
        }
    }
}
