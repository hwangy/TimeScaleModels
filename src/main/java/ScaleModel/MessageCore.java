package ScaleModel;

import ScaleModel.grpc.MessageRequest;

import java.util.LinkedList;
import java.util.Queue;

public class MessageCore {
    private final Queue<MessageRequest> messageQueue;

    public MessageCore() {
        messageQueue = new LinkedList<>();
    }

    public void queueMessage(MessageRequest message) {
        messageQueue.add(message);
    }

    public MessageRequest popMessage() {
        return messageQueue.poll();
    }
}
