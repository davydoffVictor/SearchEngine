package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.services.management.DuplicatedLemmas;
import searchengine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    LemmaEntity findByLemma(String lemma);

    List<LemmaEntity> findAllByLemma(String lemma);

    @Query(value = "SELECT * from lemma where lemma = :lemma and site_id = :siteId", nativeQuery = true)
    List<LemmaEntity> findAllByLemmaForSite(String lemma, int siteId);

    @Query(value = "SELECT count(1) from lemma", nativeQuery = true)
    int countLemmas();

    @Query(value = "SELECT count(1) from lemma where site_id = :siteId", nativeQuery = true)
    int countLemmasOnSite(int siteId);

    @Query(value = "select lemma as lemma, min(id) as minId, max(id) as maxId from lemma where lemma in (select lemma from lemma group by lemma, site_id having count(1) > 1) group by lemma", nativeQuery = true)
    List<DuplicatedLemmas> findDuplicatedLemmasForSite();
}
