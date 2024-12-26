package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.SiteEntity;
import java.util.List;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    void deleteByUrl(String url);
    void deleteAllByUrl(String url);
    List<SiteEntity> findAllByUrl(String url);
    SiteEntity findByUrl(String url);


    @Modifying
    @Query(value = "UPDATE site SET status_time = :newDateTime where site_id = :siteId", nativeQuery = true)
    void refreshStatusTime(int siteId, String newDateTime);

    @Query(value = "SELECT count(1) from site", nativeQuery = true)
    int countSites();

    @Query(value = "SELECT * from site where id = (select site_id from page where id = :pageId)", nativeQuery = true)
    SiteEntity findByPageId(int pageId);
}
