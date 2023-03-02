package ScaleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ScaleModel.grpc.MessageRequest;

/**
 * Unit tests for the MessageServer class. These unit tests encompass the various functionalities of 
 * MessageServer, including sending one or multiple messages, receiving a message, and performing an internal event.
 * These tests mostly concern how the logical clock gets updated.
 */
class MessageServerTest {

    private static final String testTargetOne = "testTargetOne";
    private static final String testTargetTwo = "testTargetTwo";

    /**
     * Test that core's initial logical clock time is zero.
     */
    @Test 
    void testInitialTimeZero() {
        MessageCore core = new MessageCore();
        // Time starts off as 0
        Assertions.assertTrue(core.getTime() == 0);
    }

    /**
     * Testing that the logical clock time was updated by 1 when one message was sent
     */
    @Test
    void testSendOneMessage() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.sendOneMessage(testTargetOne, core);
        int updatedTime = core.getTime();

        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

     /**
     * Testing that the logical clock time was updated by 1 when two messages were sent
     */
    @Test
    void testSendTwoMessages() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.sendTwoMessages(testTargetOne, testTargetTwo, core);
        int updatedTime = core.getTime();

        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

     /**
     * Testing that the logical clock time was updated by 1 when an internal event took place
     */
    @Test
    void testInternalEvent() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.internalEvent(core);
        int updatedTime = core.getTime();

        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

     /**
     * Testing that the logical clock time updates to the max of the local logical clock time 
     * and request's logical clock time, plus one.
     */
    @Test
    void testReceiveMessage() {
        MessageCore core = new MessageCore();
        // Large test time
        int large_test_time = 1000;
        MessageRequest request = MessageRequest.newBuilder().setLogicalTime(large_test_time).build();

        int currentTime = core.getTime();
        MessageServer.receiveMessage(request, core);  
        int updatedTime = core.getTime();

        // Testing that the logical clock time was updated to max(local logical clock time, request's logical clock time) + 1.
        Assertions.assertTrue(updatedTime == Math.max(currentTime, request.getLogicalTime()) + 1);
    }

}