package searchengine.services.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
public class IndexSite implements Callable<String> {
    private final ManagementServiceImpl service;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Site site;

    @Override
    @Transactional
    public String call() throws Exception {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        log.info(siteEntity.toString());

        try {
            Node root = new Node("/", 1, service, siteEntity);
            TreeSet<String> pages = new ForkJoinPool().invoke(new NodePageExplorer(root));
            if (service.getIndexingIsRunning()) {
                service.updateSiteStatus("INDEXED", "", siteEntity.getId());
                log.info("Site " + siteEntity.getUrl() + " INDEXED");
            } else {
                service.updateSiteStatus("FAILED", "Индексация остановлена пользователем", siteEntity.getId());
                log.info("Site " + siteEntity.getUrl() + " - indexing was interrupted by user");
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (pageRepository.countBySiteEntity(siteEntity) == 0) {
            service.updateSiteStatus("FAILED", "Главная страница сайта недоступна", siteEntity.getId());
        }

        return pageRepository.countBySiteEntity(siteEntity) + " for site: " + siteEntity.getId();
    }
}
