package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer>{
    void deleteAllBySiteEntity(SiteEntity site);
    List<PageEntity> findAllByPath(String path);

    @Modifying
    @Query(value = "DELETE from page where site_id = :siteId", nativeQuery = true)
    void deleteAllBySiteId(int siteId);

    @Query(value = "SELECT * from page where site_id = :siteId", nativeQuery = true)
    List<PageEntity> findAllBySiteId(int siteId);


    @Query(value = "SELECT count(1) from page where site_id = :siteId", nativeQuery = true)
    int countPagesOnSite(int siteId);

    @Query(value = "SELECT count(1) from page", nativeQuery = true)
    int countPages();

    @Query(value = "SELECT * from page where path = :path and site_id = :siteId", nativeQuery = true)
    PageEntity findByPathAndSite(String path, int siteId);

}
