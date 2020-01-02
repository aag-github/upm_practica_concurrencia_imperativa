package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
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

public class Mejora4_NotificationesParalelo_Test {
    private static final int NUM_USERS = 4;
    private static final int NEW_MESSAGE_DELAY = 1000;
    private CountDownLatch messagesSent;   

    private String getUserName(int chatIndex, int userIndex) {
        return "chat_" + chatIndex + "::user_" + userIndex;        
    }

    private List<TestUser> createUsers(Chat chat) {
        List<TestUser> users = new ArrayList<TestUser>();        
        for(int j = 0; j < NUM_USERS; j++) {
            TestUser user = new TestUser(getUserName(1, j)) {
                public void newMessage(Chat chat, User user, String message) {
                    try {
                        Thread.sleep(NEW_MESSAGE_DELAY);
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
    public void SendMessageTest() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with one chat and 4 users
        ChatManager chatManager = new ChatManager(50);
        Chat chat = chatManager.newChat("chat_1" , 5, TimeUnit.SECONDS);        
        List<TestUser> users = createUsers(chat);
        
        //When a message is sent
        messagesSent = new CountDownLatch(NUM_USERS);
        Long startTime = System.currentTimeMillis();
        chat.sendMessage(users.get(0), "message");

        // Then messages are processed concurrently
        messagesSent.await();
        Long actualDiff = System.currentTimeMillis() - startTime;
        Long expectedDiff = (long)(NEW_MESSAGE_DELAY * 1.5);
        assertTrue("Time to send the message exceeded: expected: <" + expectedDiff + " actual: " + actualDiff,
            actualDiff < expectedDiff);
        
    }   

}
