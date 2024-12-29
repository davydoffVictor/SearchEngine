package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.management.IndexingResponse;
import searchengine.dto.management.URL;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.*;
import searchengine.services.StatisticsService;
import searchengine.services.ManagementService;
import searchengine.services.SearchService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final ManagementService managementService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {
         return managementService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        return managementService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(URL url) {
        return managementService.addUpdatePage(url.getUrl());
    }

    @GetMapping("/search{query}{site}{offset}{limit}")
    public SearchResponse search(String query, String site, int offset, int limit) {
        return searchService.search(query, site, offset, limit);
    }
}
