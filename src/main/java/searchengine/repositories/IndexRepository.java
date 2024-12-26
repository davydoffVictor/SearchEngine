package searchengine.repositories;

import org.jboss.jandex.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Query(value = "SELECT * from index_table where lemma_id = :lemmaId and page_id = :pageId", nativeQuery = true)
    IndexEntity findByPageAndLemma(int lemmaId, int pageId);

    @Query(value = "SELECT count(*) from index_table where page_id = :pageId", nativeQuery = true)
    int countLemmasByPage(int pageId);

    @Query(value = "SELECT rank_field from index_table i inner join lemma l on i.lemma_id = l.id where lemma = :lemma and page_id=:pageId", nativeQuery = true)
    int getRankForLemmaAndPage(String lemma, int pageId);

    @Query(value = "SELECT id from index_table where lemma_id = :lemmaId", nativeQuery = true)
    int findIdByLemmaId(int lemmaId);
}
