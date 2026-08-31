package com.aranlucas.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class TodoRepositoryTests {

    @Autowired private TodoRepository todoRepository;

    @Test
    void scopesDeletesToTheTodoOwner() {
        var todo = todoRepository.save(new Todo("Private", "test@test.com"));

        assertThat(todoRepository.deleteByIdAndEmail(todo.getId(), "other@test.com")).isZero();
        assertThat(todoRepository.findByEmail("test@test.com", PageRequest.of(0, 10)).getContent())
                .hasSize(1);

        assertThat(todoRepository.deleteByIdAndEmail(todo.getId(), "test@test.com")).isEqualTo(1);
        assertThat(todoRepository.findById(todo.getId())).isEmpty();
    }
}
