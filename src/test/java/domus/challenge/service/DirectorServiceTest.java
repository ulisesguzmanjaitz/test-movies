package domus.challenge.service;


import com.github.benmanes.caffeine.cache.Cache;
import domus.challenge.model.dto.Movie;
import domus.challenge.model.dto.MovieApiResponse;
import domus.challenge.model.response.DirectorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("DirectorService Tests")
class DirectorServiceTest {

    @Autowired
    private DirectorService directorService;

    @MockitoBean
    private MovieApiClient movieApiClient;

    @Test
    @DisplayName("Should return directors with more movies than threshold")
    void testGetDirectorsByThresholdSuccess() {
        int threshold = 2;

        MovieApiResponse page1 = MovieApiResponse.builder()
                .page(1)
                .total_pages(1)
                .data(Arrays.asList(
                        Movie.builder().title("Movie 1").director("Martin Scorsese").build(),
                        Movie.builder().title("Movie 2").director("Martin Scorsese").build(),
                        Movie.builder().title("Movie 3").director("Martin Scorsese").build(),
                        Movie.builder().title("Movie 4").director("Woody Allen").build(),
                        Movie.builder().title("Movie 5").director("Woody Allen").build(),
                        Movie.builder().title("Movie 6").director("Steven Spielberg").build()
                ))
                .build();

        when(movieApiClient.getMoviesPage(1)).thenReturn(Mono.just(page1));

        StepVerifier.create(directorService.getDirectorsByThreshold(threshold))
                .assertNext(response -> {
                    assertThat(response.getDirectors())
                            .contains("Martin Scorsese")
                            .doesNotContain("Steven Spielberg")
                            .isSorted();
                })
                .verifyComplete();

        verify(movieApiClient, times(1)).getMoviesPage(1);
    }

    @Test
    @DisplayName("Should return empty list when threshold is negative")
    void testGetDirectorsByThresholdNegative() {
        StepVerifier.create(directorService.getDirectorsByThreshold(-5))
                .assertNext(response -> assertThat(response.getDirectors()).isEmpty())
                .verifyComplete();

        verify(movieApiClient, never()).getMoviesPage(anyInt());
    }

    @Test
    @DisplayName("Should return empty list when threshold is zero")
    void testGetDirectorsByThresholdZero() {
        MovieApiResponse page1 = MovieApiResponse.builder()
                .page(1)
                .total_pages(1)
                .data(Collections.singletonList(
                        Movie.builder().title("Movie 1").director("Director A").build()
                ))
                .build();

        when(movieApiClient.getMoviesPage(1)).thenReturn(Mono.just(page1));

        StepVerifier.create(directorService.getDirectorsByThreshold(0))
                .assertNext(response -> assertThat(response.getDirectors())
                        .containsExactly("Director A"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void testGetDirectorsByThresholdMultiplePages() {
        int threshold = 1;

        MovieApiResponse page1 = MovieApiResponse.builder()
                .page(1)
                .per_page(2)
                .total(5)
                .total_pages(2)
                .data(Arrays.asList(
                        Movie.builder().title("Movie 1").director("Director A").build(),
                        Movie.builder().title("Movie 2").director("Director A").build()
                ))
                .build();

        MovieApiResponse page2 = MovieApiResponse.builder()
                .page(2)
                .per_page(3)
                .total(5)
                .total_pages(2)
                .data(Arrays.asList(
                        Movie.builder().title("Movie 3").director("Director B").build(),
                        Movie.builder().title("Movie 4").director("Director B").build(),
                        Movie.builder().title("Movie 5").director("Director B").build()
                ))
                .build();

        when(movieApiClient.getMoviesPage(1)).thenReturn(Mono.just(page1));
        when(movieApiClient.getMoviesPage(2)).thenReturn(Mono.just(page2));

        StepVerifier.create(directorService.getDirectorsByThreshold(threshold))
                .assertNext(response -> assertThat(response.getDirectors())
                        .contains("Director A", "Director B")
                        .hasSize(2)
                        .isSorted())
                .verifyComplete();

        verify(movieApiClient).getMoviesPage(1);
        verify(movieApiClient).getMoviesPage(2);
    }

    @Test
    @DisplayName("Should return cached response if threshold was previously requested")
    void testGetDirectorsByThresholdCached() {
        int threshold = 3;

        DirectorResponse cachedResponse = DirectorResponse.builder()
                .directors(List.of("Cached Director"))
                .build();

        Object targetService = AopTestUtils.getTargetObject(directorService);
        Cache<Integer, DirectorResponse> cache = (Cache<Integer, DirectorResponse>)
                ReflectionTestUtils.getField(targetService, "directorsCache");

        cache.put(threshold, cachedResponse);

        StepVerifier.create(directorService.getDirectorsByThreshold(threshold))
                .assertNext(response -> assertThat(response.getDirectors())
                        .containsExactly("Cached Director"))
                .verifyComplete();

        verify(movieApiClient, never()).getMoviesPage(anyInt());
    }

}