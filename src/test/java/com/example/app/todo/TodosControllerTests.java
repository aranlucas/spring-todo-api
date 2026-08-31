package com.example.app.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
public class TodosControllerTests {
    @Mock private TodoService repository;

    private TodosController controller;

    @BeforeEach
    void setup() {
        controller = new TodosController(repository);
    }

    @Test
    void testGetAll() {
        Pageable pageable = PageRequest.of(0, 5);

        List<Todo> list = new ArrayList<>();

        Page<Todo> result = new PageImpl<>(list);

        OidcUser user =
                new OidcUser() {
                    @Override
                    public Map<String, Object> getClaims() {
                        return Map.of("email", "test@test.com");
                    }

                    @Override
                    public OidcUserInfo getUserInfo() {
                        return null;
                    }

                    @Override
                    public OidcIdToken getIdToken() {
                        return null;
                    }

                    @Override
                    public Map<String, Object> getAttributes() {
                        return null;
                    }

                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
        when(repository.findAll(any(String.class), any(Pageable.class))).thenReturn(result);

        assertThat(controller.allTodos(pageable, user)).isEmpty();
    }
}
