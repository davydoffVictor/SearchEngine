package searchengine.model;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@Entity
@Getter
@Setter
@Table(name = "index_table")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", columnDefinition = "INT NOT NULL")
    private PageEntity pageEntity;

    @ManyToOne
    @JoinColumn(name = "lemma_id", columnDefinition = "INT NOT NULL")
    private LemmaEntity lemmaEntity;

    @Column(name = "rank_field", nullable = false)
    private Float rank;
}
