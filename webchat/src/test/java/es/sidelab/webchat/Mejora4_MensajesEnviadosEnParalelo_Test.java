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

public class Mejora4_MensajesEnviadosEnParalelo_Test {
    private final static int NUM_USERS = 4;
    private static int maxMessageDelay = 1000;
    private static boolean randomMessageDelay = false;    
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

    private List<TestUser> createUsers(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void newMessage(Chat chat, User sender, String message) {
                    try {                   
                        if(randomMessageDelay) {
                            Thread.sleep((int)(Math.random() * maxMessageDelay));                            
                        } else {
                            Thread.sleep(maxMessageDelay);
                        }
                        if (newMessages != null) {
                            newMessages.get(getName()).add(message);
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
    public void givenAChat_WhenAMessageIsSent_ThenMessagesAreSentToAllUsersConcurrently() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        maxMessageDelay = 1000;
        randomMessageDelay = false;
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);        
        List<TestUser> users = createUsers(chat);
        
        //When a message is sent
        messagesSent = new CountDownLatch(NUM_USERS);
        Long startTime = System.currentTimeMillis();
        chat.sendMessage(users.get(0), "message");

        // Then messages are sent concurrently
        messagesSent.await();
        Long actualDiff = System.currentTimeMillis() - startTime;
        Long expectedDiff = (long)(maxMessageDelay * 1.5);
        assertTrue("Time to send the message exceeded: expected: <" + expectedDiff + " actual: " + actualDiff,
            actualDiff < expectedDiff);
        
    }   

    @Test
    public void givenAChat_WhenSeveralMessagesAreSent_ThenMessagesAreReceivedInOrder() throws InterruptedException, TimeoutException, ExecutionException {
        final Integer NUM_MESSAGES = 5;
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);
        List<TestUser> users = createUsers(chat);
        createNewMessagesMap(users);
        
        //When messages are sent
        messagesSent = new CountDownLatch(NUM_USERS * NUM_MESSAGES);
        maxMessageDelay = 1000;
        randomMessageDelay = true;        
        for(Integer i = 0; i < NUM_MESSAGES; i++) {
            chat.sendMessage(users.get(0), i.toString());
        }

        // Then messages are received in order
        messagesSent.await();
        for(TestUser u : users) {
            for(Integer i = 0; i < NUM_MESSAGES; i++) {
                assertTrue("Messagge '" + i + "' to user '" + u.getName() + "' not expected '" + newMessages.get(u.getName()).get(i) + "'",
                        newMessages.get(u.getName()).get(i).equals(i.toString()));
            }
        }
        newMessages = null;
    }

}
