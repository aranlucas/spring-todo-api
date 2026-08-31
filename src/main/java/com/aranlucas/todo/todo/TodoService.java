package com.aranlucas.todo.todo;

import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional(readOnly = true)
    public Page<Todo> findAll(String email, Pageable pageable) {
        return todoRepository.findByEmail(email, pageable);
    }

    @Transactional
    @CachePut(cacheNames = "todos", key = "#result.id")
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "todos", key = "#id")
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "todos", key = "#id")
    public void deleteById(Long id, String email) {
        if (todoRepository.deleteByIdAndEmail(id, email) == 0) {
            throw new TodoNotFoundException(id);
        }
    }
}
