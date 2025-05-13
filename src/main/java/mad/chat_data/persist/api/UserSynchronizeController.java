package mad.chat_data.persist.api;

import mad.chat_data.persist.domain.models.User;
import mad.chat_data.persist.domain.services.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserSynchronizeController {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(UserSynchronizeController.class);

    private ChatService chatService;


    public UserSynchronizeController(ChatService chatService) {
        this.chatService = chatService;
    }


    @PostMapping("/synchronize")
    public ResponseEntity<Void> synchronizeUser(@RequestBody Map<String, String> data ) {
        chatService.synchronizeUserWithPersist(data);
        return ResponseEntity.ok().build();
    }
    //Ajouter méthode delete ici , et voir si cascade ou pas?
}
