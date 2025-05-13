package mad.chat_data.persist.domain.repositories;

import mad.chat_data.persist.domain.models.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
}
