package mad.chat_data.persist.api;

import mad.chat_data.persist.domain.models.Conversation;
import mad.chat_data.persist.domain.models.Message;
import mad.chat_data.persist.domain.models.User;
import mad.chat_data.persist.domain.services.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat") //
@CrossOrigin(origins = "http://192.168.1.179:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ChatController {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChatController.class);

    private ChatService chatService;

    public ChatController(ChatService chatService) {
         this.chatService = chatService;
    }

    @PostMapping("/messages")
    public Message saveMessage(@RequestBody Message message) {
        return chatService.saveMessage(message);
    }

    @GetMapping("/messages")
    public List<Message> getRecentMessages(@RequestParam String conversationId) {
        List<Message> messages = chatService.getRecentMessages(conversationId);
        logger.debug("Messages renvoyés au frontend pour conversation {}: {}", conversationId, messages.stream().map(m -> m.getContent() + " (" + m.getTimestamp() + ")").collect(Collectors.toList()));
        return messages;
    }
    @GetMapping("/conversations")
    public List<Conversation> getConversations() {
            return chatService.getConversations();
    }
    @PostMapping("/conversations")
    public Conversation saveConversation(@RequestBody Conversation conversation) {
        return chatService.saveConversation(conversation);
    }
    @GetMapping("/users")
    public List<User> getUsers() {
            return chatService.getUser();
    }
}
