package domus.challenge.service.imp;

import com.github.benmanes.caffeine.cache.Cache;
import domus.challenge.model.dto.Movie;
import domus.challenge.model.dto.MovieApiResponse;
import domus.challenge.model.response.DirectorResponse;
import domus.challenge.service.DirectorService;
import domus.challenge.service.MovieApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DirectorServiceImp implements DirectorService {

    @Value("${external-api.max-concurrent-requests:5}")
    private int maxConcurrentRequests;

    private final MovieApiClient movieApiClient;
    private final Cache<Integer, DirectorResponse> directorsCache;

    public DirectorServiceImp(MovieApiClient movieApiClient,
                              Cache<Integer, DirectorResponse> directorsCache) {
        this.movieApiClient = movieApiClient;
        this.directorsCache = directorsCache;
    }

    public Mono<DirectorResponse> getDirectorsByThreshold(Integer threshold) {

        if (threshold < 0) {
            return Mono.just(DirectorResponse.builder().directors(List.of()).build());
        }

        DirectorResponse cached = directorsCache.getIfPresent(threshold);
        if (cached != null) {
            log.info("Returning cached response for threshold {}", threshold);
            return Mono.just(cached);
        }

        return movieApiClient.getMoviesPage(1)
                .flatMap(firstPage -> {
                    int totalPages = firstPage.getTotal_pages();
                    Flux<MovieApiResponse> allPages;

                    if (totalPages <= 1) {
                        allPages = Flux.just(firstPage);
                    } else {
                        allPages = Flux.concat(
                                Flux.just(firstPage),
                                Flux.range(2, totalPages - 1)
                                        .flatMap(movieApiClient::getMoviesPage, maxConcurrentRequests)
                        );
                    }

                    return allPages
                            .flatMap(page -> Flux.fromIterable(page.getData()))
                            .collect(Collectors.groupingByConcurrent(Movie::getDirector, Collectors.counting()))
                            .map(counts -> counts.entrySet().stream()
                                    .filter(e -> e.getValue() > threshold)
                                    .map(Map.Entry::getKey)
                                    .sorted()
                                    .collect(Collectors.toList()))
                            .map(directors -> {
                                DirectorResponse response = DirectorResponse.builder()
                                        .directors(directors)
                                        .build();
                                directorsCache.put(threshold, response);
                                return response;
                            });
                });
    }


}