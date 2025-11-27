package domus.challenge.service;

import domus.challenge.model.dto.MovieApiResponse;
import reactor.core.publisher.Mono;

public interface MovieApiClient {
    Mono<MovieApiResponse> getMoviesPage(int page);
}
