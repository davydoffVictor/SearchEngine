package searchengine.services.management;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class IndexAllMainThread implements Runnable {
    private final ManagementServiceImpl service;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final Logger log = Logger.getLogger(IndexAllMainThread.class.getName());

    public IndexAllMainThread(ManagementServiceImpl managementService, SiteRepository siteRepository, PageRepository pageRepository, SitesList sites) {
        this.service = managementService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sites = sites;
    }

    @Override
    public void run() {
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
                e.printStackTrace();
            }
        }



        executor.shutdown();

        log.info("End of IndexAllMainThread");
        service.deleteDuplicatedLemmasForSites();
        service.setIndexingIsStopped(true);
        service.setIndexingIsRunning(false);

    }


}
