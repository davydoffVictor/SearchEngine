package searchengine.services;

import searchengine.dto.management.IndexingResponse;

public interface ManagementService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse addUpdatePage(String url);
}
