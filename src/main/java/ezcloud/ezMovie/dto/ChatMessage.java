package ezcloud.ezMovie.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessage {
    private UUID conversationId;
    private String content;
    private UUID senderId;
    private boolean isAdmin;
    private String senderName;
    private String senderRole;
    private String createdAt;
} 