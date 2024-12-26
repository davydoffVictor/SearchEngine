package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    private String status;

    @Column(name = "status_time", columnDefinition = "DATETIME NOT NULL")
    private Instant statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    @OneToMany(mappedBy = "siteEntity")
    private Set<PageEntity> pageSet = new HashSet<>();

    @Override
    public String toString() {
        return "SiteEntity{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
