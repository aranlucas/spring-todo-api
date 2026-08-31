package com.aranlucas.todo.todo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "todos")
public class Todo {

    @Id @GeneratedValue private Long id;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false, length = 255)
    private String email;

    protected Todo() {}

    public Todo(String content, String email) {
        this.content = content;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getEmail() {
        return email;
    }
}
