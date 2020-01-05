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

import org.javatuples.Pair;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class Mejora1_ThreadSafe_Test {
    private static final int NUM_USERS = 4;
    private static final int NUM_CHATS = 5;
   

    private String getUserName(int chatIndex, int userIndex) {
        return "chat_" + chatIndex + "::user_" + userIndex;        
    }
    
    public Pair<String, Integer> addUser(Chat chat, int chatIndex, int userIndex) throws InterruptedException, TimeoutException {
        TestUser user = new TestUser(getUserName(chatIndex, userIndex));      
        chat.addUser(user);
        return Pair.with(user.getName(), chatIndex);
    }

    private Chat[] createChats(ChatManager chatManager, int num) throws InterruptedException, TimeoutException {
        Chat[] chats = new Chat[num];
        for (int i = 0; i < num; i++) {
            chats[i] = chatManager.newChat("chat_" + i, 5, TimeUnit.SECONDS);
        }
        return chats;
    }

    @Test
    public void givenSeveralChats_WhenUsersAreAddedConcurrently_ThenUsersExistInTheChats() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with chats
        ChatManager chatManager = new ChatManager(50);

        Chat[] chats = createChats(chatManager, NUM_CHATS);
     
        List<String>[] expectedUsers = new ArrayList[NUM_CHATS];
        List<String>[] addedUsers = new ArrayList[NUM_CHATS];        
        for(int i = 0; i < NUM_CHATS; i++) {
            expectedUsers[i] = new ArrayList<String>();
            addedUsers[i] = new ArrayList<String>();
            for(int j = 0; j < NUM_USERS; j++) {
                expectedUsers[i].add(getUserName(i, j));
            }                
        }
        
        // When adding users concurrently
        ExecutorService exec = Executors.newFixedThreadPool(NUM_USERS);       
        try {
            CompletionService<Pair<String,Integer>> service = new ExecutorCompletionService(exec);
        
            for(int i = 0; i < NUM_CHATS; i++) {    
                final int chatIndex = i;
                for(int j = 0; j < NUM_USERS; j++) {
                    final int userIndex = j;
                    service.submit(()-> addUser(chats[chatIndex], chatIndex, userIndex));
                }                
            }
            
            for (int i = 0; i < NUM_CHATS * NUM_USERS; i++) {
                Future<Pair<String,Integer>> completedTask = service.take();
                addedUsers[completedTask.get().getValue1()].add(completedTask.get().getValue0());
                System.out.println("userAdd Completed: " + completedTask.get());
            }            
        } finally {
            exec.shutdown();
        }

        // Then users exist in the chats        
        for(int i = 0; i < NUM_CHATS; i++) {
            assertTrue("Size mismatch: added users: " + addedUsers[i].size() + " expected users: " + expectedUsers[i].size(),
                    addedUsers[i].size() == expectedUsers[i].size());
            for(int j = 0; j < NUM_USERS; j++) {
                assertTrue("User " + addedUsers[i].get(j) + " not found in expectedUsers",
                        expectedUsers[i].contains(addedUsers[i].get(j)));
                expectedUsers[i].remove(addedUsers[i].get(j));                
            }
        }    
    }

}
