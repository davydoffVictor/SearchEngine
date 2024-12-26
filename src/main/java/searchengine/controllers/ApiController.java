package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.management.IndexingResponse;
import searchengine.dto.management.IndexingResponseWithError;
import searchengine.dto.management.URL;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseWithError;
import searchengine.dto.statistics.*;
import searchengine.services.StatisticsService;
import searchengine.services.ManagementService;
import searchengine.services.SearchService;


@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final ManagementService managementService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, ManagementService managementService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.managementService = managementService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = managementService.startIndexing();
        return (response instanceof IndexingResponseWithError) ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = managementService.stopIndexing();
        return (response instanceof IndexingResponseWithError) ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(URL url) {
        IndexingResponse response = managementService.addUpdatePage(url.getUrl());
        return (response instanceof IndexingResponseWithError) ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);
    }


    @GetMapping("/search{query}{site}{offset}{limit}")
    public ResponseEntity<SearchResponse> search(String query, String site, int offset, int limit) {
        SearchResponse response = searchService.search(query, site, offset, limit);
        return (response instanceof SearchResponseWithError) ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);

    }
}
