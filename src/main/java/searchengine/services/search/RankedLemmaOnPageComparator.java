package searchengine.services.search;

import java.util.Comparator;

public class RankedLemmaOnPageComparator implements Comparator<RankedLemmaOnPage> {

    @Override
    public int compare(RankedLemmaOnPage o1, RankedLemmaOnPage o2) {
        int compare = -(o1.getRank() - o2.getRank());
        if (compare != 0) {
            return compare;
        }
        return o1.getLemma().compareTo(o2.getLemma());
    }
}