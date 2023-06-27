package searchengine.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Component
@Slf4j
public class LemmaHandler {

    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexRepository;

    public LemmaHandler(
        LemmaRepository lemmaRepository,
        IndexSearchRepository indexRepository
    ) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Transactional
    public void parseOnePage(Page pageEntity) {
        try {
            LemmaService lemmaService = LemmaService.getInstance();

            Set<String> lemmaSet = lemmaService.getLemmaSet(
                pageEntity.getContent()
            );
            Map<String, Integer> lemmasFromPage = lemmaService.getLemmas(
                pageEntity.getContent()
            );

            Set<IndexSearch> indexEntitySet = new HashSet<>();

            for (String setStr : lemmaSet) {
                if (lemmasFromPage.get(setStr) != null) {
                    synchronized (lemmaRepository) {
                        Optional<Lemma> optLemma = Optional.ofNullable(
                            lemmaRepository.findByLemmaAndSiteId(
                                setStr,
                                pageEntity.getSitePage().getId()
                            )
                        );
                        Lemma lemma = new Lemma();
                        lemma.setLemma(setStr);
                        lemma.setSitePage(pageEntity.getSitePage());
                        lemma.setFrequency(1);
                        if (optLemma.isPresent()) {
                            optLemma
                                .get()
                                .setFrequency(
                                    optLemma.get().getFrequency() + 1
                                );
                            lemma = lemmaRepository.save(optLemma.get());
                        } else {
                            lemma = lemmaRepository.save(lemma);
                        }
                        IndexSearch indexEntity = getIndexForLemma(
                            lemma,
                            pageEntity,
                            lemmasFromPage
                        );
                        indexEntitySet.add(indexEntity);
                    }
                }
            }
            indexRepository.saveAll(indexEntitySet);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге страницы");
        }
    }


    public Map<String, Integer> getLemmasFromText(String html) throws IOException {
        Map<String, Integer> lemmasInText = new HashMap<>();
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.replaceAll("(?U)\\pP","").toLowerCase().split(" ")));
        words.forEach(w -> determineLemma(w, luceneMorph,lemmasInText));
        return lemmasInText;
    }

    public List<String> getLemma(String word) throws IOException {

        LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
        List<String> lemmaList = new ArrayList<>();
        try {
            List<String> baseRusForm = russianLuceneMorphology.getNormalForms(word);
            if (!isServiceWord(word)) {
                lemmaList.addAll(baseRusForm);
            }
        } catch (Exception e) {
        }
        return lemmaList;
    }
    private boolean isServiceWord(String word) throws IOException {

        LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
        List<String> morphForm = russianLuceneMorphology.getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("ПРЕДЛ")
                    || l.contains("СОЮЗ")
                    || l.contains("МЕЖД")
                    || l.contains("МС")
                    || l.contains("ЧАСТ")
                    || l.length() <= 3) {
                return true;
            }
        }
        return false;
    }
    public List<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String el : elements) {
            List<String> lemmas = getLemma(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }


    private void determineLemma(String word, LuceneMorphology luceneMorphology,Map<String,Integer> lemmasInText) {
        try{
            if (word.isEmpty() || String.valueOf(word.charAt(0)).matches("[a-z]") || String.valueOf(word.charAt(0)).matches("[0-9]")) {
                return;
            }
            List<String> normalWordForms = luceneMorphology.getNormalForms(word);
            String wordInfo = luceneMorphology.getMorphInfo(word).toString();
            if (wordInfo.contains("ПРЕДЛ") || wordInfo.contains("СОЮЗ") || wordInfo.contains("МЕЖД")) {
                return;
            }
            normalWordForms.forEach(w -> {
                if (!lemmasInText.containsKey(w)) {
                    lemmasInText.put(w,1);
                } else {
                    lemmasInText.replace(w,lemmasInText.get(w) + 1);
                }
            });
        } catch (RuntimeException ex) {
            log.debug(ex.getMessage());
        }

    }

    public void getLemmasFromUrl(URL url) throws IOException {
        org.jsoup.Connection connect = Jsoup.connect(String.valueOf(url));
        Document doc = connect.timeout(60000).get();
        Map<String,Integer> res = getLemmasFromText(doc.body().html());
    }

    private IndexSearch getIndexForLemma(
        Lemma lemma,
        Page pageEntity,
        Map<String, Integer> lemmasFromPage
    ) {
        IndexSearch indexEntity = new IndexSearch();
        indexEntity.setLemma(lemma);
        indexEntity.setPage(pageEntity);
        indexEntity.setLemmaCount(lemmasFromPage.get(lemma.getLemma()));

        return indexEntity;
    }
}
