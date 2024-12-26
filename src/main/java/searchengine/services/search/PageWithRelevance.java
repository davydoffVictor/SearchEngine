package searchengine.services.search;

import lombok.Data;
import searchengine.model.PageEntity;

@Data
public class PageWithRelevance {
    private PageEntity pageEntity;
    private float relevance;
}
