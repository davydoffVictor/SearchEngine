package searchengine.services.management;

import lombok.Data;

@Data
public class DuplicatedLemmas {
    private String lemma;
    private Integer minId;
    private Integer maxId;
}
