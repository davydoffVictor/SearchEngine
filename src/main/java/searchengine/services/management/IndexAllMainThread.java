package searchengine.services.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class IndexAllMainThread implements Runnable {
    private final ManagementServiceImpl service;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;

    @Override
    public void run() {

        for (Site siteInput : sites.getSites()) {
            log.info(siteInput.getName() + " " + siteInput.getUrl());
            service.createNewDeleteOld(siteInput, "INDEXING", "");
        }

        ExecutorService executor = Executors.newFixedThreadPool(sites.getSites().size());
        service.setIndexingIsRunning(true);
        service.setIndexingIsStopped(false);
        HashSet<Future<String>> futures = new HashSet<>();


        for (Site site : sites.getSites()) {
            IndexSite indexSite = new IndexSite(service, siteRepository, pageRepository, site);
            Future<String> futureInt = executor.submit(indexSite);
            futures.add(futureInt);
        }

        for (Future<String> fS : futures) {
            try {
                log.info(fS.get());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        executor.shutdown();
        log.info("End of IndexAllMainThread");
        try {
            service.deleteDuplicatedLemmasForSites();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        service.setIndexingIsStopped(true);
        service.setIndexingIsRunning(false);
    }
}
