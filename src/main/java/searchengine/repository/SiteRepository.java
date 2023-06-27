package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SitePage;
import searchengine.model.Status;

@Repository
public interface SiteRepository extends JpaRepository<SitePage, Integer> {
    SitePage findByUrl(String url);


    int countByStatus(Status status);

    SitePage findById(int siteId);

}
