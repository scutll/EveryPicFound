package com.everypicfound.security.interfaces.web.probe;

import com.everypicfound.security.contract.SecurityScopes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        MediaAuthProbeControllerTest.TestApplication.class,
        MediaAuthProbeController.class
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(com.everypicfound.security.infrastructure.jwt.MediaSecurityConfiguration.class)
class MediaAuthProbeControllerTest {

    private static final TestPublicKey PUBLIC_KEY = TestPublicKey.create();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void mediaSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("everypicfound.auth.jwt.issuer", () -> "everypicfound-identity");
        registry.add("everypicfound.auth.jwt.audience", () -> "everypicfound-api");
        registry.add("everypicfound.auth.jwt.clock-skew", () -> "30s");
        registry.add("everypicfound.auth.jwt.public-key-location", PUBLIC_KEY::uri);
    }

    @Test
    void probeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/search/_auth/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void probeRequiresSearchScope() throws Exception {
        mockMvc.perform(get("/api/search/_auth/probe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_READ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void probeReturnsCurrentAuthenticationWhenSearchScopeIsPresent() throws Exception {
        mockMvc.perform(get("/api/search/_auth/probe")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("42"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_SEARCH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("media-auth-probe"))
                .andExpect(jsonPath("$.data.service").value("media-search-service"))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.subject").value("42"))
                .andExpect(jsonPath("$.data.authorities[0]").value("SCOPE_" + SecurityScopes.IMAGE_SEARCH));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }

    private static final class TestPublicKey {

        private final Path publicKeyPath;

        private TestPublicKey(Path publicKeyPath) {
            this.publicKeyPath = publicKeyPath;
        }

        static TestPublicKey create() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                Path directory = Files.createTempDirectory("everypicfound-media-auth-probe-jwt-");
                Path publicKeyPath = directory.resolve("public.pem");
                Files.writeString(publicKeyPath, pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
                return new TestPublicKey(publicKeyPath);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create test public key", exception);
            }
        }

        String uri() {
            return publicKeyPath.toUri().toString();
        }

        private static String pem(String label, byte[] der) {
            return "-----BEGIN " + label + "-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(der)
                    + "\n-----END " + label + "-----\n";
        }
    }
}
