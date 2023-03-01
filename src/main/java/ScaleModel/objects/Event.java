package ScaleModel.objects;

/**
 * The Event class keeps track of events that take place, including: their description, the system time, and 
 * logical clock value. It will be used in the logging of events that takes place.
 */
public class Event {

    private final String event_description;
    private final String system_time;
    private final int logical_clock_value;
    private final int message_queue_length;

    public Event(String event_description, String system_time, int logical_clock_value) {
        this.event_description = event_description;
        this.system_time = system_time;
        this.logical_clock_value = logical_clock_value;
        // If don't input message queue length, it won't be needed and so set it to -1.
        this.message_queue_length = -1;
    }

    public Event(String event_description, String system_time, int message_queue_length, int logical_clock_value) {
        this.event_description = event_description;
        this.system_time = system_time;
        this.message_queue_length = message_queue_length;
        this.logical_clock_value = logical_clock_value;
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
     * Fetch the message queue length associated with this Event
     * @return The message queue length
     */
    public int getMessageQueueLength() {
        return this.message_queue_length;
    }

    /**
     * Returns a String version of the Event (with the same information)
     */
    public String toString() {
        String return_string = "";
        if(message_queue_length >= 0) {
             return_string = "Event: " + event_description + "; System time: " + system_time + "; Length of message queue: " + message_queue_length + "; Logical clock time: " + logical_clock_value;
        } else if (message_queue_length == -1) {
            return_string = "Event: " + event_description + "; System time: " + system_time + "; Logical clock time: " + logical_clock_value;
        }

        return return_string;
    }

}