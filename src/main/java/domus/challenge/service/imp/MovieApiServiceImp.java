package domus.challenge.service.imp;

import domus.challenge.exception.ApiException;
import domus.challenge.model.dto.MovieApiResponse;
import domus.challenge.service.MovieApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieApiServiceImp implements MovieApiClient {

    @Value("${external-api.movies-url}")
    private String moviesUrl;

    private final WebClient webClient;

    public Mono<MovieApiResponse> getMoviesPage(int pageNumber) {

        return Mono.defer(() -> webClient.get()
                        .uri(moviesUrl + "?page={page}", pageNumber)
                        .retrieve()
                        .bodyToMono(MovieApiResponse.class)
                        .doOnNext(response -> log.debug("Fetched page {} with {} movies",
                                pageNumber, response.getData().size()))
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(5))
                                .doBeforeRetry(signal -> log.warn("Retry attempt {} for page {}",
                                        signal.totalRetries() + 1, pageNumber)))
                        .onErrorMap(ex -> new ApiException("Failed to fetch page " + pageNumber, ex))
                        .timeout(Duration.ofSeconds(15)));
    }
}