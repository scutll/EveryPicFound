package com.everypicfound.interaction.picturelike.interfaces;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everypicfound.interaction.picturelike.application.PictureLikeApplicationService;
import com.everypicfound.interaction.security.AuthenticatedUserIdResolver;
import com.everypicfound.interaction.support.web.InteractionExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = {
                PictureLikeControllerContextTest.TestApplication.class,
                PictureLikeController.class,
                InteractionExceptionHandler.class
        },
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway."
                        + "FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc(addFilters = false)
class PictureLikeControllerContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void realControllerLoadsAndRejectsInvalidPictureId()
            throws Exception {
        mockMvc.perform(get(
                        "/api/interactions/pictures/0/likes/count"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedPictureIdIsBadRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/interactions/pictures/abc/likes/count"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedCursorTimeIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/interactions/me/picture-likes")
                        .param("cursorTime", "not-a-time")
                        .param("cursorPictureId", "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMethodRemains405() throws Exception {
        mockMvc.perform(put(
                        "/api/interactions/pictures/7/likes"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownResourceRemains404() throws Exception {
        mockMvc.perform(get("/api/interactions/unknown"))
                .andExpect(status().isNotFound());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        PictureLikeApplicationService pictureLikeApplicationService() {
            return mock(PictureLikeApplicationService.class);
        }

        @Bean
        AuthenticatedUserIdResolver authenticatedUserIdResolver() {
            return mock(AuthenticatedUserIdResolver.class);
        }
    }
}
