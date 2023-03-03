package ScaleModel.objects;

public class MessageReceivedEvent extends Event {

    private final int message_queue_length;

    public MessageReceivedEvent(String event_description, long system_time, int message_queue_length, int logical_clock_value) {
        super(EventType.RECEIVED_MESSAGE, event_description, system_time, logical_clock_value);
        this.message_queue_length = message_queue_length;
    }

    /**
     * Fetch the message queue length associated with this Event
     * @return The message queue length
     */
    public int getMessageQueueLength() {
        return this.message_queue_length;
    }

        @Override
    public String toString() {
        return "Event: " + getEventDescription() + "; System time: " + getSystemTime() +
                "; Length of message queue: " + message_queue_length + "; Logical clock time: "
                + getLogicalClockValue();
    }
}
