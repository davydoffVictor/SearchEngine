package searchengine.services.search;

import java.util.Comparator;

public class FragmentComparator implements Comparator<Fragment> {

    @Override
    public int compare(Fragment o1, Fragment o2) {
        int compare = -(o1.getRank() - o2.getRank());
        if (compare != 0) {
            return compare;
        }
        return o1.getStr().compareTo(o2.getStr());
    }
}
