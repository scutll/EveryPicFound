package com.everypicfound.interaction.picturelike.infrastructure.client.media;

import com.everypicfound.interaction.picturelike.application.port.PictureExistencePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public final class RestMediaPictureExistenceClient
        implements PictureExistencePort {

    private static final ParameterizedTypeReference<
            MediaServiceResponse<ImageExistenceData>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public RestMediaPictureExistenceClient(
            @Qualifier("mediaServiceRestClient")
            RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean exists(long pictureId) {
        try {
            MediaServiceResponse<ImageExistenceData> response =
                    restClient.get()
                            .uri(
                                    "/internal/images/{pictureId}/exists",
                                    pictureId)
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, upstreamResponse) -> {
                                        throw unavailable(
                                                "media service returned "
                                                        + upstreamResponse
                                                        .getStatusCode());
                                    })
                            .body(RESPONSE_TYPE);
            return validate(response, pictureId);
        } catch (MediaServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new MediaServiceUnavailableException(
                    "media service request failed",
                    exception);
        }
    }

    private boolean validate(
            MediaServiceResponse<ImageExistenceData> response,
            long requestedPictureId) {
        if (response == null
                || response.code() == null
                || response.code() != 0
                || response.data() == null
                || response.data().pictureId() == null
                || response.data().pictureId() != requestedPictureId
                || response.data().exists() == null) {
            throw unavailable(
                    "media service returned an invalid response");
        }
        return response.data().exists();
    }

    private MediaServiceUnavailableException unavailable(
            String message) {
        return new MediaServiceUnavailableException(message);
    }

    private record MediaServiceResponse<T>(
            Integer code,
            String message,
            T data,
            String requestId) {
    }

    private record ImageExistenceData(
            Long pictureId,
            Boolean exists) {
    }
}
