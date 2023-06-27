package searchengine.services;

import searchengine.dto.SearchResults;
import searchengine.dto.search.StatisticsSearch;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    SearchResults search(String request, String site,
                         int offset,
                         int limit) throws IOException;

    List<StatisticsSearch> allSiteSearch(String searchText, int offset, int limit) throws IOException;
}
