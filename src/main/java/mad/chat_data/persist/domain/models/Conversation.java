package mad.chat_data.persist.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

//@Getter @Setter @NoArgsConstructor @AllArgsConstructor
//@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String _id; // Correspond à _id dans MongoDB
    private  String  name;
    private  List<String> participants;
    private  String lastMessageId;
    private  String createdAt;
    private  String updatedAt;



    public Conversation() {
    }

    public Conversation(String _id, String name, List<String> participants, String lastMessageId, String createdAt, String updatedAt) {
        this._id = _id;
        this.name = name;
        this.participants = participants;
        this.lastMessageId = lastMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
