package com.everypicfound.interaction.picturelike.infrastructure.client.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestMediaPictureExistenceClientTest {

    private MockRestServiceServer server;
    private RestMediaPictureExistenceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://media.test")
                .requestInterceptor(
                        new BearerTokenForwardingInterceptor());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestMediaPictureExistenceClient(
                builder.build());
        authenticate("validated-token");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        server.verify();
    }

    @Test
    void forwardsBearerTokenAndReturnsTrue() {
        server.expect(requestTo(
                        "http://media.test/internal/images/7/exists"))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer validated-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "pictureId": 7,
                            "exists": true
                          },
                          "requestId": "media-request"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThat(client.exists(7L)).isTrue();
    }

    @Test
    void validFalseResponseMeansPictureDoesNotExist() {
        expectJson("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "pictureId": 7,
                    "exists": false
                  }
                }
                """);

        assertThat(client.exists(7L)).isFalse();
    }

    @Test
    void upstreamHttpErrorIsUnavailable() {
        server.expect(requestTo(
                        "http://media.test/internal/images/7/exists"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.exists(7L))
                .isInstanceOf(
                        MediaServiceUnavailableException.class);
    }

    @Test
    void nonZeroEnvelopeCodeIsUnavailable() {
        expectJson("""
                {
                  "code": 500,
                  "message": "failed",
                  "data": null
                }
                """);

        assertThatThrownBy(() -> client.exists(7L))
                .isInstanceOf(
                        MediaServiceUnavailableException.class);
    }

    @Test
    void missingExistsFieldIsUnavailable() {
        expectJson("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "pictureId": 7
                  }
                }
                """);

        assertThatThrownBy(() -> client.exists(7L))
                .isInstanceOf(
                        MediaServiceUnavailableException.class);
    }

    @Test
    void mismatchedPictureIdIsUnavailable() {
        expectJson("""
                {
                  "code": 0,
                  "message": "success",
                  "data": {
                    "pictureId": 8,
                    "exists": true
                  }
                }
                """);

        assertThatThrownBy(() -> client.exists(7L))
                .isInstanceOf(
                        MediaServiceUnavailableException.class);
    }

    @Test
    void missingAuthenticatedJwtFailsBeforeHttpCall() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> client.exists(7L))
                .isInstanceOf(
                        MediaServiceUnavailableException.class);
    }

    private void expectJson(String body) {
        server.expect(requestTo(
                        "http://media.test/internal/images/7/exists"))
                .andRespond(withSuccess(
                        body,
                        MediaType.APPLICATION_JSON));
    }

    private void authenticate(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject("42")
                .build();
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new JwtAuthenticationToken(
                        jwt,
                        java.util.List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_TEST"))));
        SecurityContextHolder.setContext(context);
    }
}
