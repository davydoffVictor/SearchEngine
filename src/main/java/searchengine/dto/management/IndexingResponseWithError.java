package searchengine.dto.management;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponseWithError extends IndexingResponse {
    private String error;
}
