package ezcloud.ezMovie.enities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cinemas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cinema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String location;
    private String city;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted = false;
    @OneToMany(mappedBy = "cinema")
    private Set<Screen> screens;

}
