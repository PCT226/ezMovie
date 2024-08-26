package ezcloud.ezMovie.enities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String cast;
    private String director;
    private String title;
    private String description;
    private String genre;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
