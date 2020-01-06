package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;

public class ChatCreationTimeout_Test {
    final int MAX_NUM_CHATS = 3;
    private Chat newChat;
    private Boolean timeoutExceptionThrown;
    
    private Chat[] createChats(ChatManager chatManager, int num) throws InterruptedException, TimeoutException {
        Chat[] chats = new Chat[num];
        for (int i = 0; i < num; i++) {
            chats[i] = chatManager.newChat("chat_" + i, 5, TimeUnit.SECONDS);
        }
        return chats;
    }
    
    @Test
    public void givenAChatManagerWithMaxNumberOfChatsCreated_WhenANewChatIsCreatedAndAnotherChatIsClosed_ThenNewChatIsCreated() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with max chats
        ChatManager chatManager = new ChatManager(MAX_NUM_CHATS);        
        Chat[] chats = createChats(chatManager, MAX_NUM_CHATS);    
       
        // When a new chat is created
        new Thread(() -> {
            try {
                newChat = chatManager.newChat("chat_new", 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }).start();
        
        // And an existing chat is deleted before timeout 
        Thread.sleep(4500);        
        for(int i = 0; i < MAX_NUM_CHATS; i++) {
            assertTrue(chatManager.getChats().contains(chats[i]));
        }        
        chatManager.closeChat(chats[0]);

        // Then new chat is created
        Thread.sleep(500);
        for(int i = 0; i < MAX_NUM_CHATS; i++) {
            if ( i == 0) {
                assertTrue(!chatManager.getChats().contains(chats[i]));                
            } else {
                assertTrue(chatManager.getChats().contains(chats[i]));
            }
        }      
        assertTrue(chatManager.getChats().contains(newChat));
    }

    @Test
    public void givenAChatManagerWithMaxNumberOfChatsCreated_WhenANewChatIsCreated_ThenTimeoutExceptionIsThrown() throws InterruptedException, TimeoutException, ExecutionException {
        // Given a chat manager with max chats
        final Integer timeout = 5000;
        ChatManager chatManager = new ChatManager(MAX_NUM_CHATS);        
        Chat[] chats = createChats(chatManager, MAX_NUM_CHATS);    

        // When a new chat is created
        timeoutExceptionThrown = false;
        new Thread(() -> {
            try {
                newChat = chatManager.newChat("chat_new", timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e) {
                System.out.println("Timeout!!!!");
                timeoutExceptionThrown = true;
            }
        }).start();
        
        //Then timeout exception is thrown after timeout
        Thread.sleep(timeout + 500);        
        assertTrue(timeoutExceptionThrown);
        
        for(int i = 0; i < MAX_NUM_CHATS; i++) {
            assertTrue(chatManager.getChats().contains(chats[i]));
        }        
        assertTrue(chatManager.getChats().size() == MAX_NUM_CHATS);
    }

}
