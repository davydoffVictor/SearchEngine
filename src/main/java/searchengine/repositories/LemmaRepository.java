package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.LemmaEntity;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);
    List<LemmaEntity> findAllByLemma(String lemma);
    int countLemmasBySiteEntity(SiteEntity siteEntity);
}
