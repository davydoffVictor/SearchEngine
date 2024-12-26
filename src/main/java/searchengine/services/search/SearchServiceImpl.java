package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseWithData;
import searchengine.dto.search.SearchResponseWithError;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.common.Lemmatizator;
import searchengine.services.SearchService;

import java.util.*;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final int DEFAULT_LEMMA_TOO_FREQUENT_THRESHOLD = 75;//In percents. Can be redefined in config file
    private double lemmaThreshold = DEFAULT_LEMMA_TOO_FREQUENT_THRESHOLD;
    private static final int MIN_FREQUENCY_FOR_LEMMA_THRESHOLD = 5;
    private static final int WORDS_TO_SHOW_BEFORE = 4;
    private static final int WORDS_TO_SHOW_AFTER = 33;
    private int totalPages = 0;
    private int siteIdForSearch = 0;
    private String lastSearchQuery = "";
    private TreeSet<Lemma> lemmas;
    private ArrayList<Integer> pages;
    private SearchResponseWithData searchResponse = new SearchResponseWithData();
    private final Logger log = Logger.getLogger(SearchServiceImpl.class.getName());

    private final Lemmatizator lemmatizator;
    private final SitesList sites;//From configuration file
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SessionFactory sessionFactory;



    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        initThreshold();
        log.info(query + " - " + "site: " + site + " - " + offset + " - " + limit + " lastSearchQuery: " + lastSearchQuery
                + " searchResponseCount: " + searchResponse.getCount());
        if (query.isEmpty()) {
            SearchResponseWithError response = new SearchResponseWithError();
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }
        if (site == null) {
            site = "";
        }

        boolean isNewSearch = (!query.equals(lastSearchQuery) || searchResponse.getCount() < 10 || (offset == 0));
        log.info(" - new search: " + isNewSearch);

        if (isNewSearch) {
            siteIdForSearch = (site.isEmpty()) ? 0 : siteRepository.findByUrl(site).getId();
            makeNewSearch(query, site);
            lastSearchQuery = query;
            searchResponse = makeResponseForSearch(pages, lemmas);
        }

        return responseWithOffsetAndLimit(offset, limit);
    }


    private void initThreshold() {
        lemmaThreshold = (sites.getLemma_threshold() == 0) ? (double)DEFAULT_LEMMA_TOO_FREQUENT_THRESHOLD / 100 : (double)sites.getLemma_threshold() / 100;
    }



    private HashMap<Integer, Integer> getNumberOfPagesOnSites(String site) {
        HashMap<Integer, Integer> returnMap = new HashMap<>();
        totalPages = 0;
        if (!site.isEmpty()) {
            SiteEntity siteEntity = siteRepository.findByUrl(site);
            log.info("Found. ID: " + siteEntity.getId() + " Name: " + siteEntity.getUrl());
            int siteId = siteEntity.getId();
            int pagesOnSite = pageRepository.countPagesOnSite(siteId);
            totalPages += pagesOnSite;
            returnMap.put(siteId, pagesOnSite);
            return returnMap;
        }

        for (Site siteInput : sites.getSites()) {
            log.info("Searching for all sites");
            SiteEntity siteEntity = siteRepository.findByUrl(siteInput.getUrl());
            log.info("Found. ID: " + siteEntity.getId() + " Name: " + siteEntity.getUrl());
            int siteId = siteEntity.getId();
            int pagesOnSite = pageRepository.countPagesOnSite(siteId);
            returnMap.put(siteId, pagesOnSite);
            totalPages += pagesOnSite;
        }

        return  returnMap;
    }

    private TreeSet<Lemma> makeSortedByFrequencyLemmaList(String[] words) {
        //образ ошибка один пробка туалет
        TreeSet<Lemma> lemmas = new TreeSet<>(new LemmaComparator());
        log.info("Разбивка на слова и леммы поискового запроса:");
        for (String word : words) {
            Lemma lemma = new Lemma();

            String lemmaS = (!lemmatizator.getValidLemmas(word).isEmpty()) ? lemmatizator.getValidLemmas(word).get(0) : "";
            log.info(word + " - lemma: " + lemmaS);
            if (!lemmaS.isEmpty()) {
                List<LemmaEntity> lemmaEntities = lemmaRepository.findAllByLemma(lemmaS);
                if (lemmaEntities == null) {
                    lemma.setLemma(lemmaS);
                    lemma.setFrequency(0);
                    lemmas.add(lemma);
                } else {
                    int totalFrequencyForLemma = 0;
                    for (LemmaEntity lemmaEntity : lemmaEntities) {
                        if (siteIdForSearch == 0 || lemmaEntity.getSiteEntity().getId() == siteIdForSearch) {
                            totalFrequencyForLemma += lemmaEntity.getFrequency();
                        }
                    }
                    if (((double) totalFrequencyForLemma / (double) totalPages) < lemmaThreshold
                            || totalFrequencyForLemma < MIN_FREQUENCY_FOR_LEMMA_THRESHOLD)
                    {
                        log.info("----->OK Lemma: " + lemmaS + " found in DB, threshold OK. " + "LemmaFrequency " + totalFrequencyForLemma + " totalPages: " +
                                totalPages + " Lemma threshold: " + lemmaThreshold + ", adding to searchlist");
                        lemma.setFrequency(totalFrequencyForLemma);
                        lemma.setLemma(lemmaS);
                        lemmas.add(lemma);
                    } else {
                        log.info("Lemma " + lemmaS + " found in DB, threshold NOT OK. LemmaFrequency: " + totalFrequencyForLemma + " totalPages: " + totalPages
                                + " Lemma threshold: " + lemmaThreshold);
                    }

                }
            }
        }
        return lemmas;
    }


    private ArrayList<Integer> returnSuitablePages(TreeSet<Lemma> lemmas, String site) {
        int siteId;
        ArrayList<Integer> pages = new ArrayList<>();

        if (lemmas.isEmpty()) {
            return pages;
        }
        if (lemmas.first().getFrequency() == 0) {
            return pages;
        }

        if (site.isEmpty()) {
            siteId = 0;
            log.info("Search all sites");
        } else {
            siteId = siteRepository.findByUrl(site).getId();
            log.info("Search 1 specific site ID: " + siteId);
        }



        for (Lemma lemma : lemmas) {
            pages = findAllPagesIDByLemmaAndPageList(lemma.getLemma(), pages, siteId);
            if (!lemma.equals(lemmas.first()) && pages.isEmpty()) {
                return pages;
            }
        }


        return pages;
    }


    private SearchResponseWithData makeResponseForSearch(ArrayList<Integer> pages, TreeSet<Lemma> lemmas) {
        SearchResponseWithData response = new SearchResponseWithData();
        response.setResult(true);

        if (pages.isEmpty()) {
            response.setData(List.of());
            response.setCount(0);
            return response;
        }

        ArrayList<PageEntity> pageEntities = (ArrayList<PageEntity>) pageRepository.findAllById(pages);
        TreeSet<PageWithRelevance> sortedByRelevancePages = calculateRelevanceAndSortPagesByRelevance(pageEntities, lemmas);
        sortedByRelevancePages.forEach(( page) ->
                log.info(page.getRelevance() + " - page: " + page.getPageEntity().getId() + " "
                        + page.getPageEntity().getSiteEntity().getUrl() + page.getPageEntity().getPath()));

        List<SearchData> searchDataList = new ArrayList<>();

        for (PageWithRelevance page : sortedByRelevancePages) {
            SearchData searchData = new SearchData();
            SiteEntity site = page.getPageEntity().getSiteEntity();
            Document document = Jsoup.parse(page.getPageEntity().getContent(), site.getUrl());

            searchData.setUri(page.getPageEntity().getPath());
            searchData.setSite(site.getUrl());
            searchData.setTitle(document.title());
            searchData.setRelevance(page.getRelevance());
            searchData.setSnippet(getSnippet(page.getPageEntity(), lemmas));
            searchData.setSiteName(site.getName());
            searchDataList.add(searchData);
        }



        response.setData(searchDataList);
        response.setCount(searchDataList.size());


        return response;
    }

    private ArrayList<Integer> findAllPagesIDByLemmaAndPageList(String lemma, List<Integer> pages, int siteID) {


        StringBuilder pagesStringForQuery = new StringBuilder();
        Session session = sessionFactory.openSession();

        String lemmaSqlPart = "select id from lemma where lemma in ('" + lemma + "')";
        if (siteID != 0) {
            lemmaSqlPart += " and site_id=" + siteID;
        }

        String sql = "";
        sql = "select id from page where id in (select page_id from index_table where lemma_id in(" +
                lemmaSqlPart + "))";


        if (!pages.isEmpty()) {
            for (int page : pages) {
                pagesStringForQuery.append(page).append(",");
            }
            pagesStringForQuery.delete(pagesStringForQuery.length() - 1, pagesStringForQuery.length());
//            log.info("Formed string for query: " + pagesStringForQuery.toString());
            sql += " and id in(" + pagesStringForQuery + ")";
        }
        log.info("Final sql string:\n" + sql);

        ArrayList<Integer> pagesFromQuery = (ArrayList<Integer>) session.createSQLQuery(sql).list();
        log.info("Returned: " + pagesFromQuery.size() + " " + pagesFromQuery);
        session.close();
        return pagesFromQuery;
    }

    private TreeSet<PageWithRelevance> calculateRelevanceAndSortPagesByRelevance(List<PageEntity> pageEntities, Set<Lemma> lemmas) {
        TreeSet<PageWithRelevance> returnSet = new TreeSet<>(new PageWithRelevanceComparator());
        if (pageEntities.isEmpty() || lemmas.isEmpty()) {
            return returnSet;
        }
        int numberOfPages = pageEntities.size();
        int numberOfLemmas = lemmas.size();
        float maxAbsRelevance = 0;
        float[] absPageRank = new float[numberOfPages];
        float[] relPageRank = new float[numberOfPages];
        float[][] lemmaRankOnPage = new float[numberOfPages][numberOfLemmas];

        int i = 0;
        for (PageEntity page : pageEntities) {

            int j = 0;
            for (Lemma lemma : lemmas) {
                lemmaRankOnPage[i][j] = indexRepository.getRankForLemmaAndPage(lemma.getLemma(), page.getId());
                absPageRank[i] += lemmaRankOnPage[i][j];
                j++;
            }
            if (absPageRank[i] > maxAbsRelevance) {
                maxAbsRelevance = absPageRank[i];
            }

            i++;
        }

        i = 0;
        for (PageEntity page : pageEntities) {
            relPageRank[i] = absPageRank[i] / maxAbsRelevance;
            PageWithRelevance pageWithRelevance = new PageWithRelevance();
            pageWithRelevance.setPageEntity(page);
            pageWithRelevance.setRelevance(relPageRank[i]);
            returnSet.add(pageWithRelevance);
            i++;
        }

        //------------------------------Logging
        i = 0;
        String logStr = "";
        for (PageEntity page : pageEntities) {
            logStr += (i + 1 + "\t" + page.getId() + "\t");
            int j = 0;
            for (Lemma lemma : lemmas) {
                logStr += (lemma.getLemma() + ": " + lemmaRankOnPage[i][j] + "\t");
                j++;
            }
            logStr += "Abs rel: " + absPageRank[i] + "\tRel rel: " + relPageRank[i] + "\t" + page.getSiteEntity().getUrl() + page.getPath();
            log.info(logStr);
            logStr = "";
            i++;
        }
        //------------------end logging


        return returnSet;
    }


    private String getSnippet(PageEntity page, TreeSet<Lemma> lemmas) {

        if (page == null || lemmas == null) {
            return "";
        }
        if (lemmas.isEmpty()) {
            return "";
        }
        String[] words = lemmatizator.splitOnWords(lemmatizator.removeHTMLTagsEnglishWordsAndDigitsRemains(page.getContent()));
//        String[] words = lemmatizator.splitOnWords(lemmatizator.removeHTMLTags(page.getContent()));
        if (words.length == 0) {
            return "";
        }

        TreeSet<RankedLemmaOnPage> sortedByRankLemmasOnPageSet = new TreeSet<>(new RankedLemmaOnPageComparator());
        for (Lemma lemma : lemmas) {
            int rank = indexRepository.getRankForLemmaAndPage(lemma.getLemma(), page.getId());
            RankedLemmaOnPage rankedLemmaOnPage = new RankedLemmaOnPage();
            rankedLemmaOnPage.setRank(rank);
            rankedLemmaOnPage.setLemma(lemma.getLemma());
            sortedByRankLemmasOnPageSet.add(rankedLemmaOnPage);
        }

        ArrayList<String> fragments = getAllFragments(words, sortedByRankLemmasOnPageSet.first());
        if (fragments.isEmpty()) {//use old method (when indexing)
            String s = lemmatizator.removeHTMLTags(page.getContent());
            words = lemmatizator.splitOnWords(s);
            fragments = getAllFragments(words, sortedByRankLemmasOnPageSet.first());
        }
        TreeSet<Fragment> rankedFragmentsSet = sortFragmentByRankAndBoldLemmas(fragments, lemmas);

        return (!rankedFragmentsSet.isEmpty()) ? rankedFragmentsSet.first().getStr() : "";
    }

    private ArrayList<String> getAllFragments(String[] words, RankedLemmaOnPage lemma) {
        String str = "";
        ArrayList<String> returnList = new ArrayList<>();
        if (words.length == 0 || lemma == null) {
            return returnList;
        }

        int i = 0;
        int foundPosition = 0;
        for (String word : words) {
            List<String> lemmasForWord = lemmatizator.getValidLemmas(lemmatizator.pureWord(word));
            if (lemmasForWord.isEmpty()) {
                i++;
                continue;
            }
            if (lemmasForWord.get(0).equals(lemma.getLemma()))
            {
                foundPosition = i;
                str = makeTextFragment(words, foundPosition);
                returnList.add(str);
            }

            i++;
        }


        return returnList;
    }

    private String makeTextFragment(String[] words, int position) {


        StringBuilder str = new StringBuilder();
        int startPosition;
        int endPosition;

        startPosition = Math.max((position - WORDS_TO_SHOW_BEFORE), 0);
        endPosition = startPosition +  WORDS_TO_SHOW_AFTER - 1;

        if (endPosition >= words.length) {
            endPosition = words.length - 1;
        }

        if (endPosition - startPosition < WORDS_TO_SHOW_AFTER) {
            startPosition = Math.max(0, endPosition - WORDS_TO_SHOW_AFTER);
        }
//        log.info("Startposition: " + startPosition + " End_POS: " + endPosition + " found: " + position);

        for (int i = startPosition; i <= endPosition; i++) {
            str.append(words[i]).append(" ");
        }

        return str.toString();
    }

    private TreeSet<Fragment> sortFragmentByRankAndBoldLemmas(List<String> fragments, TreeSet<Lemma> lemmas)
    {
        TreeSet<Fragment> returnSet = new TreeSet<>(new FragmentComparator());
        if (fragments.isEmpty() || lemmas.isEmpty()) {
            return returnSet;
        }

        for (String fragmentStr : fragments) {
            StringBuilder returnStr = new StringBuilder();
            int rank = 0;
            for (String word : lemmatizator.splitOnWords(fragmentStr)) {
                String pureWord = lemmatizator.pureWord(word);
                String lemmaStr = (!lemmatizator.getValidLemmas(pureWord).isEmpty()) ? lemmatizator.getValidLemmas(pureWord).get(0) : "";
                boolean isLemma = false;
                for (Lemma lemma : lemmas) {
                    if (lemma.getLemma().equals(lemmaStr)) {
                        rank++;
                        isLemma = true;
                    }
                }
                if (isLemma) {
                    returnStr.append("<b>").append(word).append("</b> ");
                } else {
                    returnStr.append(word).append(" ");
                }
            }
            Fragment fragment = new Fragment();
            fragment.setRank(rank);
            fragment.setStr(returnStr.toString());
            returnSet.add(fragment);
        }
        return returnSet;
    }

    private void makeNewSearch(String query, String site) {
        log.info("Threshold: " + lemmaThreshold);
        HashMap<Integer, Integer> pagesOnsite = getNumberOfPagesOnSites(site);
        pagesOnsite.forEach(
                (id, count) -> log.info("id: " + id + " pagesCount: " + count)
        );

        lemmas = makeSortedByFrequencyLemmaList(lemmatizator.splitOnWordsAndLower(query));


        log.info("Выводим леммы. Их " + lemmas.size());
        for (Lemma lemma : lemmas) {
            log.info('"' + lemma.getLemma() + '"' + " frequency: " + lemma.getFrequency());


        }

        pages = returnSuitablePages(lemmas, site);
        log.info("Founded pages: " + pages.toString());
    }

    private SearchResponseWithData responseWithOffsetAndLimit(int offset, int limit) {
     SearchResponseWithData returnResponse = new SearchResponseWithData();
     returnResponse.setResult(searchResponse.isResult());
     returnResponse.setCount(searchResponse.getCount());
     int numberOfPages = searchResponse.getData().size();
     int statPosition = offset;
     if (statPosition > numberOfPages) {
         statPosition = numberOfPages;
     }
     int finalPosition = offset + limit;
     if (finalPosition > numberOfPages) {
         finalPosition = numberOfPages;
     }

     ArrayList<SearchData> returnSearchDataList = new ArrayList<>();

     for (int i = statPosition; i < finalPosition; i++) {
         returnSearchDataList.add(searchResponse.getData().get(i));
     }
     returnResponse.setData(returnSearchDataList);
     return returnResponse;
    }
}
