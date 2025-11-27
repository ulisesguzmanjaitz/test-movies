package domus.challenge.service;

import domus.challenge.exception.ApiException;
import domus.challenge.model.dto.Movie;
import domus.challenge.model.dto.MovieApiResponse;
import domus.challenge.service.imp.MovieApiServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class MovieApiServiceTest {

    private MovieApiServiceImp movieApiService;

    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec requestSpec;

    @Mock
    private RequestHeadersSpec headersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        movieApiService = new MovieApiServiceImp(webClient);
        
        ReflectionTestUtils.setField(movieApiService, "moviesUrl", "http://asd-api/movies");
    }

    @Test
    @DisplayName("Should fetch movies page successfully")
    void testGetMoviesPageSuccess() {
        MovieApiResponse mockResponse = MovieApiResponse.builder()
                .page(1)
                .data(Collections.singletonList(
                        Movie.builder().title("Movie 1").director("Director A").build()
                ))
                .build();

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("http://asd-api/movies?page={page}", 1)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MovieApiResponse.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(movieApiService.getMoviesPage(1))
                .expectNext(mockResponse)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should transform errors into ApiException")
    void testGetMoviesPageErrorTransformsToApiException() {
        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("http://asd-api/movies?page={page}", 1)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MovieApiResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        StepVerifier.create(movieApiService.getMoviesPage(1))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ApiException.class);
                    assertThat(throwable.getMessage()).contains("Failed to fetch page 1");
                })
                .verify();
    }

    @Test
    @DisplayName("Should return empty list if no movies")
    void testGetMoviesPageEmpty() {
        MovieApiResponse mockResponse = MovieApiResponse.builder()
                .page(1)
                .data(Collections.emptyList())
                .build();

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("http://asd-api/movies?page={page}", 1)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MovieApiResponse.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(movieApiService.getMoviesPage(1))
                .expectNextMatches(resp -> resp.getData().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should timeout if request takes too long")
    void testGetMoviesPageTimeout() {
        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("http://asd-api/movies?page={page}", 1)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(MovieApiResponse.class)).thenReturn(Mono.never());

        StepVerifier.create(movieApiService.getMoviesPage(1).timeout(Duration.ofMillis(50)))
                .expectErrorMatches(throwable -> throwable instanceof java.util.concurrent.TimeoutException)
                .verify();
    }
}

