package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private Boolean result;
    private Integer count;
    private List<SearchData> data;
}
