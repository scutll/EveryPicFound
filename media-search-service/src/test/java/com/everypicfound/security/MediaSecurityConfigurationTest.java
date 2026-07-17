package com.everypicfound.security;

import com.everypicfound.security.contract.SecurityScopes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        MediaSecurityConfigurationTest.TestApplication.class,
        MediaSecurityConfigurationTest.TestController.class
})
@AutoConfigureMockMvc
@Import(com.everypicfound.security.infrastructure.jwt.MediaSecurityConfiguration.class)
class MediaSecurityConfigurationTest {

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
    void infoEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/search/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchEndpointRequiresSearchScope() throws Exception {
        mockMvc.perform(post("/api/search/text")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_READ)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchEndpointAcceptsSearchScope() throws Exception {
        mockMvc.perform(post("/api/search/text")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_SEARCH)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("search"));
    }

    @Test
    void imageUploadEndpointRequiresUploadScope() throws Exception {
        mockMvc.perform(post("/api/images/upload")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_SEARCH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void imageFileEndpointRequiresReadScope() throws Exception {
        mockMvc.perform(get("/images/2026/07/17/example.jpg")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + SecurityScopes.IMAGE_READ))))
                .andExpect(status().isOk())
                .andExpect(content().string("image"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Controller
    static class TestController {

        @PostMapping("/api/search/text")
        @ResponseBody
        String search() {
            return "search";
        }

        @PostMapping("/api/images/upload")
        @ResponseBody
        String upload() {
            return "upload";
        }

        @GetMapping("/images/**")
        @ResponseBody
        String image() {
            return "image";
        }
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
                Path directory = Files.createTempDirectory("everypicfound-media-jwt-");
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
