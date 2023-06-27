package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.component.LemmaHandler;
import searchengine.component.PageFinder;
import searchengine.component.PageIndexer;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    @Autowired
    private PageIndexer pageIndexer;
    @Autowired
    private LemmaHandler lemmaHandler;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SitePage> sitePagesAllFromDB;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private AtomicBoolean indexingProcessing = new AtomicBoolean(false);

    @Override
    public ResponseEntity<String> findLemmas(SitesList sitesList, URL url) throws IOException {

        try {
            sitesList.getSites().stream().filter(site -> url.getHost().equals(site.getUrl().getHost())).findFirst().orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("result: false " +
                    "error: Данная страница находится за пределами сайтов " +
                    "указанных в конфигурационном файле");
        }
        lemmaHandler.getLemmasFromUrl(url);
        return ResponseEntity.status(HttpStatus.OK).body("result: true");
    }

    @Override
    public ResponseEntity startIndexing() {
        if (indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("'result' : false, " +
                    "'error' : Индексация уже запущена");
        } else {
            indexingProcessing.set(true);
            Runnable start = () -> startIndexing(indexingProcessing);
            new Thread(start).start();
            return ResponseEntity.status(HttpStatus.OK).body("'result' : true");
        }
    }

    @Override
    public ResponseEntity stopIndexing() {
        if (!indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("'result' : false, " +
                    "'error' : Индексация не запущена");
        } else {
            indexingProcessing.set(false);
            return ResponseEntity.status(HttpStatus.OK).body("'result' : true ");
        }
    }

    private void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            logger.error("Error: ", ex);
        }
    }

    private void deleteSitePagesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl())) {
                    siteRepository.deleteById(sitePageDb.getId());
                }
            }
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePageDAO = new SitePage();
            sitePageDAO.setStatus(Status.INDEXING);
            sitePageDAO.setName(siteApp.getName());
            sitePageDAO.setUrl(siteApp.getUrl().toString());
            siteRepository.save(sitePageDAO);
        }

    }

    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SitePage siteDomain : sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    System.out.println("Запущена индексация " + siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(siteRepository,pageRepository,siteDomain, "", resultForkJoinPageIndexer, connection, lemmaHandler,pageIndexer,indexingProcessing));
                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId());
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }
                if (!indexingProcessing.get()) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId());
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    System.out.println("Проиндексирован сайт: " + siteDomain.getName());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId());
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }
}
