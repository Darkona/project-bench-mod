package com.darkona.projectbench;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.darkona.jdlib.testkit.ArchZones;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    private static final String ROOT = "com.darkona.projectbench";
    private static final JavaClasses MOD = ArchZones.importFolders("projectbench.classes");
    private static final JavaClasses GAMETESTS = ArchZones.importFolders("projectbench.gametestClasses");
    private static final Set<String> CLIENT_ENTRIES = Set.of(ROOT + ".ProjectBenchClient");
    private static final Set<String> CLIENT_COMPAT = Set.of(ROOT + ".compat.emi", ROOT + ".compat.jei", ROOT + ".compat.polymorph", ROOT + ".compat.xei");

    private static final DescribedPredicate<JavaClass> USES_CLIENT_CODE = new DescribedPredicate<>("use client code") {
        @Override
        public boolean test(JavaClass type) {
            return type.getPackageName().startsWith(ROOT) && type.getDirectDependenciesFromSelf().stream()
                    .map(d -> d.getTargetClass().getPackageName())
                    .anyMatch(p -> p.startsWith("net.minecraft.client") || p.startsWith("com.mojang.blaze3d"));
        }
    };

    private static final DescribedPredicate<JavaClass> RUNS_ONLY_ON_THE_CLIENT = new DescribedPredicate<>("run only on the client") {
        @Override
        public boolean test(JavaClass type) {
            String top = topLevel(type).getName();
            return USES_CLIENT_CODE.test(type) || USES_CLIENT_CODE.test(topLevel(type)) || CLIENT_ENTRIES.contains(top) || CLIENT_COMPAT.contains(type.getPackageName())
                    || type.getPackageName().startsWith(ROOT + ".index.client");
        }
    };

    private static JavaClass topLevel(JavaClass type) {
        JavaClass top = type;
        while (top.getEnclosingClass().isPresent()) {
            top = top.getEnclosingClass().get();
        }
        return top;
    }

    @Test
    void registriesAreCreatedOnlyInIndexAndCompat() {
        noClasses().that().resideOutsideOfPackages(ROOT + ".index..", ROOT + ".compat..")
                .should().callMethodWhere(new DescribedPredicate<JavaMethodCall>("DeferredRegister.create*") {
                    @Override
                    public boolean test(JavaMethodCall call) {
                        return call.getTargetOwner().isAssignableTo(DeferredRegister.class) && call.getName().startsWith("create");
                    }
                })
                .check(MOD);
    }

    @Test
    void configSpecsLiveOnlyInConfig() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".config..")
                .should().dependOnClassesThat().belongToAnyOf(ModConfigSpec.class)
                .check(MOD);
    }

    @Test
    void dataProvidersLiveOnlyInData() {
        classes().that().areAssignableTo(DataProvider.class).or().areAssignableTo(LootTableSubProvider.class)
                .should().resideInAPackage(ROOT + ".data..")
                .check(MOD);
        noClasses().that().resideOutsideOfPackage(ROOT + ".data..").and().doNotHaveFullyQualifiedName(ROOT + ".ProjectBench")
                .should().dependOnClassesThat().belongToAnyOf(GatherDataEvent.class)
                .check(MOD);
    }

    @Test
    void payloadsLiveOnlyInNetwork() {
        classes().that().implement(CustomPacketPayload.class)
                .should().resideInAPackage(ROOT + ".network..")
                .check(MOD);
        noClasses().that().resideOutsideOfPackage(ROOT + ".network..")
                .should().dependOnClassesThat().belongToAnyOf(RegisterPayloadHandlersEvent.class)
                .check(MOD);
    }

    @Test
    void noServerClassNamesAClassThatUsesClientCode() {
        noClasses().that(DescribedPredicate.not(RUNS_ONLY_ON_THE_CLIENT))
                .should().dependOnClassesThat(USES_CLIENT_CODE)
                .check(MOD);
    }

    @Test
    void onlyTheEntryPointsNameCompatAndData() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".compat..")
                .and().doNotHaveFullyQualifiedName(ROOT + ".ProjectBench")
                .and().doNotHaveFullyQualifiedName(ROOT + ".ProjectBenchClient")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".compat..")
                .check(MOD);
        noClasses().that().resideOutsideOfPackage(ROOT + ".data..")
                .and().doNotHaveFullyQualifiedName(ROOT + ".ProjectBench")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".data..")
                .check(MOD);
    }

    @Test
    void theGameTestsLiveInInfrastructureAndTheModNamesNone() {
        classes().should().resideInAPackage(ROOT + ".infrastructure.gametest..").check(GAMETESTS);
        noClasses().should().dependOnClassesThat().resideInAPackage(ROOT + ".infrastructure.gametest..").check(MOD);
    }

    @Test
    void noClassNameHasAnSPrefix() {
        classes().should(new ArchCondition<>("not start with an S prefix") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                String simple = type.getSimpleName();
                boolean prefixed = simple.length() > 1 && simple.charAt(0) == 'S' && Character.isUpperCase(simple.charAt(1));
                events.add(new SimpleConditionEvent(type, !prefixed, type.getName()));
            }
        }).check(MOD);
    }
}
