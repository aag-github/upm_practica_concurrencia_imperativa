package es.codeurjc.webchat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatUser implements User {
    public ChatUser(User user) {
        this.user = user;
        executor = Executors.newFixedThreadPool(1);
    }
    public String getName() {
        return user.getName();
    }
    
    public String getColor() {
        return user.getColor();
    }

    public void newChat(Chat chat) {
        executor.execute(() -> user.newChat(chat));
    }

    public void chatClosed(Chat chat) {
        executor.execute(() -> user.chatClosed(chat));
    }

    public void newUserInChat(Chat chat, User user) {
        executor.execute(() -> user.newUserInChat(chat, user));
    }

    public void userExitedFromChat(Chat chat, User user) {
        executor.execute(() -> user.userExitedFromChat(chat, user));
    }

    public void newMessage(Chat chat, User user, String message) {
        executor.execute(() -> user.newMessage(chat, user, message));        
    }
    
    private User user;
    private Executor executor;
}
