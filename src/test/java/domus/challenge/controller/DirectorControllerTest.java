package domus.challenge.controller;

import domus.challenge.exception.ApiException;
import domus.challenge.exception.TooManyRequestsException;
import domus.challenge.model.response.DirectorResponse;
import domus.challenge.model.response.ErrorResponse;
import domus.challenge.service.DirectorService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(DirectorController.class)
@DisplayName("DirectorController Tests")
class DirectorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DirectorService directorService;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    @DisplayName("Should return 200 with directors list")
    void testGetDirectorsSuccess() {
        DirectorResponse response = DirectorResponse.builder()
                .directors(Arrays.asList("Martin Scorsese", "Woody Allen"))
                .build();

        when(directorService.getDirectorsByThreshold(4))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/directors?threshold=4")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DirectorResponse.class)
                .value(resp -> assertThat(resp.getDirectors())
                        .contains("Martin Scorsese", "Woody Allen"));

        verify(directorService).getDirectorsByThreshold(4);
    }

    @Test
    @DisplayName("Should return 400 when threshold is missing")
    void testGetDirectorsMissingThreshold() {
        webTestClient.get()
                .uri("/directors")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return 200 for health check")
    void testHealthCheck() {
        webTestClient.get()
                .uri("/directors/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("API is running");
    }

    @Test
    @DisplayName("Should return 429 when rate limit is exceeded")
    void testGetDirectorsRateLimitExceeded() {
        when(directorService.getDirectorsByThreshold(2))
                .thenReturn(Mono.error(new TooManyRequestsException("Rate limit reached")));

        webTestClient.get()
                .uri("/directors?threshold=2")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(429);
                    assertThat(errorResponse.getMessage()).isEqualTo("Rate limit reached");
                    assertThat(errorResponse.getTimestamp()).isNotNull();
                });

        verify(directorService).getDirectorsByThreshold(2);
    }

    @Test
    @DisplayName("Should return 500 when ApiException is thrown")
    void testGetDirectorsApiException() {
        when(directorService.getDirectorsByThreshold(3))
                .thenReturn(Mono.error(new ApiException("Invalid page or per_page value.",null)));

        webTestClient.get()
                .uri("/directors?threshold=3")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(500);
                    assertThat(errorResponse.getMessage())
                            .isEqualTo("Error fetching movies from external API: Invalid page or per_page value.");
                    assertThat(errorResponse.getTimestamp()).isNotNull();
                });

        verify(directorService).getDirectorsByThreshold(3);
    }

    @Test
    @DisplayName("Should return 500 when an unexpected exception is thrown")
    void testGetDirectorsGeneralException() {
        when(directorService.getDirectorsByThreshold(3))
                .thenReturn(Mono.error(new RuntimeException("Something went wrong")));

        webTestClient.get()
                .uri("/directors?threshold=3")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(500);
                    assertThat(errorResponse.getMessage()).isEqualTo("An unexpected error occurred");
                    assertThat(errorResponse.getTimestamp()).isNotNull();
                });

        verify(directorService).getDirectorsByThreshold(3);
    }


}