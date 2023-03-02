package ScaleModel.objects;

/**
 * The Event class keeps track of events that take place, including: their description, the system time, and 
 * logical clock value. It will be used in the logging of events that takes place.
 */
public class Event {

    public enum EventType {
        SENT_MESSAGE(0),
        RECEIVED_MESSAGE(1),
        INTERNAL_EVENT(2);

        private final int identifier;

        EventType(int identifier) {
            this.identifier = identifier;
        }
    }

    private final EventType eventType;
    private final String event_description;
    private final String system_time;
    private final int logical_clock_value;

    public Event(EventType eventType, String event_description, String system_time, int logical_clock_value) {
        this.eventType = eventType;
        this.event_description = event_description;
        this.system_time = system_time;
        this.logical_clock_value = logical_clock_value;
    }

    /**
     * Fetches the event type of this event.
     * @return  The event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Fetch the event description associated with this Event
     * @return The event description
     */
    public String getEventDescription() {
        return this.event_description;
    }

    /**
     * Fetch the system time associated with this Event
     * @return The system time
     */
    public String getSystemTime() {   
        return this.system_time;
    }

    /**
     * Fetch the logical clock value associated with this Event
     * @return The logical clock value
     */
    public int getLogicalClockValue() {
        return this.logical_clock_value;
    }


    /**
     * Returns a String version of the Event (with the same information)
     */
    public String toString() {
        return "Event: " + event_description + "; System time: " + system_time +
                "; Logical clock time: " + logical_clock_value;
    }

}