package ezcloud.ezMovie.manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RatingDto {
    private Integer id;
    private Integer movieId;
    private UUID userId;
    private String username;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 