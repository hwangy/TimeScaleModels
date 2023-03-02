## February 27th
We decided to use GRPC for network communications. Each "computer" will be a 
separate Java process, each of which implement a `MessageServer` wire protocol.
The protocol has the following definition,
```protobuf
service Message {
  rpc SendMessage (MessageRequest) returns (MessageReply) {}
}

message MessageRequest {
  int32 logical_time = 1;
}

message MessageReply {
  bool success = 1;
}
```

## March 1st
For each client, the following is done.
1. It asks the user for a unique identifier, and sets its port as a default port
    plus the identifier
2. Then, it sets up two channels to connect to the other hosts
3. Finally, it enters a loop, where every randomly selected times per second, it
    executes one of the tasks.

>**Decision**
>We decided to create an Event object that includes the event description (stored
> as a enum), event description, logical time, and (for MessageReceivedEvent which
> extends Event) the message queue length.

## March 2nd
>**Bug**
> We encountered an IO exception when starting the clients. This was because the
> clients were not waiting for the other servers to become available. We fixed this
> by looping and calling `channel.getState` to verify whether the connection was 
> active

>**Decision**
> We've moved some of the logic from `MessageServer` to `MessageCore`. In particular,
> the logical time, message queue, and event list are stored in `MessageCore`. Furthermore
> we've refactored the logic in the main loop to separate methods to allow easier
> unit testing.



