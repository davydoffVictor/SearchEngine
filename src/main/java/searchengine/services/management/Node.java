package searchengine.services.management;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Logger;

@Getter
@Setter
public class Node {
    public static TreeSet<String> pages = new TreeSet<>();
    private static String userAgent = "DavydoffSearchBot";//Default value, can be overwritten in configuration file
    private static String referrer = "http://www.google.com";//Default value, can be overwritten in configuration file
    private SiteEntity siteEntity;
    private String url;
    private int level;
    private static final int MAX_RECURSIVE_LEVEL = 25;
    private Document doc;
    private Elements elements;
    private StringBuilder html;
    private ManagementServiceImpl service;
    private Connection.Response response;
    private static final Logger log = Logger.getLogger(Node.class.getName());




    public Node(String url, int level, ManagementServiceImpl service, SiteEntity siteEntity) throws IOException {
        this.url = url;
        this.level = level;
        this.service = service;
        this.siteEntity = siteEntity;
        try {
            response = Jsoup.connect(siteEntity.getUrl() + url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .ignoreHttpErrors(true)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc = response.parse();
        html = new StringBuilder(doc.outerHtml());
        elements = doc.select("a[href]");


    }

    @Transactional
    public Collection<Node> getChildren() throws InterruptedException {
        if (!service.getIndexingIsRunning()) {
            return new ArrayList<>();
        }

        TreeSet<String> refsOnLevel = new TreeSet<>();
        log.info("Starting exploring " + url + " Recursive level " + level);
        Collection<Node> returnCollection = new ArrayList<>();
        Page page = service.extractPageAndSiteFromUrl(url);
        PageEntity pageEntity = savePage(siteEntity.getUrl() + url);


        if (level >= MAX_RECURSIVE_LEVEL) {
            log.info("MAXIMUM RECURSIVE LEVEL " + level + " ACHIEVED for " + url);
            return new ArrayList<>();
        }

        if (service.isOkStatusCode(pageEntity.getCode())) {
            service.indexPage(pageEntity);
        }

        for (Element element : elements) {
            String ref = okRef(element.attr("href"));
            if (!ref.isEmpty()) {
                refsOnLevel.add(siteEntity.getUrl() + ref);
                log.info("Added to DB: " + siteEntity.getUrl() + ref);
            }
        }

        if (!service.getIndexingIsRunning()) {
            return new ArrayList<>();
        }

        synchronized (Node.pages) {
            Node.pages.addAll(refsOnLevel);

        }

        if (!service.getIndexingIsRunning()) {
            return new ArrayList<>();
        }

        refsOnLevel.forEach( r -> {
            log.info("Adding new node for: " + r);
            try {
                returnCollection.add(new Node(service.extractPageAndSiteFromUrl(r).getPath(),  level + 1, service, siteEntity));
            } catch (IOException e) {
                log.info("Error occured: " + e.getMessage() + " R: " + r );
                e.printStackTrace();
            }
        });
        log.info("Added " + refsOnLevel.size() + " new links in database from " + url);

        return returnCollection;
    }

    public PageEntity savePage(String ref) {
        log.info("Node.savePage ref: " + ref);
//            SiteEntity site = new SiteEntity();
        Page page = service.extractPageAndSiteFromUrl(ref);
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPath(page.getPath());
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setCode(response.statusCode());
        pageEntity.setContent(html.toString());
        service.savePageAndRefreshStatusTime(pageEntity);
        log.info("Saved to DB: " + ref);
        return pageEntity;
    }

    public String okRef(String ref) {
        if (!isValidLink(ref)) {
            return "";
        }

        if (ref.startsWith(siteEntity.getUrl())) {
            ref = ref.substring(siteEntity.getUrl().length());
        }

        if (ref.contains(".") && !ref.endsWith("/") && !ref.toLowerCase().endsWith(".html")) {//It's filename
            return "";
        }

        if (!ref.endsWith("/") && !ref.contains(".") && !ref.toLowerCase().endsWith(".html")) {
            ref += "/";
        }

        synchronized (Node.pages) {
            if (Node.pages.contains(siteEntity.getUrl() + ref)) {
                return "";
            }
        }

        return ref;
    }

    private boolean isValidLink(String ref) {
        if (ref.contains("#") || ref.contains("?")) {
            return false;
        }
        if (!(ref.startsWith("/") || ref.startsWith(siteEntity.getUrl() + "/"))) {
            return false;
        }

        return !ref.equals("/");
    }



    public static void setUserAgent(String userAgent) {
        Node.userAgent = userAgent;
    }


    public static void setReferrer(String referrer) {
        Node.referrer = referrer;
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static String getReferrer() {
        return referrer;
    }
}
