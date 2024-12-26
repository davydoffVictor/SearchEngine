package searchengine.services.search;

import lombok.Data;

@Data
public class Lemma {
    private int id;
    private String lemma;
    private int frequency;

}
