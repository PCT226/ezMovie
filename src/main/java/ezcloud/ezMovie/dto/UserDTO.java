package ezcloud.ezMovie.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean verified;
} 