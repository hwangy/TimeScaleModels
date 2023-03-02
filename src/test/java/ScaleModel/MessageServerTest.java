package ScaleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ScaleModel.grpc.MessageRequest;

/**
 * Tests (to be continued)
 */
class MessageServerTest {

    private static final String testTargetOne = "testTargetOne";
    private static final String testTargetTwo = "testTargetTwo";

    @Test 
    void testInitialTimeZero() {
        MessageCore core = new MessageCore();
        // Time starts off as 0
        Assertions.assertTrue(core.getTime() == 0);
    }

    @Test
    void testSendOneMessage() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.sendOneMessage(testTargetOne, core);
        int updatedTime = core.getTime();

        // Testing that the logical clock time was updated by 1.
        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

    @Test
    void testSendTwoMessages() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.sendTwoMessages(testTargetOne, testTargetTwo, core);
        int updatedTime = core.getTime();

        // Testing that the logical clock time was updated by 1.
        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

    @Test
    void testInternalEvent() {
        MessageCore core = new MessageCore();
        int currentTime = core.getTime();
        MessageServer.internalEvent(core);
        int updatedTime = core.getTime();

        // Testing that the logical clock time was updated by 1.
        Assertions.assertTrue(updatedTime == currentTime + 1);
    }

    @Test
    void testReceiveMessage() {
        MessageCore core = new MessageCore();
        // Large test time, for testing that the logical clock time updates to the max of the local logical clock time 
        // and request's logical clock time, plus one.
        int large_test_time = 1000;
        MessageRequest request = MessageRequest.newBuilder().setLogicalTime(large_test_time).build();

        int currentTime = core.getTime();
        MessageServer.receiveMessage(request, core);  
        int updatedTime = core.getTime();

        // Testing that the logical clock time was updated to max(local logical clock time, request's logical clock time) + 1.
        Assertions.assertTrue(updatedTime == Math.max(currentTime, request.getLogicalTime()) + 1);
    }

}