package mad.chat_data.persist.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import mad.chat_data.persist.domain.models.Conversation;
import mad.chat_data.persist.domain.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {
    //============================== PROPERTIES  ===================================================================
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_MESSAGES = 50;
    private static final String ACTIVE_USERS_KEY = "active_users";

    public RedisCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    //==============================  Gestion des MESSAGES ========================================================

    // Ajoute un seul message au cache (liste Redis)

    public void addMessageToCache(String conversationId, Message message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush("conv:messages:" + conversationId, json);
            redisTemplate.opsForList().trim("conv:messages:" + conversationId, 0, MAX_MESSAGES - 1);
            redisTemplate.expire("conv:messages:" + conversationId, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l’ajout du message au cache", e);
        }
    }

    // Cache une liste entière (pour initialisation ou refresh)
    public void cacheRecentMessages(String conversationId, List<Message> messages) {
        try {
            redisTemplate.delete("conv:messages:" + conversationId); // Supprime l’ancienne liste
            for (Message msg : messages) {
                String json = objectMapper.writeValueAsString(msg);
                redisTemplate.opsForList().rightPush("conv:messages:" + conversationId, json);
            }
            redisTemplate.expire("conv:messages:" + conversationId, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du cache des messages", e);
        }
    }
    public List<Message> getCachedMessages(String conversationId) {
        List<String> jsonList = redisTemplate.opsForList().range("conv:messages:" + conversationId, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) return null;
        List<Message> cachedMessages = new ArrayList<>();
        try {
            for (String json : jsonList) {
                cachedMessages.add(objectMapper.readValue(json, Message.class));
            }
            logger.debug("Messages récupérés depuis Redis: {}", cachedMessages);
            return cachedMessages;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des messages en cache", e);
        }
    }

    //==============================  Gestion des USERS ================================================================

    public void setUserStatus(String userId, String status) {
        redisTemplate.opsForValue().set("user:status:" + userId, status, 1, TimeUnit.HOURS);
        if ("online".equals(status) || "typing".equals(status)) {
            redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, userId);
        } else {
            redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY, userId);
        }
        redisTemplate.expire(ACTIVE_USERS_KEY, 1, TimeUnit.HOURS);
        logger.info("Utilisateur {} mis à jour avec statut: {}", userId, status);
    }

    public Set<String> getActiveUsers() {
        Set<String> activeUsers = redisTemplate.opsForSet().members(ACTIVE_USERS_KEY);
        logger.info("Utilisateurs actifs dans Redis: {}", activeUsers);
        return activeUsers != null ? activeUsers : new HashSet<>();
    }
    public String getUserStatus(String userId) {
        return redisTemplate.opsForValue().get("user:status:" + userId);
    }

}
