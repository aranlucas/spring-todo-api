package com.aranlucas.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class TodosControllerTests {

    @Mock private TodoService todoService;

    @Mock private OidcUser principal;

    private TodosController controller;

    @BeforeEach
    void setUp() {
        controller = new TodosController(todoService);
        when(principal.getEmail()).thenReturn("test@test.com");
    }

    @Test
    void returnsOnlyTheAuthenticatedUsersTodos() {
        var todo = new Todo("Buy milk", "test@test.com");
        when(todoService.findAll(eq("test@test.com"), any()))
                .thenReturn(new PageImpl<>(List.of(todo)));

        var result = controller.allTodos(PageRequest.of(0, 5), principal);

        assertThat(result.getContent())
                .extracting(TodoResponse::content)
                .containsExactly("Buy milk");
        verify(todoService).findAll(eq("test@test.com"), any());
    }

    @Test
    void createsTodoForTheAuthenticatedUser() {
        var savedTodo = new Todo("Ship the API", "test@test.com");
        when(todoService.save(any(Todo.class))).thenReturn(savedTodo);

        var result = controller.newTodo(new CreateTodoRequest("Ship the API"), principal);

        assertThat(result.content()).isEqualTo("Ship the API");
        verify(todoService).save(any(Todo.class));
    }

    @Test
    void doesNotReturnAnotherUsersTodo() {
        when(todoService.findById(42L, "test@test.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> controller.getTodo(42L, principal))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void deletesTodoForTheAuthenticatedUser() {
        controller.deleteTodo(42L, principal);

        verify(todoService).deleteById(42L, "test@test.com");
    }
}
