package searchengine.services.search;

import java.util.Comparator;

public class PageWithRelevanceComparator implements Comparator<PageWithRelevance> {

    @Override
    public int compare(PageWithRelevance o1, PageWithRelevance o2) {
        float compare = -(o1.getRelevance() - o2.getRelevance());
        if (compare < 0) {
            return -1;
        }
        if (compare > 0) {
            return 1;
        }
        return o1.getPageEntity().getId() - o2.getPageEntity().getId();
    }
}
