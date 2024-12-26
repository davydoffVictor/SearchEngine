package searchengine.dto.search;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponseWithData extends SearchResponse{
    private int count;
    private List<SearchData> data;
}
