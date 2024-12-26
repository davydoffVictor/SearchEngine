package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.StatisticsService;
import searchengine.services.management.ManagementServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;//From configuration file
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final ManagementServiceImpl managementService;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(managementService.getIndexingIsRunning());
        total.setPages(pageRepository.countPages());
        total.setLemmas(lemmaRepository.countLemmas());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());


            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
            if (siteEntity == null) {
                continue;
            }
            int pages = pageRepository.countPagesOnSite(siteEntity.getId());
            int lemmas = lemmaRepository.countLemmasOnSite(siteEntity.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus());
            item.setError(siteEntity.getLastError());

            Instant statusDTI = siteEntity.getStatusTime();
            long statusDTL = Long.parseLong(statusDTI.getEpochSecond() + "" + statusDTI.get(ChronoField.MILLI_OF_SECOND) + "00");
            item.setStatusTime(statusDTL);

            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }



}
