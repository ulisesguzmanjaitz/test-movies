package domus.challenge.service;


import domus.challenge.model.response.DirectorResponse;
import reactor.core.publisher.Mono;

public interface DirectorService {
    Mono<DirectorResponse> getDirectorsByThreshold(Integer threshold);
}
