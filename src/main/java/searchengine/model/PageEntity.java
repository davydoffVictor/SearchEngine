package searchengine.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "page")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    private SiteEntity siteEntity;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, Index(path(512))")
    private String path;

    @Column(name = "code", columnDefinition = "INT NOT NULL")
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @Override
    public String toString() {
        return "PageEntity{" +
                "id=" + id +
                ", siteEntity=" + siteEntity +
                ", path='" + path +
                ", code=" + code +
                '}';
    }
}
