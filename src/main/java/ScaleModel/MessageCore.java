package ScaleModel;

import ScaleModel.grpc.MessageRequest;
import ScaleModel.objects.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MessageCore {
    private final Queue<MessageRequest> messageQueue;
    // TODO: this doesn't work
    private final List<Event> eventList;

    /**
     * Default MessageCore constructor instantiates a new messageQueue.
     */
    public MessageCore() {
        messageQueue = new LinkedList<>();
        eventList = new LinkedList<>();
    }

    public void queueMessage(MessageRequest message) {
        messageQueue.add(message);
    }

    public MessageRequest popMessage() {
        return messageQueue.poll();
    }

    public Boolean messageQueueIsEmpty() {
        return messageQueue.isEmpty();
    }

    public int getMessageQueueLength() {
        return messageQueue.size();
    }
}
