package domus.challenge.model.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieApiResponse {
    private int page;
    private int per_page;
    private int total;
    private int total_pages;
    private List<Movie> data;
}
