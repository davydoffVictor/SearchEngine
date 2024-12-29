package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatistics {
    private Integer sites;
    private Integer pages;
    private Integer lemmas;
    private Boolean indexing;
}
