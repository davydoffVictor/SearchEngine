package searchengine.dto.search;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResponseWithError extends SearchResponse{
    private String error;
}
