package com.aranlucas.todo;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class TodoApplicationTests {

    @Test
    void applicationModulesAreValid() {
        var modules = ApplicationModules.of(TodoApplication.class).verify();
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
