package searchengine.services.search;

import lombok.Data;

@Data
public class RankedLemmaOnPage {
    private int id;
    private String lemma;
    private float rank;
}
