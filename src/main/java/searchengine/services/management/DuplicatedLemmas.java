package searchengine.services.management;

import lombok.Data;

import java.math.BigInteger;

@Data
public class DuplicatedLemmas {
    private String lemma;
    private Integer minId;
    private Integer maxId;

}
