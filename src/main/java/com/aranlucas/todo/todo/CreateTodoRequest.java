package com.aranlucas.todo.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
        @Schema(description = "The text of the todo", example = "Ship the API")
                @NotBlank @Size(max = 255) String content) {}
