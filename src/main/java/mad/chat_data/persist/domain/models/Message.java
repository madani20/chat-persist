package mad.chat_data.persist.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
//@JsonIgnoreProperties(ignoreUnknown = true)
//@Setter
@Document(collection = "messages")
public class Message {

    @Id
    private  String _id;  // Correspond à _id dans MongoDB
    private  String conversationId; // Référence à _id de Conversation
    private  String userId; // Référence à _id de User
    private  String content;
    private  String timestamp;
    private  String status; // 'sent' | 'sending' | 'failed' | 'read'
    private  String type;

    public Message() {
    }

    public Message(String _id, String conversationId, String userId, String content, String timestamp, String status, String type) {
        this._id = _id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
        this.status = status;
        this.type = type;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
