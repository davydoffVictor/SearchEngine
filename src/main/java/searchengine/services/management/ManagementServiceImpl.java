package searchengine.services.management;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.management.IndexingResponse;
import searchengine.dto.management.IndexingResponseWithError;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.common.Lemmatizator;
import searchengine.services.ManagementService;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
@Getter
@Setter
public class ManagementServiceImpl implements ManagementService {
    private final SitesList sites;//From configuration file
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SessionFactory sessionFactory;
    private final Lemmatizator lemmatizator;

    private volatile boolean indexingIsRunning = false;
    private volatile boolean indexingIsStopped = false;
    private final Logger log = Logger.getLogger(ManagementServiceImpl.class.getName());


    public ManagementServiceImpl(SitesList sites, PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository
                                 ,IndexRepository indexRepository, SessionFactory sessionFactory, Lemmatizator lemmatizator) {
        this.sites = sites;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sessionFactory = sessionFactory;
        this.lemmatizator = lemmatizator;
        createSitesInDbFromConfig();
        if (sites.getReferrer() != null)
        {
            Node.setReferrer(sites.getReferrer());
            log.info("Read from config. Referrer: " + Node.getReferrer());
        }
        if (sites.getUserAgent() != null) {
            Node.setUserAgent(sites.getUserAgent());
            log.info("Read from config. User-agent: " + Node.getUserAgent());
        }
    }

