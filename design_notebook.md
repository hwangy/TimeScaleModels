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

Size and gaps of the jumps in the values for the logical clocks: The machine with cycle speed 2 had jumps of about 2 to 3 around every second. It had frequent jumps but never large jumps. The machine with cycle speed 4 never had jumps. The machine with cycle speed 3 mostly did not have jumps, but occasionally (every 7 or so ticks) had a jump of around 2.

Drift in the values of the local logical clocks in the different machines: The machine with cycle speed 2 accumulated a drift of around 2 ticks every second. The machine with cycle speed 2 accumulated a drift of a little less than 1 tick every second. The machine with cycle speed 4 did not accumulate drift.

Length of the message queue: The machines with 3 and 4 never had a message queue longer than length, while the machine with cycle speed 2 had message queues that jumped up to a size of 3 sometimes, though it was still at 0 or 1 most of the time. This is to be expected since all of the cycle speeds are pretty similar, but the cycle speed of 2 is the slowest so it will accumulate longer message queues.

- 3, 5, 6

This is very similar to the 3, 2, 4 experiment. Although we may have expected it to be similar to the 1, 3, 4 experiment, because the increase in frequency has diminishing returns it ends up being closer to the 3, 2, 4 case. The machine wth cycle speed 3 had a drift of roughly 3 every second, which is more than the drift of the machine with cycle speed 2 in the previous experiment. Otherwise the behavior was very similar. The machine with cycle speed 3 has a queue length of 2 fairly frequently. 

- 1, 2, 2

This experiment reinforced our hypothesis that for lower cycle speeds, gaps in the cycle speed have a larger influence.

Size and gaps of the jumps in the values for the logical clocks: The machine with cycle speed 1 had a very large gap jump after removing all of the messages in the message queue of size 6. The gap was size 12. The other two machines never really had jumps.

Drift in the values of the local logical clocks in the different machines: When the machine with cycle speed 1 was eliminating the queue of size 6, it incurred a drift of 12 (equivalent to gap). The average drift of the cycle speed 1 machine was 1 unit per second. For the other two machines there was no drift.

Length of the message queue: At some point near the beginning, the message queue got up to size 6. It was lower as the experiment progressed. For the machines with cycle speed 2, rarely was the message queue nonempty.

- 1, 2, 6

Size and gaps of the jumps in the values for the logical clocks: The machine with cycle time 1 had gaps of 6 to 8 seconds often. It would have a gap larger than 1 pretty much every time tick. The machine with cycle time 6 did not have gaps, and the machine with cycle time 2 had gaps of up to 6, but they were on average closer to 2.

Drift in the values of the local logical clocks in the different machines: The drifts of machines with cycle time 1 and 2 were similar - roughly 4 units of drift per second. This makes sense because, if we were working with a stack there would be less time lag (because the times we update the logical clock to are more recent), but because we are working with a queue the machine incurs more of a drift and time lag.

Length of the message queue: The machine with cycle time 1 ended up with a message queue of size 16. Overall through the experiment it kept accumulating a longer and longer message queue. The machine with cycle time 2 only had a short message queue (around size 0 to 2 always). This may be because the machine with cycle time 1 has such a large message queue, so never really sent messages, and so the machine with cycle time 2 only had to deal with messages from the machine with cycle time 6.

- 6, 5, 6

Size and gaps of the jumps in the values for the logical clocks: The machines with cycle time 6 never have gaps. The machine with cycle time 5 has an infrequent small gap (around a gap of 2 every 7 ticks).

Drift in the values of the local logical clocks in the different machines: The machine with cycle time 5 had 1 unit of drift every second. The other machines didn't have drift.

Length of the message queue: The message queue was at most 1 for each machine.

**Summary**: Generally, drift was just a function of which machines had lower frequencies. Similarly with the gaps, significant gaps were usually incurred when a machine was running down a large message queue. This was evident in the 1, 2, 2 example. Changes in lower values of frequency were more important than changes in higher values of frequency. For example observe 1, 2, 2 versus 6, 5, 6 experiments: The 1 versus 2 had a large difference as compared to the 5 versus 6. Usually large message queues were harder to come back from. For the 1, 2, 6 experiement, the machine with cycle time 1 had a very large message queue and lagged behind / incurred a large drift due to this.

***Test 2: Other scenarios***

***Smaller probability of the event being internal***

We ran this experiment with much less chance of having an internal event (now probability 1/2 of internal event, down from probability 0.3 of non-internal event before)

- 1, 2, 6

We tried 1, 2, 6 in this scenario. Intuitively we expected the performance to be worse for the cycle speed 1 machine, because lowering the probability of an internal event doesn't improve the probability of receiving a message (because will always receive a message when it can). However, the cycle speed 6 machine now sends messages at a much higher cadence in expectation. Thus the primary change we observed was that the message queue went up to 50+ in length, and both the 1 and 2 cycle speed machines only received messages, with small exceptions in the 2 cycle speed machine and no exceptions in the 1 cycle speed machine.

- 6, 5, 6

The behavior in the 6, 5, 6 is mostly similar to the 6, 5, 6 case we tried before. The 5 cycle speed length occasionally got a message queue length of 2, which was infrequent but worse than the setup with higher internal event probability.

***Smaller variation in the clock cycles***

Smaller variation in the clock cycles corresponds to higher frequency.

- 5, 6, 10

The machine with cycle speed 5 had a message queue that got up to 3, but not ever for long and never moved beyond length 3 queues. You can recall that in the 1, 2, 6 case, we saw the cycle time 1 machine had message queue length of up to 50. This further reinforces our claim that shifts in clock speed matter more at lower frequencies than higher ones.

 The 5, 6, 10 experiement here was rather analogous to the 6, 5, 6 experiment with the lower probability of an internal event.
