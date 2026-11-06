package com.aranlucas.todo;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class TodoApplicationTests {

    @Test
    void applicationModulesAreValid() {
        ApplicationModules.of(TodoApplication.class).verify();
    }
}
