package searchengine.services.search;

import java.util.Comparator;

public class LemmaComparator implements Comparator<Lemma> {
    @Override
    public int compare(Lemma o1, Lemma o2) {
        int compare = o1.getFrequency() - o2.getFrequency();
        if (compare != 0) {
            return compare;
        }
        return o1.getLemma().compareTo(o2.getLemma());
    }
}
