package com.societix;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private static final Set<String> MODULES = Set.of(
            "treasury", "governance", "copilot", "gate", "privacy", "migration", "platform");

    @Test
    void eachModuleHasAnApplicationModulePackageMarker() throws Exception {
        Set<String> markedModules = MODULES.stream()
                .filter(ModularityTest::hasApplicationModuleMarker)
                .collect(Collectors.toSet());

        assertTrue(markedModules.containsAll(MODULES), "Every SOC-2 module must declare its Modulith boundary");
    }

    @Test
    void moduleBoundariesAreValid() {
        ApplicationModules.of(SocietixApplication.class).verify();
    }

    private static boolean hasApplicationModuleMarker(String moduleName) {
        try {
            Package modulePackage = Class.forName("com.societix." + moduleName + ".package-info").getPackage();
            return modulePackage.isAnnotationPresent(ApplicationModule.class);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
