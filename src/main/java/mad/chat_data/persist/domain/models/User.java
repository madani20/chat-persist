package mad.chat_data.persist.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//@Getter
//@Setter
@Document(collection = "users") //@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @Id
    private String _id; // Identifiant principal, correspond à _id dans MongoDB
    private String  username;
    private String email;
    private  String status;

    public User() {
    }

    public User(String userId, String username, String email, String status) {
        this._id = userId;
        this.username = username;
        this.email = email;
        this.status = status;
    }

    public User(String _id, String username) {
        this._id = _id;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
