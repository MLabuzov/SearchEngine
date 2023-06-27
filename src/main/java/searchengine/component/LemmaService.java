package searchengine.component;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaService {

    private static final String REGEX_TO_SPLIT = "[^а-яё\\s]";
    private static final String REGEX_TO_REMOVE_TAGS =
        "<[^>]+>|\\p{Punct}|\\{[^}]*}";
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String REGEX_CYRILLIC_ONLY = "[^\\p{IsCyrillic}\\s]+";
    private static final String[] PARTICLES_NAMES = new String[] {
        "МЕЖД",
        "ПРЕДЛ",
        "СОЮЗ",
        "ЧАСТ",
    };
    private static LuceneMorphology luceneMorphology;

    public LemmaService(LuceneMorphology morphology) {
        luceneMorphology = morphology;
    }

    public static LemmaService getInstance() throws IOException {
        LuceneMorphology morphology = new RussianLuceneMorphology();

        return new LemmaService(morphology);
    }

    public Map<String, Integer> getLemmas(String text) throws IOException {
        Map<String, Integer> lemmas = new HashMap<>();
        text = removeTagsFromText(text);

        if (text == null || text.length() == 0) {
            return lemmas;
        }
        String[] words = text
            .toLowerCase(Locale.ROOT)
            .replaceAll(REGEX_TO_SPLIT, " ")
            .trim()
            .split("\\s+");

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            word = word.trim();
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);

            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);

            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0).replaceAll("ё", "е");

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, (lemmas.get(normalWord) + 1));
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }

    public Set<String> getLemmaSet(String text) {
        text = removeTagsFromText(text);
        Set<String> lemmaSet = new HashSet<>();

        if (text == null || text.length() == 0) {
            return lemmaSet;
        }

        String[] words = text
            .toLowerCase(Locale.ROOT)
            .replaceAll(REGEX_TO_SPLIT, " ")
            .replaceAll("ё", "е")
            .trim()
            .split("\\s+");

        for (String word : words) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(
                    word
                );
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }

    public String getOneLemma(String word) {
        String correctWord = word
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^а-яё\\s]", "")
            .trim();

        if (correctWord.length() > 0) {
            return luceneMorphology.getNormalForms(correctWord).get(0);
        } else {
            return null;
        }
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);

        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    public static String removeTagsFromText(String content) {
        return content
            .replaceAll(REGEX_TO_REMOVE_TAGS, " ")
            .replaceAll(REGEX_CYRILLIC_ONLY, " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
