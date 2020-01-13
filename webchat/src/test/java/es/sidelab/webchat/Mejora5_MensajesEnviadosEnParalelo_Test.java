package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class Mejora5_MensajesEnviadosEnParalelo_Test {
    private final static int NUM_USERS = 4;
    private static int maxMessageDelay = 1000;
    private CountDownLatch messagesSent;   
    private Map<String, List<String>> newMessages = null;

    private String getUserName(int chatIndex, int userIndex) {
        return "chat_" + chatIndex + "::user_" + userIndex;
    }

    private void createNewMessagesMap(List<TestUser> users) {
        newMessages = new HashMap<String, List<String>>();
        for(TestUser u : users) {
            newMessages.put(u.getName(), new ArrayList<String>());
        }
    }

    private void createNewMessagesMap(Integer num_users) {
        newMessages = new HashMap<String, List<String>>();
        for(int i = 0; i < num_users; i++) {
            newMessages.put(getUserName(1, i), new ArrayList<String>());
        }
    }

    private List<TestUser> createUsersNewChat(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void newChat(Chat chat) {
                    try {
                        Thread.sleep((int)(Math.random() * maxMessageDelay));
                        if (newMessages != null) {
                            newMessages.get(getName()).add("Create: " +  getName() + "--" + chat.getName());
                        }
                        messagesSent.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };      
            chat.addUser(user);
            users.add(user);
        }
        return users;
    }


    private List<TestUser> createUsersCloseChat(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void chatClosed(Chat chat) {
                    try {
                        Thread.sleep((int)(Math.random() * maxMessageDelay));
                        if (newMessages != null) {
                            newMessages.get(getName()).add("Delete: " +  getName() + "--" + chat.getName());
                        }
                        messagesSent.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };      
            chat.addUser(user);
            users.add(user);
        }
        return users;
    }
    
    private List<TestUser> createUsersUserExit(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void userExitedFromChat(Chat chat, User user) {
                    try {
                        Thread.sleep((int)(Math.random() * maxMessageDelay));
                        if (newMessages != null) {
                            newMessages.get(getName()).add(user.getName());
                        }
                        messagesSent.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };      
            chat.addUser(user);
            users.add(user);
        }
        return users;
    }

    private List<TestUser> createUsersNewUserInChat(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void newUserInChat(Chat chat, User user) {
                    try {
                        Thread.sleep((int)(Math.random() * maxMessageDelay));
                        if (newMessages != null) {
                            newMessages.get(getName()).add(user.getName());
                        }
                        messagesSent.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };      
            chat.addUser(user);
            users.add(user);
        }
        return users;
    }
    
    @Test
    public void givenAChatWithSeveralUsers_WhenAUserLeavesTheChat_AllOtherUsersReceiveAMessage() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);
        List<TestUser> users = createUsersUserExit(chat);
        createNewMessagesMap(users);
         
        //When user exits
        messagesSent = new CountDownLatch(NUM_USERS - 1);
        chat.removeUser(users.get(0));

        // Then all users receive the message
        messagesSent.await();
        for(TestUser u : users) {
            if (u == users.get(0)) {
                assertTrue(newMessages.get(u.getName()).size() == 0);
            } else {
                assertTrue(newMessages.get(u.getName()).size() == 1);                
                assertTrue("Messagge to user '" + u.getName() + "' not expected '" + newMessages.get(u.getName()).get(0) + "'",
                        newMessages.get(u.getName()).get(0).equals(users.get(0).getName()));
            }
        }
        newMessages = null;
    }

    @Test
    public void givenAChatWithSeveralUsers_WhenAUserEntersTheChat_AllOtherUsersReceiveAMessage() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);
        createNewMessagesMap(NUM_USERS);
        
        //When users are added to a chat
        messagesSent = new CountDownLatch((NUM_USERS * (NUM_USERS - 1))/ 2);
        createUsersNewUserInChat(chat);

        // Then all users receive the message
        messagesSent.await();
        for(int i = 0; i < NUM_USERS; i++) {
            String currentUserName = getUserName(1, i);
            int k = 0;
            for (int j = i + 1; j < NUM_USERS; j++) {
                String expectedName = getUserName(1, j);
                String actualName = newMessages.get(currentUserName).get(k);
                assertTrue("Messagge to user '" + currentUserName + "' not expected '" + actualName + "'",
                        actualName.equals(expectedName));
                k = k + 1;
            }
        }
        newMessages = null;
    }
    
    @Test
    public void givenAChatWithSeveralUsers_WhenANewChatIsCreated_AllOtherUsersReceiveAMessage() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);
        createNewMessagesMap(NUM_USERS);
        List<TestUser> users = createUsersNewChat(chat);
        for(User u : users) {
            chatManager.newUser(u);
        }        
        
        //When users are added to a chat
        messagesSent = new CountDownLatch(NUM_USERS);
        final String newChatName = "chat_to_create";
        chatManager.newChat(newChatName , 5, TimeUnit.SECONDS);        

        // Then all users receive the message
        messagesSent.await();
        for(int i = 0; i < NUM_USERS; i++) {
            String currentUserName = getUserName(1, i);

            assertTrue(newMessages.get(currentUserName).size() == 1);            
            String actualMessage = newMessages.get(currentUserName).get(0);
            String expectedMessage = "Create: " + getUserName(1, i) + "--" + newChatName;            
            assertTrue("Messagge to user '" + currentUserName + "' not expected '" + actualMessage + "'",
                    actualMessage.equals(expectedMessage));            
        }
        newMessages = null;
    }

    @Test
    public void givenAChatWithSeveralUsers_WhenANewChatIsDeleted_AllOtherUsersReceiveAMessage() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);
        createNewMessagesMap(NUM_USERS);
        List<TestUser> users = createUsersCloseChat(chat);
        for(User u : users) {
            chatManager.newUser(u);
        }        
        
        //When users are added to a chat
        messagesSent = new CountDownLatch(NUM_USERS);
        final String targetChatName = "chat_to_delete";
        chatManager.closeChat(chatManager.newChat(targetChatName , 5, TimeUnit.SECONDS));        

        // Then all users receive the message
        messagesSent.await();
        for(int i = 0; i < NUM_USERS; i++) {
            String currentUserName = getUserName(1, i);

            assertTrue(newMessages.get(currentUserName).size() == 1);            
            String actualMessage = newMessages.get(currentUserName).get(0);
            String expectedMessage = "Delete: " + getUserName(1, i) + "--" + targetChatName; 
            assertTrue("Messagge to user '" + currentUserName + "' not expected '" + actualMessage + "'",
                    actualMessage.equals(expectedMessage));            
        }
        newMessages = null;
    }
    
}
