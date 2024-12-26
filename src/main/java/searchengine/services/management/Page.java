package searchengine.services.management;

import lombok.Data;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

@Data
public class Page {
    private String site;
    private String path;
    private int siteId;
    private SiteEntity siteEntity;
    private PageEntity pageEntity;
}
