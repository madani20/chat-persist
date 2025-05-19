package mad.chat_data.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mad.chat_data.persist.domain.models.User;
import mad.chat_data.persist.domain.repositories.UserRepository;
import mad.chat_data.persist.domain.services.ChatService;
import mad.chat_data.persist.domain.services.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = Logger.getLogger(ChatWebSocketHandler.class.getName());

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, String> sessionToUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastPongTimes = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final RedisCacheService redisCacheService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ChatWebSocketHandler(UserRepository userRepository, ChatService chatService, RedisCacheService redisCacheService) {
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.redisCacheService = redisCacheService;
        logger.info("Nouvelle instance de ChatWebSocketHandler créée.");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Init afterConnectionEstablished");
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query == null || !query.contains("Authorization=Bearer ")) {
            log.warn("Token manquant, session: {}", session.getId());
            session.close(new CloseStatus(1003, "Token d'authentification manquant"));
            return;
        }
        String token = query.split("Authorization=Bearer ")[1].trim();
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String userId = claims.getSubject();

            synchronized (sessions) {
                Iterator<WebSocketSession> iterator = sessions.iterator();
                while (iterator.hasNext()) {
                    WebSocketSession existingSession = iterator.next();
                    String existingUserId = sessionToUserId.get(existingSession.getId());
                    if (userId.equals(existingUserId) && !existingSession.getId().equals(session.getId())) {
                        log.info("Fermeture de la session existante pour userId: {}, sessionId: {}", userId, existingSession.getId());
                        existingSession.close(new CloseStatus(1000, "Nouvelle connexion détectée"));
                        iterator.remove();
                        sessionToUserId.remove(existingSession.getId());
                        lastPongTimes.remove(existingSession.getId());
                    }
                }
                session.getAttributes().put("_id", userId);
                sessions.add(session);
                sessionToUserId.put(session.getId(), userId);
                lastPongTimes.put(session.getId(), System.currentTimeMillis());
                startHeartbeat(session);
            }

            userRepository.updateStatus(userId, "online");
            redisCacheService.setUserStatus(userId, "online");
            broadcastUserStatus();
            log.info("Connexion WebSocket établie, session: {}, userId: {}, total sessions: {}", session.getId(), userId, sessions.size());
        } catch (Exception e) {
            log.warn("Connexion WebSocket avec token invalide, session: {}", session.getId());
            session.close(new CloseStatus(1003, "Token invalide: " + e.getMessage()));
        }
    }



    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId;
        synchronized (sessions) {
            userId = sessionToUserId.remove(session.getId());
            sessions.remove(session);
            lastPongTimes.remove(session.getId());
            log.info("Connexion WebSocket fermée, session: {}, raison: {}, total sessions: {}", session.getId(), status, sessions.size());
        }
        if (userId != null && !sessionToUserId.containsValue(userId)) {
            userRepository.updateStatus(userId, "offline");
            redisCacheService.setUserStatus(userId, "offline");
            broadcastUserStatus();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = sessionToUserId.get(session.getId());
        String payload = message.getPayload();
        try {
            Map<String, Object> data = mapper.readValue(payload, Map.class);
            if ("pong".equals(data.get("event"))) {
                log.debug("Pong JSON reçu de session: {}", session.getId());
                lastPongTimes.put(session.getId(), System.currentTimeMillis());
                return;
            }
            chatService.handleWebSocketMessage(userId, payload, (event, d) -> {
                try {
                    broadcast(event, d);
                    if ("message".equals(event) || "userStatus".equals(event)) {
                        broadcast("userStatus", chatService.getOnlineUsers());
                        log.info("Diffusion userStatus après événement: {}", event);
                    }
                } catch (Exception e) {
                    log.error("Erreur lors de la diffusion : ", e);
                }
            });
        } catch (Exception e) {
            log.error("Erreur parsing message: {}, payload: {}", e.getMessage(), payload);
            // Ignorer les messages non-JSON pour éviter les erreurs
        }
    }

    public void broadcast(String event, Object data) throws Exception {
        if ("userStatus".equals(event)) {
            broadcastUserStatus();
            return;
        }
        String payload = mapper.writeValueAsString(Map.of("event", event, "data", data));
        log.info("Début diffusion événement: {} à {} sessions", event, sessions.size());
        synchronized (sessions) {
            Iterator<WebSocketSession> iterator = sessions.iterator();
            while (iterator.hasNext()) {
                WebSocketSession session = iterator.next();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                    log.debug("Envoyé à session: {}", session.getId());
                } else {
                    log.warn("Session fermée: {}, suppression", session.getId());
                    iterator.remove();
                    sessionToUserId.remove(session.getId());
                    lastPongTimes.remove(session.getId());
                }
            }
        }
        log.info("Fin diffusion événement: {}", event);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Erreur transport WebSocket, session: {}", session.getId(), exception);
        String userId;
        synchronized (sessions) {
            userId = sessionToUserId.remove(session.getId());
            sessions.remove(session);
            lastPongTimes.remove(session.getId());
        }
        if (userId != null && !sessionToUserId.containsValue(userId)) {
            userRepository.updateStatus(userId, "offline");
            redisCacheService.setUserStatus(userId, "offline");
            broadcastUserStatus();
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void broadcastUserStatus() throws Exception {
        List<User> onlineUsers = chatService.getOnlineUsers();
        String payload = mapper.writeValueAsString(Map.of(
                "event", "userStatus",
                "data", onlineUsers,
                "total", onlineUsers.size()
        ));
        log.info("Début diffusion événement: userStatus à {} sessions, payload: {}", sessions.size(), payload);
        synchronized (sessions) {
            Iterator<WebSocketSession> iterator = sessions.iterator();
            while (iterator.hasNext()) {
                WebSocketSession session = iterator.next();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                    log.debug("Envoyé à session: {}", session.getId());
                } else {
                    log.warn("Session fermée: {}, suppression", session.getId());
                    iterator.remove();
                    sessionToUserId.remove(session.getId());
                    lastPongTimes.remove(session.getId());
                }
            }
        }
        log.info("Fin diffusion événement: userStatus");
    }

    private void startHeartbeat(WebSocketSession session) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    long lastPong = lastPongTimes.getOrDefault(session.getId(), 0L);
                    if (System.currentTimeMillis() - lastPong > 90000) { // Changé de 60000 à 90000
                        log.warn("Aucun pong reçu pour session: {}, fermeture", session.getId());
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Aucun pong reçu"));
                        return;
                    }
                    session.sendMessage(new TextMessage("{\"event\":\"ping\"}"));
                    log.debug("Ping envoyé à session: {}", session.getId());
                }
            } catch (Exception e) {
                log.error("Erreur lors de l’envoi du ping à session {}: ", session.getId(), e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
}
