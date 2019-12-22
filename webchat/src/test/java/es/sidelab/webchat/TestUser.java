package es.sidelab.webchat;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.User;

public class TestUser implements User {

	public String name;

	public TestUser(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
	
	public String getColor(){
		return "007AFF";
	}
	
	private void userLog(String s) {
	    System.out.println("User(" + name +"): " + s);
	}

	@Override
	public void newChat(Chat chat) {
	    userLog("New chat " + chat.getName());
	}

	@Override
	public void chatClosed(Chat chat) {
		userLog("Chat " + chat.getName() + " closed ");
	}

	@Override
	public void newUserInChat(Chat chat, User user) {
		userLog("New user " + user.getName() + " in chat " + chat.getName());
	}

	@Override
	public void userExitedFromChat(Chat chat, User user) {
	    userLog("User " + user.getName() + " exited from chat " + chat.getName());
	}

	@Override
	public void newMessage(Chat chat, User user, String message) {
	    userLog("New message '" + message + "' from user " + user.getName()
				+ " in chat " + chat.getName());
	}

	@Override
	public String toString() {
		return "User[" + name + "]";
	}	
}
