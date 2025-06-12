package ezcloud.ezMovie.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageDTO {
    private UUID id;
    private String content;
    private UUID senderId;
    private boolean admin;
    private String senderName;
    private String senderRole;
    private LocalDateTime createdAt;
} 