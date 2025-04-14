package ezcloud.ezMovie.manage.model.enities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "screens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;
    @Column(nullable = false)
    private Integer screenNumber;
    @Column(nullable = false)
    private Integer capacity;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted = false;


}
