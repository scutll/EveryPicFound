package com.everypicfound.identity.integration;

import com.everypicfound.identity.application.service.UserAccountRegistrationTransaction;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.support.security.TestRsaKeyMaterial;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(
        named = "EPF_TEST_MYSQL_ENABLED",
        matches = "true")
class UserRegistrationMySqlIntegrationTest {

    private static final JwtTestKeys JWT_TEST_KEYS = createJwtTestKeys();
    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-16T08:00:00.123456Z");
    private static final LocalDateTime STORED_AT =
            LocalDateTime.of(2026, 7, 16, 8, 0, 0, 123_000_000);
    private static final List<String> TEST_USERNAMES = List.of(
            "CaseUser",
            "caseuser",
            "DuplicateUser",
            "ConcurrentUser",
            "HttpUser01",
            "HttpDuplicate01",
            "LoginUser01",
            "BadStatus01",
            "BadVersion01");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> requiredEnvironment("EPF_TEST_MYSQL_URL"));
        registry.add(
                "spring.datasource.username",
                () -> requiredEnvironment("EPF_TEST_MYSQL_USERNAME"));
        registry.add(
                "spring.datasource.password",
                () -> requiredEnvironment("EPF_TEST_MYSQL_PASSWORD"));
        registry.add(
                "everypicfound.auth.password.bcrypt-strength",
                () -> "4");
        registry.add(
                "everypicfound.auth.jwt.private-key-location",
                () -> JWT_TEST_KEYS.privateKey().toUri().toString());
        registry.add(
                "everypicfound.auth.jwt.public-key-location",
                () -> JWT_TEST_KEYS.publicKey().toUri().toString());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRegistrationTransaction registrationTransaction;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    @AfterEach
    void removeTestAccounts() {
        String placeholders = String.join(
                ", ",
                TEST_USERNAMES.stream().map(ignored -> "?").toList());
        jdbcTemplate.update(
                "DELETE FROM user_account WHERE username IN ("
                        + placeholders + ")",
                TEST_USERNAMES.toArray());
    }

    @Test
    void flywayCreatesVersionedCaseSensitiveAccountSchema() {
        Integer successfulMigration = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '1' AND success = TRUE
                        """,
                Integer.class);
        String usernameCollation = jdbcTemplate.queryForObject(
                """
                        SELECT collation_name
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'user_account'
                          AND column_name = 'username'
                        """,
                String.class);
        List<String> checkConstraints = jdbcTemplate.queryForList(
                """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE table_schema = DATABASE()
                          AND table_name = 'user_account'
                          AND constraint_type = 'CHECK'
                        ORDER BY constraint_name
                        """,
                String.class);

        assertThat(successfulMigration).isEqualTo(1);
        assertThat(usernameCollation).isEqualTo("ascii_bin");
        assertThat(checkConstraints).containsExactly(
                "chk_user_account_status",
                "chk_user_account_version");
    }

    @Test
    void repositoryPreservesCaseSensitiveUsernamesAndUtcMilliseconds() {
        long upperCaseId = registrationTransaction.save(
                account("CaseUser"));
        long lowerCaseId = registrationTransaction.save(
                account("caseuser"));

        PersistedAccountRow row = jdbcTemplate.queryForObject(
                """
                        SELECT id, created_time, updated_time,
                               auth_valid_after
                        FROM user_account
                        WHERE username = 'CaseUser'
                        """,
                (resultSet, rowNumber) -> new PersistedAccountRow(
                        resultSet.getLong("id"),
                        resultSet.getObject(
                                "created_time", LocalDateTime.class),
                        resultSet.getObject(
                                "updated_time", LocalDateTime.class),
                        resultSet.getObject(
                                "auth_valid_after", LocalDateTime.class)));

        assertThat(upperCaseId).isPositive().isNotEqualTo(lowerCaseId);
        assertThat(lowerCaseId).isPositive();
        assertThat(userRepository.existsByUsername(
                Username.of("CaseUser"))).isTrue();
        assertThat(userRepository.existsByUsername(
                Username.of("caseuser"))).isTrue();
        assertThat(row).isNotNull();
        assertThat(row.id()).isEqualTo(upperCaseId);
        assertThat(row.createdTime()).isEqualTo(STORED_AT);
        assertThat(row.updatedTime()).isEqualTo(STORED_AT);
        assertThat(row.authValidAfter()).isEqualTo(STORED_AT);
    }

    @Test
    void repositoryConvertsExactDuplicateKeyToDomainConflict() {
        registrationTransaction.save(account("DuplicateUser"));

        assertThatThrownBy(() -> registrationTransaction.save(
                account("DuplicateUser")))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void uniqueIndexAllowsOnlyOneConcurrentInsert() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> insert = () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new IllegalStateException("start signal timed out");
            }
            try {
                return registrationTransaction.save(
                        account("ConcurrentUser"));
            } catch (RuntimeException exception) {
                return exception;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(insert);
            Future<Object> second = executor.submit(insert);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(Long.class::isInstance))
                    .hasSize(1);
            assertThat(results.stream().filter(
                    UsernameAlreadyExistsException.class::isInstance))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void databaseRejectsUnsupportedAccountStatus() {
        assertThatThrownBy(() -> insertDirectly(
                "BadStatus01", "LOCKED", 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_user_account_status");
    }

    @Test
    void databaseRejectsNegativeVersion() {
        assertThatThrownBy(() -> insertDirectly(
                "BadVersion01", "NORMAL", -1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_user_account_version");
    }

    @Test
    void registerEndpointPersistsBcryptAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  HttpUser01  ",
                                  "password": "secret123",
                                  "nickname": "  探索者  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value("HttpUser01"))
                .andExpect(jsonPath("$.nickname").value("探索者"))
                .andExpect(jsonPath("$.displayName").value("探索者"));

        String storedHash = jdbcTemplate.queryForObject(
                """
                        SELECT password_hash
                        FROM user_account
                        WHERE username = 'HttpUser01'
                        """,
                String.class);
        assertThat(storedHash).startsWith("{bcrypt}");
        assertThat(storedHash).doesNotContain("secret123");
        assertThat(passwordEncoder.matches("secret123", storedHash))
                .isTrue();
    }

    @Test
    void registerEndpointReturnsConflictForStoredUsername()
            throws Exception {
        registrationTransaction.save(account("HttpDuplicate01"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "HttpDuplicate01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode")
                        .value("USER_USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.field").value("username"));
    }

    @Test
    void registeredAccountLogsInWithStoredBcryptHashAndReceivesJwt()
            throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "LoginUser01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "LoginUser01",
                                  "password": "wrong123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTH_INVALID_CREDENTIALS"));

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "LoginUser01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tokenValue = objectMapper.readTree(responseBody)
                .path("accessToken")
                .asText();
        Jwt jwt = jwtDecoder.decode(tokenValue);
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE username = ?",
                Long.class,
                "LoginUser01");
        LocalDateTime lastLoginTime = jdbcTemplate.queryForObject(
                "SELECT last_login_time FROM user_account WHERE username = ?",
                LocalDateTime.class,
                "LoginUser01");

        assertThat(tokenValue).isNotBlank();
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(jwt.getClaimAsString("sid")).isNotBlank();
        assertThat(jwt.getClaimAsString("scope"))
                .isEqualTo("image:read image:search image:upload "
                        + "user:read user:write");
        assertThat(lastLoginTime).isNull();
    }

    private UserAccount account(String username) {
        return UserAccount.register(
                Username.of(username),
                PasswordHash.of("{bcrypt}integration-test-hash"),
                Optional.empty(),
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));
    }

    private void insertDirectly(
            String username,
            String status,
            int version) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_account(
                            username,
                            password_hash,
                            status,
                            auth_valid_after,
                            version,
                            created_time,
                            updated_time
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                username,
                "{bcrypt}integration-test-hash",
                status,
                STORED_AT,
                version,
                STORED_AT,
                STORED_AT);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "required integration-test environment is missing: "
                            + name);
        }
        return value;
    }

    private static JwtTestKeys createJwtTestKeys() {
        try {
            Path directory = Files.createTempDirectory("everypicfound-jwt-test-");
            var keyPair = TestRsaKeyMaterial.generate(2048);
            Path privateKey = TestRsaKeyMaterial.writePrivateKey(directory, "private.pem", keyPair);
            Path publicKey = TestRsaKeyMaterial.writePublicKey(directory, "public.pem", keyPair);
            privateKey.toFile().deleteOnExit();
            publicKey.toFile().deleteOnExit();
            directory.toFile().deleteOnExit();
            return new JwtTestKeys(privateKey, publicKey);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private record JwtTestKeys(Path privateKey, Path publicKey) {
    }

    private record PersistedAccountRow(
            long id,
            LocalDateTime createdTime,
            LocalDateTime updatedTime,
            LocalDateTime authValidAfter) {
    }
}
