package searchengine.services.management;

import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.statistics.StatisticsServiceImpl;

import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;


public class IndexSite implements Callable<String> {
    private final ManagementServiceImpl service;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Site site;

    public IndexSite(ManagementServiceImpl managementService, SiteRepository siteRepository, PageRepository pageRepository, Site site) {
        this.service = managementService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.site = site;
    }



    @Override
    @Transactional
    public String call() throws Exception {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        Logger.getLogger(StatisticsServiceImpl.class.getName()).info(siteEntity.toString());

        try {
            Node root = new Node("/", 1, service, siteEntity);
            TreeSet<String> pages = new ForkJoinPool().invoke(new NodePageExplorer(root));
            if (service.getIndexingIsRunning()) {
                service.updateSiteStatus("INDEXED", "", siteEntity.getId());
                Logger.getLogger(StatisticsServiceImpl.class.getName()).info("Site " + siteEntity.getUrl() + " INDEXED");
            } else {
                service.updateSiteStatus("FAILED", "Индексация остановлена пользователем", siteEntity.getId());
                Logger.getLogger(StatisticsServiceImpl.class.getName()).info("Site " + siteEntity.getUrl() + " - indexing was interrupted by user");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pageRepository.countPagesOnSite(siteEntity.getId()) == 0) {
            service.updateSiteStatus("FAILED", "Главная страница сайта недоступна", siteEntity.getId());
        }

        return pageRepository.countPagesOnSite(siteEntity.getId()) + " for site: " + siteEntity.getId();
    }
}
