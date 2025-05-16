package mad.chat_data.persist.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import mad.chat_data.persist.domain.models.Conversation;
import mad.chat_data.persist.domain.models.Message;
import mad.chat_data.persist.domain.models.User;
import mad.chat_data.persist.domain.repositories.ConversationRepository;
import mad.chat_data.persist.domain.repositories.MessageRepository;
import mad.chat_data.persist.domain.repositories.UserRepository;
import mad.chat_data.persist.tools.JwtTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;

@Service
public class ChatService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);


    //=======================================  SERVICES  ==============================================================

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatService( UserRepository userRepository, MessageRepository messageRepository, ConversationRepository conversationRepository, RedisCacheService redisCacheService) {
       // this.webSocketHandler = webSocketHandler;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.redisCacheService = redisCacheService;

    }

    //===================================== [ GESTION DES SESSIONS ] ===================================================


    public void handleWebSocketMessage(String userId, String payload, BiConsumer<String, Object> broadcastCallback) throws Exception {
        logger.info("Init handleWebSocketMessage");
        try {
            Map<String, Object> eventPayloadData = mapper.readValue(payload, Map.class);
            String event = (String) eventPayloadData.get("event");
            if ("message".equals(event)) {
                Message message = mapper.convertValue(eventPayloadData.get("data"), Message.class);
                message.setUserId(userId);
                Message persistedMessage = messageRepository.save(message);
                logger.debug("Message avant diffusion: '{}'", persistedMessage.getContent());
                broadcastCallback.accept("message", persistedMessage);
                broadcastCallback.accept("userStatus", getOnlineUsers()); // Ajout pour synchroniser les utilisateurs
            } else if ("userStatus".equals(event)) {
                Map<String, String> data = (Map<String, String>) eventPayloadData.get("data");
                String status = data.get("status");
                userRepository.updateStatus(userId, status);
                redisCacheService.setUserStatus(userId, status);
                broadcastCallback.accept("userStatus", getOnlineUsers());
            }
        } catch (Exception e) {
            logger.error("Erreur traitement message: {}, payload: {}", e.getMessage(), payload);
            throw new RuntimeException(e);
        }
        logger.info("Fin handleWebSocketMessage");
    }

    void addUserToConversation(String conversationId, String userId) {

    }


    //=======================================  METHODS  ================================================================

    //============================= GESTION DES MESSAGES ===========================================================
    //Ajout d'un message

    public Message saveMessage(Message message) {
        if (message.getUserId() == null || message.getConversationId() == null) {
            throw new IllegalArgumentException("userId et conversationId requis");
        }
        logger.info("Sauvegarde du message avec userId: {}, content: {}", message.getUserId(), message.getContent());
        Message saved = messageRepository.save(message);
        logger.info("Message sauvegardé: {}", saved);
        redisCacheService.addMessageToCache(message.getConversationId(), saved);
        return saved;
    }

public List<Message> getRecentMessages(String conversationId) {
    List<Message> cached = redisCacheService.getCachedMessages(conversationId);
    List<Message> sortedMessages = messageRepository.findByConversationIdOrderByTimestampDesc(conversationId);

    // Fusionne les messages du cache et de MongoDB, en éliminant les doublons par ID
    Set<String> messageIds = new HashSet<>();
    List<Message> allMessages = new ArrayList<>();
    if (cached != null) {
        cached.forEach(msg -> {
            if (messageIds.add(msg.get_id())) allMessages.add(msg);
        });
    }
    sortedMessages.forEach(msg -> {
        if (messageIds.add(msg.get_id())) allMessages.add(msg);
    });
    // Trie par timestamp descendant
    allMessages.sort(Comparator.comparing(Message::getTimestamp).reversed());
    if (!allMessages.isEmpty()) {
        redisCacheService.cacheRecentMessages(conversationId, allMessages);
    }
    return allMessages;
}
    //============================= GESTION DES CONVERSATIONS  ===================================================

    public Conversation saveConversation(Conversation conversation) {
        if (conversation == null) {
            throw new RuntimeException();
        }
        try {
            Conversation savedConversation = conversationRepository.save(conversation);
            logger.debug("Conversation sauvegardée avec succès");
            return savedConversation;
        } catch (Exception e) {
            logger.error("Erreur de la sauvegarde de la conversation");
            throw new RuntimeException(e);
        }
    }

    public List<Conversation> getConversations() {
        return conversationRepository.findAll();
    }


    //============================= GESTION DES USERS  ===========================================================

    public List<User> getUser() {
        return userRepository.findAll();
    }

    /**
     * Synchronise l'utilisateur créé en bdd MySQL avec l'utilisateur en bdd noSQL MongoDB
     *
     * @param userData
     */
    public void synchronizeUserWithPersist(Map<String, String> userData) {
        String userId = userData.get("userId"); // Fourni par API authentification
        String username = userData.get("username");
        String email = userData.get("email");
        String status = userData.get("status");

        User user = userRepository.findBy_id(userId)
                .orElse((new User(userId, username)));
        user.set_id(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(status);
        userRepository.save(user);

        logger.info("User synchronized in database: " + user.getUsername());
    }

    public List<User> getOnlineUsers() {
        Set<String> activeUserIds = redisCacheService.getActiveUsers();
        logger.info("Active user IDs from Redis: {}", activeUserIds);
        if (activeUserIds == null || activeUserIds.isEmpty()) {
            logger.debug("Aucun utilisateur actif dans Redis");
            return List.of();
        }
        List<User> onlineUsers = userRepository.findByIdIn(new ArrayList<>(activeUserIds));
        logger.debug("Utilisateurs en ligne récupérés: {}", onlineUsers);
        return onlineUsers;
    }

    private List<User> getUsersWithOnlineStatus(ArrayList<User> users) {
        if(users == null || users.isEmpty()) {
            logger.debug("Aucun utilisateurs fourni");
            return List.of();
        }
        return users
                    .stream()
                    .filter(user -> user.getStatus().equals("online"))
                    .toList();
        }
    }


