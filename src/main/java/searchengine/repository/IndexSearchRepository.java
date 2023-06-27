package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexSearchRepository extends JpaRepository<IndexSearch,Integer> {
    @Query(value = "select * from index_search t where t.page_id = :pageId and t.lemma_id = :lemmaId",nativeQuery = true)
    IndexSearch indexSearchExist(@Param("pageId")Integer pageId,@Param("lemmaId")Integer lemmaId);

    @Query(value = "SELECT i.* FROM index_search i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<IndexSearch> findByPagesAndLemmas(@Param("lemmas") List<Lemma> lemmaListId,
                                           @Param("pages") List<Page> pageListId);



}
