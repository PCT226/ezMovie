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
import ezcloud.ezMovie.admin.repository.AdminRepository;
import ezcloud.ezMovie.admin.model.entities.Admin;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
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

        // Lấy lại thông tin sender từ DB để enrich message trả về
        String senderName = "Unknown";
        String senderRole = "USER";
        User sender = userRepository.findById(chatMessage.getSenderId()).orElse(null);
        if (sender != null) {
            senderName = sender.getUsername() != null ? sender.getUsername() : sender.getEmail();
            senderRole = sender.getRole();
        } else {
            Admin admin = adminRepository.findById(chatMessage.getSenderId()).orElse(null);
            if (admin != null) {
                senderName = admin.getEmail();
                senderRole = admin.getRole();
            }
        }
        chatMessage.setSenderName(senderName);
        chatMessage.setSenderRole(senderRole);
        chatMessage.setAdmin(isAdmin);

        messagingTemplate.convertAndSend("/topic/chat." + conversation.getId(), chatMessage);
    }

    @PostMapping("/api/chat/conversation")
    @Transactional
    @ResponseBody
    public ConversationDTO createConversation(@RequestParam UUID userId) {
        try {
            System.out.println("=== BẮT ĐẦU TẠO CONVERSATION CHO USER: " + userId + " ===");
            
            // Sử dụng method helper thread-safe
            ConversationDTO result = getOrCreateConversationForUser(userId);
            
            System.out.println("=== HOÀN THÀNH TẠO CONVERSATION ===");
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo conversation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create conversation", e);
        }
    }

    @PostMapping("/api/chat/conversation/simple")
    @Transactional
    @ResponseBody
    public ConversationDTO createSimpleConversation(@RequestParam UUID userId) {
        try {
            System.out.println("=== BẮT ĐẦU TẠO CONVERSATION ĐƠN GIẢN CHO USER: " + userId + " ===");
            
            // Sử dụng method helper thread-safe
            ConversationDTO result = getOrCreateConversationForUser(userId);
            
            System.out.println("=== HOÀN THÀNH TẠO CONVERSATION ĐƠN GIẢN ===");
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo conversation đơn giản: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create simple conversation", e);
        }
    }

    @GetMapping("/api/chat/conversations/{userId}")
    @ResponseBody
    @Transactional
    public List<ConversationDTO> getUserConversations(@PathVariable UUID userId) {
        try {
            System.out.println("=== LẤY CONVERSATIONS CHO USER: " + userId + " ===");
            
            // Kiểm tra user có tồn tại không
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                System.out.println("User không tồn tại: " + userId);
                return new ArrayList<>();
            }
            System.out.println("Tìm thấy user: " + user.getEmail());
            
            List<Conversation> conversations = conversationRepository.findByUserId(userId);
            System.out.println("Số conversation tìm thấy: " + conversations.size());
            
            for (int i = 0; i < conversations.size(); i++) {
                Conversation conv = conversations.get(i);
                System.out.println("Conversation " + (i+1) + ": ID=" + conv.getId() + ", Created=" + conv.getCreatedAt());
            }
            
            List<ConversationDTO> result = conversations.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            System.out.println("Trả về " + result.size() + " conversation DTOs");
            System.out.println("=== HOÀN THÀNH LẤY CONVERSATIONS ===");
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy user conversations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get user conversations", e);
        }
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

    @GetMapping("/api/chat/test")
    @ResponseBody
    public String test() {
        return "Chat API is working!";
    }

    @DeleteMapping("/api/chat/conversations/{userId}")
    @ResponseBody
    @Transactional
    public String deleteAllUserConversations(@PathVariable UUID userId) {
        try {
            System.out.println("=== XÓA TẤT CẢ CONVERSATIONS CỦA USER: " + userId + " ===");
            
            List<Conversation> conversations = conversationRepository.findByUserId(userId);
            System.out.println("Số conversation sẽ xóa: " + conversations.size());
            
            for (Conversation conv : conversations) {
                System.out.println("Xóa conversation: " + conv.getId());
                conversationRepository.delete(conv);
            }
            
            System.out.println("Đã xóa " + conversations.size() + " conversations");
            System.out.println("=== HOÀN THÀNH XÓA CONVERSATIONS ===");
            return "Đã xóa " + conversations.size() + " conversations";
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa conversations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete conversations", e);
        }
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        try {
            System.out.println("Converting conversation to DTO: " + conversation.getId());
            
            ConversationDTO dto = new ConversationDTO();
            dto.setId(conversation.getId());
            dto.setCreatedAt(conversation.getCreatedAt());
            dto.setUpdatedAt(conversation.getUpdatedAt());
            
            try {
                if (conversation.getUser() != null) {
                    System.out.println("User found for conversation: " + conversation.getUser().getId());
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
                } else {
                    System.err.println("User is null for conversation: " + conversation.getId());
                    // Tạo userDTO với thông tin mặc định
                    UserDTO userDTO = new UserDTO();
                    userDTO.setId(null);
                    userDTO.setUsername("Unknown User");
                    userDTO.setEmail("unknown@example.com");
                    userDTO.setRole("USER");
                    dto.setUser(userDTO);
                }
            } catch (Exception userError) {
                System.err.println("Error processing user for conversation " + conversation.getId() + ": " + userError.getMessage());
                // Tạo userDTO với thông tin mặc định
                UserDTO userDTO = new UserDTO();
                userDTO.setId(null);
                userDTO.setUsername("Unknown User");
                userDTO.setEmail("unknown@example.com");
                userDTO.setRole("USER");
                dto.setUser(userDTO);
            }

            try {
                if (conversation.getMessages() != null) {
                    dto.setMessages(conversation.getMessages().stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList()));
                } else {
                    dto.setMessages(new ArrayList<>());
                }
            } catch (Exception messagesError) {
                System.err.println("Error processing messages for conversation " + conversation.getId() + ": " + messagesError.getMessage());
                dto.setMessages(new ArrayList<>());
            }
            
            System.out.println("Successfully converted conversation to DTO: " + conversation.getId());
            return dto;
        } catch (Exception e) {
            System.err.println("Error converting conversation to DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to convert conversation to DTO", e);
        }
    }

    private MessageDTO convertToDTO(Message message) {
        try {
            MessageDTO dto = new MessageDTO();
            dto.setId(message.getId());
            dto.setContent(message.getContent());
            dto.setSenderId(message.getSender_id());
            dto.setAdmin(message.isAdmin());
            dto.setCreatedAt(message.getCreatedAt());
            // Lấy thông tin sender từ user hoặc admin/staff
            try {
                User sender = userRepository.findById(message.getSender_id()).orElse(null);
                if (sender != null) {
                    dto.setSenderName(sender.getUsername() != null ? sender.getUsername() : sender.getEmail());
                    dto.setSenderRole(sender.getRole());
                } else {
                    Admin admin = adminRepository.findById(message.getSender_id()).orElse(null);
                    if (admin != null) {
                        dto.setSenderName(admin.getEmail());
                        dto.setSenderRole(admin.getRole());
                    } else {
                        dto.setSenderName("Unknown");
                        dto.setSenderRole("USER");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting sender info for message " + message.getId() + ": " + e.getMessage());
                dto.setSenderName("Unknown");
                dto.setSenderRole("USER");
            }
            return dto;
        } catch (Exception e) {
            System.err.println("Error converting message to DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to convert message to DTO", e);
        }
    }

    // Method helper để kiểm tra conversation hiện có một cách thread-safe
    private synchronized ConversationDTO getOrCreateConversationForUser(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Kiểm tra conversation hiện có
            List<Conversation> existingConversations = conversationRepository.findByUserId(userId);
            if (!existingConversations.isEmpty()) {
                System.out.println("Tìm thấy conversation hiện có cho user: " + userId);
                return convertToDTO(existingConversations.get(0));
            }

            // Tạo conversation mới
            System.out.println("Tạo conversation mới cho user: " + userId);
            Conversation conversation = new Conversation();
            conversation.setUser(user);
            conversation = conversationRepository.save(conversation);
            System.out.println("Đã tạo conversation với ID: " + conversation.getId());
            
            return convertToDTO(conversation);
        } catch (Exception e) {
            System.err.println("Lỗi trong getOrCreateConversationForUser: " + e.getMessage());
            throw e;
        }
    }
} 