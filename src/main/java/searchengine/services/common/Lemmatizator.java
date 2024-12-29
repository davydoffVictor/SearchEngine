package searchengine.services.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class Lemmatizator {
    private final LuceneMorphology luceneMorph = initiateRussianLuceneMorphology();

    private LuceneMorphology initiateRussianLuceneMorphology() {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public HashMap<String, Integer> lemmatizeHTML(String text) {
        return lemmatize(removeHTMLTags(text));
    }

    public HashMap<String, Integer> lemmatize(String text) {
        HashMap<String, Integer> returnMap = new HashMap<>();

        String[] words = splitOnWordsAndLower(text);
        log.info("Split on " + words.length + " words.");


        for (String word : words) {
            List<String> lemmas = getValidLemmas(word);
            if (lemmas.isEmpty()) {
                continue;
            }
            for (String lemma : lemmas) {
                if (!returnMap.containsKey(lemma)) {
                    returnMap.put(lemma, 1);
                } else {
                    int count = returnMap.get(lemma);
                    returnMap.put(lemma, count + 1);
                }
            }
        }
        return returnMap;
    }


    public List<String> getValidLemmas(String word) {
        List<String> returnLemmaList = new ArrayList<>();

        word = word.toLowerCase();
        boolean isValidWord = false;
        if (luceneMorph != null) {
            isValidWord = luceneMorph.checkString(word);
        }
        if (!isValidWord || word.isEmpty() || word.equals("при") || word.equals("из") || word.equals("мне")) {
            return List.of();
        }
        List<String> lemmaList = luceneMorph.getNormalForms(word);
        String lemma = lemmaList.get(0);

        List<String> morphInfoList = luceneMorph.getMorphInfo(lemma);

        for (String morphInfo : morphInfoList) {

            if (morphInfo.contains("МЕЖД") ||
                    morphInfo.contains("КР_ПРИЛ")
                    || morphInfo.contains("СОЮЗ")
                    || morphInfo.contains("ПРЕДЛ")
                    || morphInfo.contains("А С")
                    || morphInfo.contains("МС-П")
                    || morphInfo.contains("ЧАСТ")
            ) {
//                    log.info(" - false");
                continue;
            } else {
//                    log.info(" - true");
                returnLemmaList.add(lemma);
            }
        }
        return returnLemmaList;
    }

    public String[] splitOnWordsAndLower(String s) {

        return splitOnWords(s.toLowerCase());
    }

    public String[] splitOnWords(String s) {
        String regEx = "[\\s.,!?;0-9]+";
        return s.split(regEx);
    }

    public String removeHTMLTags(String s) {
        s = Jsoup.parse(s).text();
        String regEx = "[^а-яёА-ЯЁ\\s]";
        return s.replaceAll(regEx, " ");
    }

    public String removeHTMLTagsEnglishWordsAndDigitsRemains(String s)
    {
        s =  Jsoup.parse(s).text();
        String regEx = "[^а-яёА-ЯЁa-zA-Z0-9\\s]]";
        return s.replaceAll(regEx, " ");
    }

    public String pureWord(String s) {
        String regEx = "[«»„“©:" + '"' + "]";
        return s.replaceAll(regEx, "");
    }
}
