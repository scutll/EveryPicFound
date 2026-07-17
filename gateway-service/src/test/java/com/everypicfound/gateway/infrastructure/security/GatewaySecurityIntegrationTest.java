package com.everypicfound.gateway.infrastructure.security;

import com.everypicfound.security.contract.SecurityScopes;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityIntegrationTest {

    private static final String ISSUER = "everypicfound-identity";
    private static final String AUDIENCE = "everypicfound-api";
    private static final TestJwtKeys JWT_KEYS = TestJwtKeys.create();
    private static final TestDownstream DOWNSTREAM = new TestDownstream();

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startDownstream() throws IOException {
        DOWNSTREAM.start();
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.stop();
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("IDENTITY_SERVICE_URI", DOWNSTREAM::uri);
        registry.add("MEDIA_SEARCH_SERVICE_URI", DOWNSTREAM::uri);
        registry.add("everypicfound.auth.jwt.issuer", () -> ISSUER);
        registry.add("everypicfound.auth.jwt.audience", () -> AUDIENCE);
        registry.add("everypicfound.auth.jwt.clock-skew", () -> "30s");
        registry.add("everypicfound.auth.jwt.public-key-location", JWT_KEYS::publicKeyUri);
    }

    @Test
    void publicAuthRouteDoesNotRequireToken() {
        DOWNSTREAM.clear();

        webTestClient.post()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("POST /api/auth/login");

        assertThat(DOWNSTREAM.requests()).hasSize(1);
    }

    @Test
    void protectedMediaRouteRejectsMissingTokenBeforeRouting() {
        DOWNSTREAM.clear();

        webTestClient.post()
                .uri("/api/search/text")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.requests()).isEmpty();
    }

    @Test
    void protectedMediaRouteRejectsInsufficientScopeBeforeRouting() {
        DOWNSTREAM.clear();
        String token = JWT_KEYS.token(List.of(SecurityScopes.IMAGE_READ));

        webTestClient.post()
                .uri("/api/search/text")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();

        assertThat(DOWNSTREAM.requests()).isEmpty();
    }

    @Test
    void protectedMediaRouteForwardsOriginalBearerTokenAfterAuthentication() {
        DOWNSTREAM.clear();
        String token = JWT_KEYS.token(List.of(SecurityScopes.IMAGE_SEARCH));

        webTestClient.post()
                .uri("/api/search/text")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("POST /api/search/text");

        assertThat(DOWNSTREAM.requests()).hasSize(1);
        assertThat(DOWNSTREAM.requests().get(0).authorization())
                .isEqualTo("Bearer " + token);
    }

    @Test
    void protectedMediaAuthProbeRouteUsesSearchScopeAndForwardsBearerToken() {
        DOWNSTREAM.clear();
        String token = JWT_KEYS.token(List.of(SecurityScopes.IMAGE_SEARCH));

        webTestClient.get()
                .uri("/api/search/_auth/probe")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET /api/search/_auth/probe");

        assertThat(DOWNSTREAM.requests()).hasSize(1);
        assertThat(DOWNSTREAM.requests().get(0).authorization())
                .isEqualTo("Bearer " + token);
    }

    @Test
    void protectedMediaRouteRejectsInvalidTokenBeforeRouting() {
        DOWNSTREAM.clear();

        webTestClient.post()
                .uri("/api/search/text")
                .headers(headers -> headers.setBearerAuth("not-a-jwt"))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.requests()).isEmpty();
    }

    private record DownstreamRequest(String method, String path, String authorization) {
    }

    private static final class TestDownstream {

        private final List<DownstreamRequest> requests = new CopyOnWriteArrayList<>();
        private HttpServer server;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        String uri() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void clear() {
            requests.clear();
        }

        List<DownstreamRequest> requests() {
            return requests;
        }

        private void handle(HttpExchange exchange) throws IOException {
            requests.add(new DownstreamRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization")));
            byte[] body = (exchange.getRequestMethod() + " "
                    + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }
    }

    private static final class TestJwtKeys {

        private final RSAPublicKey publicKey;
        private final RSAPrivateKey privateKey;
        private final Path publicKeyPath;
        private final NimbusJwtEncoder encoder;

        private TestJwtKeys(
                RSAPublicKey publicKey,
                RSAPrivateKey privateKey,
                Path publicKeyPath) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.publicKeyPath = publicKeyPath;
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .build();
            this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
        }

        static TestJwtKeys create() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
                Path directory = Files.createTempDirectory("everypicfound-gateway-jwt-");
                Path publicKeyPath = directory.resolve("public.pem");
                Files.writeString(publicKeyPath, pem("PUBLIC KEY", publicKey.getEncoded()));
                return new TestJwtKeys(publicKey, privateKey, publicKeyPath);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create test RSA keys", exception);
            }
        }

        String publicKeyUri() {
            return publicKeyPath.toUri().toString();
        }

        String token(List<String> scopes) {
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(ISSUER)
                    .subject("42")
                    .audience(List.of(AUDIENCE))
                    .id(UUID.randomUUID().toString())
                    .issuedAt(now)
                    .notBefore(now)
                    .expiresAt(now.plusSeconds(1800))
                    .claim("sid", "test-session")
                    .claim("scope", String.join(" ", scopes))
                    .claim("auth_time", now.getEpochSecond())
                    .build();
            JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                    .type("JWT")
                    .build();
            return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        }

        private static String pem(String label, byte[] der) {
            return "-----BEGIN " + label + "-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(der)
                    + "\n-----END " + label + "-----\n";
        }
    }
}
