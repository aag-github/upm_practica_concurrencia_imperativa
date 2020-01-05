package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.tomcat.jni.Time;
import org.javatuples.Pair;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class Mejora4_MensajesEnviadosEnParalelo_Test {
    private final static int NUM_USERS = 4;
    private static int newMessageDelay = 1000;
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
                        Thread.sleep(newMessageDelay);
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
        newMessageDelay = 1000;
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
        Long expectedDiff = (long)(newMessageDelay * 1.5);
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
        final Integer MAX_DELAY = 1000;
        messagesSent = new CountDownLatch(NUM_USERS * NUM_MESSAGES);
        for(Integer i = 0; i < NUM_MESSAGES; i++) {
            newMessageDelay = (int)(Math.random() * MAX_DELAY);
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
