package com.everypicfound.interaction.infrastructure.security;

import com.everypicfound.interaction.infrastructure.security.jwt.InteractionSecurityConfiguration;
import com.everypicfound.interaction.security.AuthenticatedUserIdResolver;
import com.everypicfound.interaction.support.web.InteractionExceptionHandler;
import com.everypicfound.interaction.support.web.InteractionRequestIdFilter;
import com.everypicfound.interaction.support.web.InteractionSecurityErrorWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {
                InteractionSecurityConfigurationTest.TestApplication.class,
                InteractionSecurityConfigurationTest.TestController.class
        },
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway."
                        + "FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
@Import({
        InteractionSecurityConfiguration.class,
        InteractionSecurityErrorWriter.class,
        InteractionRequestIdFilter.class,
        InteractionExceptionHandler.class,
        AuthenticatedUserIdResolver.class
})
class InteractionSecurityConfigurationTest {

    private static final TestPublicKey PUBLIC_KEY =
            TestPublicKey.create();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void interactionSecurityProperties(
            DynamicPropertyRegistry registry) {
        registry.add(
                "everypicfound.auth.jwt.issuer",
                () -> "everypicfound-identity");
        registry.add(
                "everypicfound.auth.jwt.audience",
                () -> "everypicfound-api");
        registry.add(
                "everypicfound.auth.jwt.clock-skew",
                () -> "30s");
        registry.add(
                "everypicfound.auth.jwt.public-key-location",
                PUBLIC_KEY::uri);
    }

    @Test
    void businessEndpointRejectsMissingTokenWithStableResponse()
            throws Exception {
        mockMvc.perform(get("/api/interactions/_stage0")
                        .header(
                                InteractionRequestIdFilter.HEADER_NAME,
                                "stage0-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        InteractionRequestIdFilter.HEADER_NAME,
                        "stage0-request"))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message")
                        .value("authentication required"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.requestId")
                        .value("stage0-request"));
    }

    @Test
    void validJwtNeedsNoAdditionalInteractionScope()
            throws Exception {
        mockMvc.perform(get("/api/interactions/_stage0")
                        .with(jwt()
                                .jwt(token -> token.subject("42"))
                                .authorities(List.of())))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void invalidJwtSubjectIsRejectedAsUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/interactions/_stage0")
                        .with(jwt()
                                .jwt(token -> token.subject("not-a-user"))
                                .authorities(List.of())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message")
                        .value("authenticated user is invalid"));
    }

    @Test
    void actuatorEndpointAlsoRequiresAuthentication()
            throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Controller
    static class TestController {

        private final AuthenticatedUserIdResolver userIdResolver;

        TestController(
                AuthenticatedUserIdResolver userIdResolver) {
            this.userIdResolver = userIdResolver;
        }

        @GetMapping("/api/interactions/_stage0")
        @ResponseBody
        String currentUserId(
                @AuthenticationPrincipal Jwt jwt) {
            return String.valueOf(userIdResolver.resolve(jwt));
        }
    }

    private static final class TestPublicKey {

        private final Path publicKeyPath;

        private TestPublicKey(Path publicKeyPath) {
            this.publicKeyPath = publicKeyPath;
        }

        static TestPublicKey create() {
            try {
                KeyPairGenerator generator =
                        KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                Path directory = Files.createTempDirectory(
                        "everypicfound-interaction-jwt-");
                Path publicKeyPath = directory.resolve("public.pem");
                Files.writeString(
                        publicKeyPath,
                        pem(
                                "PUBLIC KEY",
                                keyPair.getPublic().getEncoded()));
                return new TestPublicKey(publicKeyPath);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not create interaction test public key",
                        exception);
            }
        }

        String uri() {
            return publicKeyPath.toUri().toString();
        }

        private static String pem(String label, byte[] der) {
            return "-----BEGIN " + label + "-----\n"
                    + Base64.getMimeEncoder(
                                    64,
                                    "\n".getBytes(
                                            StandardCharsets.US_ASCII))
                            .encodeToString(der)
                    + "\n-----END " + label + "-----\n";
        }
    }
}
