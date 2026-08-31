package com.aranlucas.todo.todo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoServiceTests {

    @Mock private TodoRepository todoRepository;

    @InjectMocks private TodoService todoService;

    @Test
    void rejectsDeletingAnotherUsersTodo() {
        when(todoRepository.deleteByIdAndEmail(42L, "test@test.com")).thenReturn(0L);

        assertThatThrownBy(() -> todoService.deleteById(42L, "test@test.com"))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void deletesOnlyTheAuthenticatedUsersTodo() {
        when(todoRepository.deleteByIdAndEmail(42L, "test@test.com")).thenReturn(1L);

        todoService.deleteById(42L, "test@test.com");

        verify(todoRepository).deleteByIdAndEmail(42L, "test@test.com");
    }
}
