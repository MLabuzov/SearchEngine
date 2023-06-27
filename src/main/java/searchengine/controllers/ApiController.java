package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.SearchResults;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.URL;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchServiceImpl;
    private final SitesList sitesList;

    @GetMapping("/search")
    public SearchResults search(@RequestParam(name = "query", required = false, defaultValue = "")
                                String request, @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) throws IOException {
        return searchServiceImpl.search(request, site, offset, limit);
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        return indexingService.stopIndexing();
    }

    @GetMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam URL url) throws IOException {
        return indexingService.findLemmas(sitesList, url);
    }
}
