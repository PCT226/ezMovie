package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.dto.ChatMessage;
import ezcloud.ezMovie.dto.ConversationDTO;
import ezcloud.ezMovie.dto.MessageDTO;
import ezcloud.ezMovie.dto.UserDTO;
import ezcloud.ezMovie.entity.Conversation;
import ezcloud.ezMovie.entity.Message;
import ezcloud.ezMovie.repository.ConversationRepository;
import ezcloud.ezMovie.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        Conversation conversation = conversationRepository.findById(chatMessage.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isAdmin = chatMessage.isAdmin();

        Message message = new Message();
        message.setContent(chatMessage.getContent());
        message.setSender_id(chatMessage.getSenderId());
        message.setConversation(conversation);
        message.setAdmin(isAdmin);
        messageRepository.save(message);

        chatMessage.setAdmin(isAdmin);
        messagingTemplate.convertAndSend("/topic/chat." + conversation.getId(), chatMessage);
    }

    @PostMapping("/api/chat/conversation")
    @Transactional
    @ResponseBody
    public ConversationDTO createConversation(@RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation = conversationRepository.save(conversation);
        return convertToDTO(conversation);
    }

    @GetMapping("/api/chat/conversations/{userId}")
    @ResponseBody
    public List<ConversationDTO> getUserConversations(@PathVariable UUID userId) {
        return conversationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    @Transactional
    public List<ConversationDTO> getAllConversations() {
        return conversationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/chat/messages/{conversationId}")
    @ResponseBody
    @Transactional
    public List<MessageDTO> getConversationMessages(@PathVariable UUID conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null");
        }

        return messageRepository.findByConversationId(conversationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        UserDTO userDTO = new UserDTO();
        userDTO.setId(conversation.getUser().getId());
        userDTO.setUsername(conversation.getUser().getUsername());
        userDTO.setEmail(conversation.getUser().getEmail());
        userDTO.setPhoneNumber(conversation.getUser().getPhoneNumber());
        userDTO.setRole(conversation.getUser().getRole());
        userDTO.setCreatedAt(conversation.getUser().getCreatedAt());
        userDTO.setUpdatedAt(conversation.getUser().getUpdatedAt());
        userDTO.setVerified(conversation.getUser().isVerified());
        dto.setUser(userDTO);

        if (conversation.getMessages() != null) {
            dto.setMessages(conversation.getMessages().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setSenderId(message.getSender_id());
        dto.setAdmin(message.isAdmin());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
} 