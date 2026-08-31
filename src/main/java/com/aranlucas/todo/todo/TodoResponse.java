package com.aranlucas.todo.todo;

public record TodoResponse(Long id, String content) {

    static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getContent());
    }
}
