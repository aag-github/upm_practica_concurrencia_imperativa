package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatManager {

	private Map<String, Chat> chats = new ConcurrentHashMap<>();
	private Map<String, User> users = new ConcurrentHashMap<>();
	private Semaphore smChats;
    private ExecutorService executor = Executors.newFixedThreadPool(10);

	public ChatManager(int maxChats) {
		smChats = new Semaphore(maxChats);
	}

	public void newUser(User user) {
		
		if(users.containsKey(user.getName())){
			throw new IllegalArgumentException("There is already a user with name \'"
					+ user.getName() + "\'");
		} else {
			users.put(user.getName(), user);
		}
	}

	private Boolean isThereRoomForANewChat(long timeout, TimeUnit unit) {
	    try {
            return smChats.tryAcquire(timeout, unit);
        } catch (InterruptedException e) {
            return false;
        }
	}
	
	public Chat newChat(String name, long timeout, TimeUnit unit) throws InterruptedException,
			TimeoutException {

        CompletionService<Boolean> service = new ExecutorCompletionService<Boolean>(executor);
        service.submit(()-> isThereRoomForANewChat(timeout, unit));

        Future<Boolean> isThereRoomForANewChat = service.take();
	    try {
            if(!isThereRoomForANewChat.get()) {
				throw new TimeoutException("There is no enought capacity to create a new chat");
            }
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            return null;
        }

		if(chats.containsKey(name)){
			return chats.get(name);
		} else {
			Chat newChat = new Chat(this, name);
			chats.put(name, newChat);
			
			for(User user : users.values()){
				user.newChat(newChat);
			}

			return newChat;
		}
	}

	public void closeChat(Chat chat) {
		Chat removedChat = chats.remove(chat.getName());
		if (removedChat == null) {
			throw new IllegalArgumentException("Trying to remove an unknown chat with name \'"
					+ chat.getName() + "\'");
		}

		for(User user : users.values()){
			user.chatClosed(removedChat);
		}
		smChats.release();
	}

	public Collection<Chat> getChats() {
		return Collections.unmodifiableCollection(chats.values());
	}

	public Chat getChat(String chatName) {
		return chats.get(chatName);
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public User getUser(String userName) {
		return users.get(userName);
	}

	public void close() {}
}
