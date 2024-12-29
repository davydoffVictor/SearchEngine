package searchengine.services.management;

import lombok.RequiredArgsConstructor;
import searchengine.model.PageEntity;

@RequiredArgsConstructor
public class IndexSinglePageThread implements Runnable{
    private final ManagementServiceImpl service;
    private final PageEntity pageEntity;

    @Override
    public void run() {
        service.indexPage(pageEntity);
    }
}
