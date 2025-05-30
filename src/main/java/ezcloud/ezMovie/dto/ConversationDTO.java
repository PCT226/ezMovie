package ezcloud.ezMovie.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ConversationDTO {
    private UUID id;
    private UserDTO user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageDTO> messages;
} 