    @Override
    public IndexingResponse startIndexing() {
        if (indexingIsRunning) {
            IndexingResponseWithError response = new IndexingResponseWithError();
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }

        indexingIsRunning = true;
        indexingIsStopped = false;
        Node.pages = new TreeSet<>();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        IndexAllMainThread indexAllMainThread = new IndexAllMainThread(this, siteRepository, pageRepository, sites);
        executor.submit(indexAllMainThread);
        executor.shutdown();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {

        if (indexingIsRunning) {
            indexingIsRunning = false;
            IndexingResponse response = new IndexingResponse();
            response.setResult(true);


            while (!indexingIsStopped) {
                Thread.onSpinWait();

            }

            return response;
        } else {
            IndexingResponseWithError response = new IndexingResponseWithError();
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }
    }

    @Override
    public IndexingResponse addUpdatePage(String url) {

        Page page = extractPageAndSiteFromUrl(url);
        PageEntity pageEntity = page.getPageEntity();

        if (page.getSiteEntity() == null) {
            IndexingResponseWithError response = new IndexingResponseWithError();
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        if (page.getPageEntity() != null) {
            reduceRankForLemmasOnPage(pageEntity);
            deleteIndexesForPage(pageEntity);
            deleteLemmasWithZeroRankForSite(pageEntity.getSiteEntity());
            deletePage(pageEntity.getId());
            page.setPageEntity(null);

        }
        if (page.getPageEntity() == null) {//Need to download the page
            log.info("Downloading page: " + page.getSite() + page.getPath() + " site id: " + page.getSiteEntity().getId());
            try {
                Node node = new Node(page.getPath(), 0, this, page.getSiteEntity());
                node.savePage(page.getSiteEntity().getUrl() + page.getPath());
                int statusCode = node.getResponse().statusCode();
                if (!isOkStatusCode(statusCode)) {
                    return errorDuringPageOpening(statusCode);
                }
//                pageEntity = pageRepository.findByPathAndSite(pageEntity.getPath(), pageEntity.getSiteEntity().getId());
                pageEntity = pageRepository.findByPathAndSite(page.getPath(), page.getSiteId());
                log.info("path: " + pageEntity.getPath() + " siteid: " + pageEntity.getSiteEntity().getId());
                log.info("new pageEntity: " + pageEntity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        if (pageEntity != null) {
            int statusCode = pageEntity.getCode();
            if (!isOkStatusCode(statusCode)) {
                return errorDuringPageOpening(statusCode);
            }
        }



        ExecutorService executor = Executors.newFixedThreadPool(1);
        IndexSinglePageThread indexSinglePageThread = new IndexSinglePageThread(this, pageEntity);
        executor.submit(indexSinglePageThread);
        executor.shutdown();


        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }




    @Transactional
    public void indexPage(PageEntity pageEntity) {
        log.info("URL: " + pageEntity.getPath() + " site id: " + pageEntity.getSiteEntity().getId());
        boolean alreadyIndexed = (indexRepository.countLemmasByPage(pageEntity.getId()) > 0);
        if (alreadyIndexed) {
            log.info("Already indexed! " + pageEntity.getSiteEntity().getUrl() + pageEntity.getPath() + " " + pageEntity.getId());
            reduceRankForLemmasOnPage(pageEntity);
            deleteIndexesForPage(pageEntity);
            deleteLemmasWithZeroRankForSite(pageEntity.getSiteEntity());
        } else {
            log.info("Not indexed yet... starting indexing");
        }

        lemmitize(pageEntity);
        refreshStatusTime(pageEntity.getSiteEntity().getId());
    }


    private void reduceRankForLemmasOnPage(PageEntity page) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("update lemma set frequency = frequency - 1 where id in " +
                    "(select lemma_id from index_table where page_id = " + page.getId() + ")").executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void deleteIndexesForPage(PageEntity page) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("delete from index_table where page_id = " + page.getId()).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }


    private void deleteLemmasWithZeroRankForSite(SiteEntity site) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("delete from lemma where site_id = " + site.getId() + " and frequency < 1").executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void lemmitize(PageEntity page) {
        log.info("Starting lemmitize for " + page.getSiteEntity().getUrl() + page.getPath());
        HashMap<String, Integer> lemmas = lemmatizator.lemmatizeHTML(page.getContent());
        lemmas.forEach((s, count) -> {
//            log.info('"' + s + '"' + " - " + count + ". For page: " + page.getPath() + ". Then saveOrUpdateLemma, then saveIndex");
            LemmaEntity lemmaEntity = saveOrUpdateLemma(s, page);
            saveIndex(lemmaEntity, count, page);
        });


    }

    private void refreshStatusTime(int siteId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("UPDATE site SET status_time = '" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) +
                    "' where id = " + siteId).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }


    @Transactional
    private LemmaEntity saveOrUpdateLemma(String lemma, PageEntity page) {
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemmaForSite(lemma, page.getSiteEntity().getId());
        LemmaEntity lemmaEntity = null;
        if (!lemmaEntityList.isEmpty()) {
            lemmaEntity = lemmaEntityList.get(0);
        }

        if (lemmaEntity != null) {
            int pageCount = pageRepository.countPagesOnSite(page.getSiteEntity().getId());
//            log.info("Lemma found: " + lemmaEntity.toString());
            int frequency = lemmaEntity.getFrequency();
            lemmaEntity.setFrequency((frequency >= pageCount) ? pageCount : frequency + 1);
            lemmaRepository.saveAndFlush(lemmaEntity);
        } else {
//            log.info("Lemma " + '"' + lemma + '"' + " not found.");
            lemmaEntity = new LemmaEntity();
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setFrequency(1);
            lemmaEntity.setSiteEntity(page.getSiteEntity());
            lemmaRepository.saveAndFlush(lemmaEntity);
        }

        return lemmaEntity;

    }

    @Transactional
    private void saveIndex(LemmaEntity lemmaEntity, int count, PageEntity pageEntity) {
        IndexEntity indexEntity = indexRepository.findByPageAndLemma(lemmaEntity.getId(), pageEntity.getId());
        if (indexEntity == null) {
            indexEntity = new IndexEntity();
            indexEntity.setLemmaEntity(lemmaEntity);
            indexEntity.setPageEntity(pageEntity);
            indexEntity.setRank((float)count);
            indexRepository.saveAndFlush(indexEntity);
        } else if (indexEntity.getRank() != count) {
            indexEntity.setRank((float)count);
            indexRepository.saveAndFlush(indexEntity);
        }
    }

    @Transactional
    public void savePageAndRefreshStatusTime(PageEntity page) {
        pageRepository.saveAndFlush(page);
        refreshStatusTime(page.getSiteEntity().getId());
    }

    public boolean getIndexingIsRunning() {
        return indexingIsRunning;
    }

    public void createNewDeleteOld(Site siteInput, String status, String errorMessage) {

        siteRepository.findAllByUrl(siteInput.getUrl())
                .forEach(s -> {
                    deleteIndexesBySiteId(s.getId());
                    deleteLemmasBySiteId(s.getId());
                    deletePagesBySiteId(s.getId());
                    deleteSiteById(s);
                });

        SiteEntity siteEntity = newSiteEntity(siteInput, status, errorMessage);
        createNewSite(siteEntity);
    }

    public void updateSiteStatus(String status, String error, int siteId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("UPDATE site SET status_time = '" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) +
                    "', status = '" + status + "', last_error = '" + error + "' where id = " + siteId).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void deleteIndexesBySiteId(int siteId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM INDEX_TABLE WHERE page_id in (SELECT page_id from site where id = " + siteId + ")").executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void deleteLemmasBySiteId(int siteId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM LEMMA WHERE site_id = " + siteId).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void deletePagesBySiteId(int siteId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM PAGE where site_id = " + siteId).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private void deleteSiteById(SiteEntity site) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM SITE where id = " + site.getId()).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }

    private SiteEntity newSiteEntity(Site site, String status, String errorMessage) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        String url = site.getUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        siteEntity.setUrl(url);
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(Instant.now());
        siteEntity.setLastError(errorMessage);
        return siteEntity;
    }

    @Transactional
    private void createNewSite(SiteEntity site) {
        siteRepository.saveAndFlush(site);
    }


    public void deleteDuplicatedLemmasForSites() {
        Session session = sessionFactory.openSession();
//        String sql = "select lemma as lemma, min(id) as min_id, max(id) as max_id from lemma where lemma in (select lemma from lemma group by lemma, site_id having count(1) > 1) group by lemma";
        String sql = "with x as (\n" +
                "select lemma, site_id from lemma group by lemma, site_id having count(1) > 1\n" +
                ") select lemma as lemma, min(id) as min_id, max(id) as max_id from (\n" +
                "select l.* from x inner join lemma l on l.lemma = x.lemma and l.site_id = x.site_id) y\n" +
                "group by lemma";

        List<DuplicatedLemmas> duplicatedLemmas =  mapDuplicatedLemmasQueryResults(session.createSQLQuery(sql).list());
        if (duplicatedLemmas == null || duplicatedLemmas.isEmpty()) {
            log.info("Duplicated lemmas not found");

        } else {
            log.info("Duplicated lemmas FOUND:");
            for (DuplicatedLemmas duplicatedLemma : duplicatedLemmas) {
                log.info(duplicatedLemma.getLemma() + " - minID: " + duplicatedLemma.getMinId() +
                        " - maxID: " + duplicatedLemma.getMaxId());
            }
            handleDuplicatedLemmas(duplicatedLemmas);

        }
         session.close();


    }

    private List<DuplicatedLemmas> mapDuplicatedLemmasQueryResults(List<Object[]> result)
    {
        List<DuplicatedLemmas> list = new ArrayList<>();
        BigInteger b;

        for (Object[] object : result) {
            DuplicatedLemmas duplicatedLemmas = new DuplicatedLemmas();
            if (object[0] != null) {
                duplicatedLemmas.setLemma((String)object[0]);
            }
            if (object[1] != null) {
                b = (BigInteger)object[1];
                duplicatedLemmas.setMinId(b.intValue());
            }
            if (object[2] != null) {
                b = (BigInteger)object[2];
                duplicatedLemmas.setMaxId(b.intValue());
            }
            list.add(duplicatedLemmas);
        }
        return list;
    }

    private void handleDuplicatedLemmas(List<DuplicatedLemmas> duplicatedLemmas) {
        Transaction tx = null;
        for (DuplicatedLemmas duplicatedLemma : duplicatedLemmas) {
            try(Session session = sessionFactory.openSession()) {
                log.info("Starting trxn for lemma: " + duplicatedLemma.getLemma());
                int frequencyForMaxId = lemmaRepository.findById(duplicatedLemma.getMaxId()).get().getFrequency();
                int indexIdToBeUpdated = indexRepository.findIdByLemmaId(duplicatedLemma.getMaxId());

                tx = session.beginTransaction();
                session.createSQLQuery("update lemma set frequency = frequency + " + frequencyForMaxId +
                        " where id = " + duplicatedLemma.getMinId()).executeUpdate();
                session.createSQLQuery("update index_table set lemma_id = " + duplicatedLemma.getMinId() + " where id = " +
                        indexIdToBeUpdated).executeUpdate();
                session.createSQLQuery("delete from lemma where id =  " + duplicatedLemma.getMaxId()).executeUpdate();
                log.info("Commiting trxn for lemma: " + duplicatedLemma.getLemma());
                tx.commit();
                log.info("Trxn committed");
            } catch (HibernateException e) {
                log.info("Catched execption for trxns");
                e.printStackTrace();
                if (tx != null) {
                    tx.rollback();
                } else {
                    e.printStackTrace();
                }
            }

        }
    }


    public Page extractPageAndSiteFromUrl(String url) {
        if (!url.endsWith("/") && !url.toLowerCase().endsWith(".html")) {
            url += "/";
        }
        Page returnPage = new Page();
        for (Site siteInput : sites.getSites())
        {
            if (url.startsWith(siteInput.getUrl()) && url.substring(siteInput.getUrl().length()).startsWith("/")) {
                returnPage.setSite(siteInput.getUrl());
                returnPage.setPath(url.substring(siteInput.getUrl().length()));
                SiteEntity siteEntity = siteRepository.findByUrl(returnPage.getSite());
                returnPage.setSiteEntity(siteEntity);
                returnPage.setSiteId(siteEntity.getId());
                PageEntity pageEntity = pageRepository.findByPathAndSite(returnPage.getPath(), returnPage.getSiteEntity().getId());
                returnPage.setPageEntity(pageEntity);
                return returnPage;
            }
        }
        return returnPage;
    }

    private void createSitesInDbFromConfig()
    {
        for (Site siteInput : sites.getSites())
        {
            SiteEntity siteEntity = siteRepository.findByUrl(siteInput.getUrl());
            if (siteEntity == null || siteEntity.getName().isEmpty()) {
                log.info("Site: " + siteInput.getUrl() + " not found in DB, creating new record...");
                createNewDeleteOld(siteInput, "FAILED", "Full indexing was not started yet.");

            } else
            {
                log.info("Site: " + siteEntity.getUrl() + " FOUND in db");
            }

        }
    }

    public boolean isOkStatusCode(int statusCode) {
        String statusCodeStr = String.valueOf(statusCode);
        return statusCodeStr.startsWith("2") || statusCodeStr.startsWith("3");
    }

    private IndexingResponseWithError errorDuringPageOpening(int statusCode) {
        IndexingResponseWithError response = new IndexingResponseWithError();
        response.setResult(false);
        response.setError("Ошибка: " + statusCode + " при открытии указанной страницы");
        return response;
    }

    private void deletePage(int pageId) {
        Transaction tx = null;
        try(Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM PAGE where id = " + pageId).executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            } else {
                e.printStackTrace();
            }
        }
    }
}





