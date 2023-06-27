package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.SitesList;

import java.io.IOException;
import java.net.URL;

public interface IndexingService {
    ResponseEntity<String> findLemmas(SitesList sitesList, URL url) throws IOException;

    ResponseEntity startIndexing();

    ResponseEntity stopIndexing();
}
