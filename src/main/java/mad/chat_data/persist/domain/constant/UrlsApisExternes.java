package mad.chat_data.persist.domain.constant;

public class UrlsApisExternes {

    /**
     * URLs de connexion à autoriser vers l'API d'authentification et le client Angular
     */
    private static final String[] URLs = {
            "https://chat-auth-j6ww.onrender.com",
            "https://chat-client-e2lv.onrender.com"

    };

    public static String[] getUrls() {
        return  URLs;
    }
}