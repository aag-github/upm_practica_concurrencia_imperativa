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
        executor.execute(() -> this.user.newChat(chat));
    }

    public void chatClosed(Chat chat) {
        executor.execute(() -> this.user.chatClosed(chat));
    }

    public void newUserInChat(Chat chat, User user) {
        executor.execute(() -> this.user.newUserInChat(chat, user));
    }

    public void userExitedFromChat(Chat chat, User user) {
        executor.execute(() -> this.user.userExitedFromChat(chat, user));
    }

    public void newMessage(Chat chat, User user, String message) {
        executor.execute(() -> this.user.newMessage(chat, user, message));        
    }
    
    public boolean isSameUser(User user) {
        // Esta forma de comparar usuarios no me parece buena, pero es como estaba en la implementación original
        // cada usuario debería tener un ID único, que podría ser ser el nombre u otro identificador.
        return this.user == user;
    }
    
    private User user;
    private Executor executor;
}
