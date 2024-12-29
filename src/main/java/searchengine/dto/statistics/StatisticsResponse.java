package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponse {
    private Boolean result;
    private StatisticsData statistics;
}
