package domus.challenge.controller;

import domus.challenge.exception.TooManyRequestsException;
import domus.challenge.model.response.DirectorResponse;
import domus.challenge.service.DirectorService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/directors")
public class DirectorController {

    private final DirectorService directorService;
    private final RateLimiter rateLimiter;

    public DirectorController(DirectorService directorService,
                              RateLimiter rateLimiter) {
        this.directorService = directorService;
        this.rateLimiter = rateLimiter;
    }

    @Tag(name = "Directors", description = "Endpoints for managing directors")
    @Operation(
            summary = "Get Directors by Movie Threshold",
            description = "Retrieves a list of directors who have directed more movies " +
                    "than the specified threshold. Results are sorted alphabetically."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved directors",
                    content = @Content(schema = @Schema(implementation = DirectorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid threshold value (non-numeric or invalid format)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred"
            )
    })
    @Parameter(
            name = "threshold",
            description = "Minimum number of movies. Directors with more than this count are returned.",
            required = true,
            example = "4"
    )
    @GetMapping
    public Mono<DirectorResponse> getDirectors(@RequestParam Integer threshold) {
        log.info("Received request for directors with threshold: {}", threshold);

        return directorService.getDirectorsByThreshold(threshold)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .onErrorMap(RequestNotPermitted.class,
                        ex -> new TooManyRequestsException(
                                "Rate limit exceeded. Try again later"));
    }


    @Tag(name = "Health")
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Simple health check endpoint")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("API is running"));
    }
}
