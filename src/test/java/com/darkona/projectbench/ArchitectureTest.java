package com.darkona.projectbench;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureTest {
    private static final String BASE = "com/darkona/projectbench/";
    private static final Pattern MOD_CLASS = Pattern.compile(Pattern.quote(BASE) + "[\\w/$]+");
    private static final Pattern CLIENT_CLASS = Pattern.compile("(net/minecraft/client/|com/mojang/blaze3d/|" + Pattern.quote(BASE) + "(client/|ProjectBenchClient\\b))[\\w/$]*");

    private static final String COMMON = "common";
    private static final Map<String, Set<String>> MAY_NAME = Map.of(
            COMMON, Set.of(),
            "block", Set.of("config"),
            "blueprint", Set.of("config", "registry"),
            "block.archive", Set.of("block", "blueprint", "config", "registry"),
            "block.bench", Set.of("block", "block.archive", "blueprint", "config", "network", "registry"),
            "config", Set.of("", "block", "registry"),
            "network", Set.of("", "block.bench", "blueprint"),
            "registry", Set.of("", "block", "block.archive", "block.bench", "blueprint", "config"));
    private static final Map<String, Set<String>> EXCEPTIONS = Map.of(
            "block/archive/BlueprintArchiveMenu", Set.of("block.bench"),
            "config/Feature", Set.of("block.bench"));
    private static final Set<String> CLIENT_COMPAT = Set.of("compat.emi", "compat.jei", "compat.polymorph", "compat.xei");

    private static final Map<String, Set<String>> NAMES = new TreeMap<>();
    private static final Map<String, Set<String>> CLIENT_NAMES = new TreeMap<>();
    private static final Set<String> GAMETEST = new TreeSet<>();

    @BeforeAll
    static void read() throws IOException {
        read(Path.of(System.getProperty("projectbench.classes", "build/classes/java/main")), null);
        read(Path.of(System.getProperty("projectbench.gametestClasses", "build/classes/java/gametest")), GAMETEST);
        assertTrue(NAMES.containsKey("ProjectBench"), "the entry point among " + NAMES.size() + " classes");
        assertTrue(!GAMETEST.isEmpty(), "the GameTests' classes");
    }

    private static void read(Path classes, @Nullable Set<String> into) throws IOException {
        Path root = classes.resolve(BASE);
        assertTrue(Files.isDirectory(root), "compiled classes at " + root);
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                String name = root.relativize(file).toString().replace('\\', '/');
                name = name.substring(0, name.length() - ".class".length());
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                Set<String> named = new TreeSet<>();
                for (Matcher m = MOD_CLASS.matcher(bytes); m.find(); ) {
                    named.add(m.group().substring(BASE.length()));
                }
                named.remove(name);
                NAMES.put(name, named);
                Set<String> client = new TreeSet<>();
                for (Matcher m = CLIENT_CLASS.matcher(bytes); m.find(); ) {
                    client.add(m.group());
                }
                CLIENT_NAMES.put(name, client);
                if (into != null) {
                    into.add(name);
                }
            }
        }
    }

    private static String zone(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash).replace('/', '.');
    }

    private static String topLevel(String name) {
        int dollar = name.indexOf('$');
        return dollar < 0 ? name : name.substring(0, dollar);
    }

    private static void assertNone(List<String> wrong) {
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
    }

    @Test
    void eachZoneNamesOnlyWhatItMay() {
        List<String> wrong = new ArrayList<>();
        NAMES.forEach((name, named) -> {
            String from = zone(name);
            Set<String> allowed = MAY_NAME.get(from);
            if (allowed == null) {
                return;
            }
            Set<String> excepted = EXCEPTIONS.getOrDefault(topLevel(name), Set.of());
            for (String other : named) {
                String to = zone(other);
                if (!to.equals(from) && !to.equals(COMMON) && !allowed.contains(to) && !excepted.contains(to)) {
                    wrong.add(name + " (" + from + ") names " + other);
                }
            }
        });
        assertNone(wrong);
    }

    @Test
    void onlyTheClientSideNamesClientCode() {
        List<String> wrong = new ArrayList<>();
        CLIENT_NAMES.forEach((name, client) -> {
            String zone = zone(name);
            boolean clientSide = zone.equals("client") || CLIENT_COMPAT.contains(zone) || topLevel(name).equals("ProjectBenchClient");
            if (!clientSide && !client.isEmpty()) {
                wrong.add(name + " names " + client);
            }
        });
        assertNone(wrong);
    }

    @Test
    void onlyTheEntryPointsNameCompatAndDatagen() {
        List<String> wrong = new ArrayList<>();
        NAMES.forEach((name, named) -> {
            String from = zone(name);
            String entry = topLevel(name);
            for (String other : named) {
                String to = zone(other);
                if (to.equals(from)) {
                    continue;
                }
                boolean compat = to.startsWith("compat.") && !entry.equals("ProjectBench") && !entry.equals("ProjectBenchClient");
                boolean harness = to.equals("datagen") && !entry.equals("ProjectBench");
                if (compat || harness) {
                    wrong.add(name + " names " + other);
                }
            }
        });
        assertNone(wrong);
    }

    @Test
    void theModNamesNoGameTests() {
        List<String> wrong = new ArrayList<>();
        NAMES.forEach((name, named) -> {
            boolean gametest = GAMETEST.contains(name);
            if (gametest != zone(name).equals("gametest")) {
                wrong.add(name + (gametest ? " is a GameTest class outside the gametest package" : " is a class of the mod in the gametest package"));
            }
            if (!gametest) {
                for (String other : named) {
                    if (GAMETEST.contains(other) || zone(other).equals("gametest")) {
                        wrong.add(name + " names " + other);
                    }
                }
            }
        });
        assertNone(wrong);
    }

    @Test
    void noClassNameHasAnSPrefix() {
        List<String> wrong = new ArrayList<>();
        for (String name : NAMES.keySet()) {
            for (String simple : name.substring(name.lastIndexOf('/') + 1).split("\\$")) {
                if (simple.length() > 1 && simple.charAt(0) == 'S' && Character.isUpperCase(simple.charAt(1))) {
                    wrong.add(name);
                }
            }
        }
        assertNone(wrong);
    }
}
