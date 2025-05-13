package mad.chat_data.persist.domain.repositories;

import mad.chat_data.persist.domain.models.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByTimestampDesc(String conversationId);
}
