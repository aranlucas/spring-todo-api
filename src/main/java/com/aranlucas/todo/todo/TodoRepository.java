package com.aranlucas.todo.todo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Page<Todo> findByEmail(String email, Pageable pageable);

    long deleteByIdAndEmail(Long id, String email);
}
