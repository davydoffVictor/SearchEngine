package searchengine.services.management;

import searchengine.model.PageEntity;

public class IndexSinglePageThread implements Runnable{
    private final ManagementServiceImpl service;
    private final PageEntity pageEntity;

    public IndexSinglePageThread(ManagementServiceImpl service, PageEntity pageEntity) {
        this.service = service;
        this.pageEntity = pageEntity;
    }

    @Override
    public void run() {
        service.indexPage(pageEntity);
    }
}
