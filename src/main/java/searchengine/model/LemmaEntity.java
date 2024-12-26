package searchengine.model;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@Entity
@Getter
@Setter
@Table(name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    private SiteEntity siteEntity;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255) NOT NULL, Index(lemma(255))")
    private String lemma;

    @Column(name= "frequency", columnDefinition = "INT NOT NULL")
    private Integer frequency;


    @Override
    public String toString() {
        return "LemmaEntity{" +
                "id=" + id +
                ", siteEntity=" + siteEntity +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
