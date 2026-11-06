package com.aranlucas.todo.todo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
@Tag(name = "Todos", description = "Todo management APIs")
public class TodosController {

    private final TodoService todoService;

    public TodosController(TodoService todoService) {
        this.todoService = todoService;
    }

    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidSortProperty() {}

    @GetMapping
    @Operation(
            summary = "Retrieve all todos",
            description = "Retrieve the authenticated user's todos")
    public Page<TodoResponse> allTodos(
            @SortDefault(sort = "id") Pageable pageable,
            @AuthenticationPrincipal OidcUser principal) {
        return todoService.findAll(ownerEmail(principal), pageable).map(TodoResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new todo",
            description = "Create a todo for the authenticated user")
    public TodoResponse newTodo(
            @Valid @RequestBody CreateTodoRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return TodoResponse.from(todoService.create(request.content(), ownerEmail(principal)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a todo", description = "Get one of the authenticated user's todos")
    public TodoResponse getTodo(
            @PathVariable Long id, @AuthenticationPrincipal OidcUser principal) {
        return todoService
                .findById(id, ownerEmail(principal))
                .map(TodoResponse::from)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    private static String ownerEmail(OidcUser principal) {
        String email = principal.getEmail();
        if (email == null || email.isBlank() || email.length() > 255) {
            throw new AccessDeniedException("A usable email claim is required for todo access");
        }
        return email;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a todo",
            description = "Delete one of the authenticated user's todos")
    public void deleteTodo(@PathVariable Long id, @AuthenticationPrincipal OidcUser principal) {
        todoService.deleteById(id, ownerEmail(principal));
    }
}
