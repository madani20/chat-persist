package mad.chat_data.persist.domain.repositories;

import mad.chat_data.persist.domain.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    User findByUsername (String username);
    List<User> findByStatus(String status);
    Optional<User> findBy_id(String _id);


    default void updateStatus(String _id, String status) {
        User user = findById(_id).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(status);
        save(user);
    }

    @Query("{ '_id': { $in: ?0 } }")
    List<User> findByIdIn(ArrayList<String> strings);
}
