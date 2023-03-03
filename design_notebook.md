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

## March 3rd
> **Bug**
> There was a null pointer exception being returned in the tests. This was because if
> `getSecondSinceStart` was called before the timer was initialized (which wasn't done
> in the tests), the start time would be null. Added a special case to avoid this.

**Behavior of the Logs**

***Test 1: Random clock cycles***

We analyze the behavior for the following triples of cycle speeds (in number of clock ticks per (real world) second for each machine). Note that to keep track of drift, we compare the logical clock value to our system timer (which keeps track of time elapsed since starting the machine).

- 1, 3, 4

Size and gaps of the jumps in the values for the logical clocks: The machine with cycle speed 1 ended up with many logical clock value gaps, ranging from 1 and 2 to 13. The machine with cycle speed 4 did not have gaps, which is expected since it is the machine that is operating the quickest. The machine with cycle speed 3 often had no gaps (about every second it had one unit of logical time drift), but occasionally had gaps of 1 to 3 logical clock units.

Drift in the values of the local logical clocks in the different machines: The machine with cycle speed 1 almost always had a drift and accumulated drift at a rate of 2.5 units per second.  The machine with cycle speed 3 had a drift of about one unit of logical time every second. The machine with cycle speed 4 did not have any drift.

Length of the message queue: The machine with cycle speed 1 ended up having a message queue length of 8 after 1 minute. The other two machines ended up with a message queue length of 0.

- 3, 2, 4

Size of the jumps in the values for the logical clocks:

Drift in the values of the local logical clocks in the different machines: 

Gaps in the logical clock values

Length of the message queue:

- 3, 5, 6

Size of the jumps in the values for the logical clocks:

Drift in the values of the local logical clocks in the different machines: 

Gaps in the logical clock values

Length of the message queue:

- 1, 2, 6

Size of the jumps in the values for the logical clocks:

Drift in the values of the local logical clocks in the different machines: 

Gaps in the logical clock values

Length of the message queue:

- 1, 2, 2

Size of the jumps in the values for the logical clocks:

Drift in the values of the local logical clocks in the different machines: 

Gaps in the logical clock values

Length of the message queue:

- 6, 5, 6

Size of the jumps in the values for the logical clocks:

Drift in the values of the local logical clocks in the different machines: 

Gaps in the logical clock values

Length of the message queue:

***Test 2: Random clock cycles***

running it with a smaller variation in the clock cycles and a smaller probability of the event being internal. What differences do those variations make?




