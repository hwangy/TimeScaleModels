package ScaleModel;

import ScaleModel.grpc.MessageRequest;
import ScaleModel.objects.Event;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MessageCore {

    private Instant startTime = null;
    private int logicalTime;
    private final Queue<MessageRequest> messageQueue;
    private final List<Event> eventList;

    /**
     * Default MessageCore constructor instantiates a new messageQueue.
     */
    public MessageCore() {
        logicalTime = 0;
        messageQueue = new LinkedList<>();
        eventList = new LinkedList<>();
    }

    public int getTime() {
        return logicalTime;
    }
    public void incrementTime() {
        logicalTime++;
    }

    public void setStartTime() {
        startTime = Instant.now();
    }

    /**
     * If the start time has been initialized, return the difference between
     * the current time and the start. Otherwise return -1l.
     * @return  The time delta
     */
    public long getSecondsSinceStart() {
        if (startTime != null) {
            return Duration.between(startTime, Instant.now()).toSeconds();
        } else {
            return -1l;
        }
    }

    public void setTimeToMax(int time) {
        logicalTime = Math.max(logicalTime, time);
    }

    public void recordEvent(Event event) {
        eventList.add(event);
    }

    public List<Event> getEventList() {
        return eventList;
    }

    public void queueMessage(MessageRequest message) {
        messageQueue.add(message);
    }

    public MessageRequest peakMessage() {
        return messageQueue.peek();
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
