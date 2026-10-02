package com.aranlucas.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TodosHttpIntegrationTests.LocalOidcConfiguration.class)
class TodosHttpIntegrationTests {

    private static final String OWNER = "owner@example.test";
    private static final String OTHER = "other@example.test";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TodoRepository repository;
    @Autowired private CacheManager cacheManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private Flyway flyway;
    @Autowired private StringRedisTemplate redis;

    @BeforeEach
    void resetTodos() {
        repository.deleteAll();
        cacheManager.getCache("todos").clear();
    }

    @Test
    void startsFromTheProductionMigration() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_CACHE_TYPE", matches = "redis")
    void usesTheProductionRedisCacheAndTenMinuteTtl() throws Exception {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        long id = createTodo(OWNER, "Redis serialization and expiry");
        var keys = redis.keys("todos::*");
        assertThat(keys).hasSize(1);
        assertThat(redis.getExpire(keys.iterator().next())).isBetween(590L, 600L);
        mvc.perform(get("/todos/{id}", id).with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Redis serialization and expiry"));
    }

    @Test
    void isolatesCachedReadsAndDeletesByOwner() throws Exception {
        long id = createTodo(OWNER, "Private todo");
        cacheManager.getCache("todos").clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertThat(statistics.isStatisticsEnabled()).isTrue();
        long statementsBeforeFirstRead = statistics.getPrepareStatementCount();

        mvc.perform(get("/todos/{id}", id).with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Private todo"))
                .andExpect(jsonPath("$.email").doesNotExist());
        long statementsAfterFirstRead = statistics.getPrepareStatementCount();
        assertThat(statementsAfterFirstRead).isGreaterThan(statementsBeforeFirstRead);
        mvc.perform(get("/todos/{id}", id).with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Private todo"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(statementsAfterFirstRead);

        mvc.perform(get("/todos/{id}", id).with(as(OTHER))).andExpect(status().isNotFound());
        mvc.perform(delete("/todos/{id}", id).with(as(OTHER)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/todos/{id}", id).with(as(OWNER))).andExpect(status().isOk());
        mvc.perform(delete("/todos/{id}", id).with(as(OWNER)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/todos/{id}", id).with(as(OWNER))).andExpect(status().isNotFound());
        mvc.perform(delete("/todos/{id}", id).with(as(OWNER)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void populatesTheOwnerCacheWhenCreatingATodo() throws Exception {
        long id = createTodo(OWNER, "Cached on creation");
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertThat(statistics.isStatisticsEnabled()).isTrue();
        long statementsAfterCreate = statistics.getPrepareStatementCount();

        mvc.perform(get("/todos/{id}", id).with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Cached on creation"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(statementsAfterCreate);
        mvc.perform(get("/todos/{id}", id).with(as(OTHER))).andExpect(status().isNotFound());
    }

    @Test
    void listsOnlyTheOwnersTodosInStablePages() throws Exception {
        long first = createTodo(OWNER, "First");
        createTodo(OTHER, "Hidden");
        long second = createTodo(OWNER, "Second");

        mvc.perform(get("/todos").param("size", "1").with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first))
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
        mvc.perform(get("/todos").param("page", "1").param("size", "1").with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second));
        mvc.perform(get("/todos").param("size", "1000").with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"{}", "{\"content\":null}", "{\"content\":\"\"}", "{\"content\":\"   \"}"})
    void rejectsMissingOrBlankContentBeforeSaving(String body) throws Exception {
        mvc.perform(
                        post("/todos")
                                .with(as(OWNER))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void enforcesContentLengthAtTheHttpInterface() throws Exception {
        mvc.perform(
                        post("/todos")
                                .with(as(OWNER))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateTodoRequest("x".repeat(256)))))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
        createTodo(OWNER, "x".repeat(255));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void requiresLoginForTodoReads() throws Exception {
        mvc.perform(get("/todos")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/todos/42")).andExpect(status().is3xxRedirection());
    }

    @Test
    void requiresCsrfForAuthenticatedMutations() throws Exception {
        mvc.perform(
                        post("/todos")
                                .with(as(OWNER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"Rejected\"}"))
                .andExpect(status().isForbidden());
        assertThat(repository.count()).isZero();

        long id = createTodo(OWNER, "Keep me");
        mvc.perform(delete("/todos/{id}", id).with(as(OWNER))).andExpect(status().isForbidden());
        mvc.perform(delete("/todos/{id}", id).with(as(OWNER)).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/todos/{id}", id).with(as(OWNER))).andExpect(status().isOk());
    }

    @Test
    void returnsNotFoundForUnknownIds() throws Exception {
        mvc.perform(get("/todos/{id}", Long.MAX_VALUE).with(as(OWNER)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/todos/{id}", Long.MAX_VALUE).with(as(OWNER)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private long createTodo(String owner, String content) throws Exception {
        var response =
                mvc.perform(
                                post("/todos")
                                        .with(as(owner))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new CreateTodoRequest(content))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.content").value(content))
                        .andExpect(jsonPath("$.email").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private static RequestPostProcessor as(String email) {
        return oidcLogin().idToken(token -> token.subject(email).claim("email", email));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalOidcConfiguration {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(
                    ClientRegistration.withRegistrationId("auth0")
                            .clientId("local-test-client")
                            .clientSecret("local-test-secret")
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                            .scope("openid", "profile", "email")
                            .authorizationUri("http://auth0.invalid/authorize")
                            .tokenUri("http://auth0.invalid/token")
                            .jwkSetUri("http://auth0.invalid/jwks")
                            .userInfoUri("http://auth0.invalid/userinfo")
                            .userNameAttributeName("sub")
                            .build());
        }
    }
}
