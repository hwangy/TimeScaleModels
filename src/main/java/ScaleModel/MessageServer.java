package ScaleModel;

import ScaleModel.grpc.MessageGrpc;
import ScaleModel.grpc.MessageReply;
import ScaleModel.grpc.MessageRequest;
import ScaleModel.objects.MessageReceivedEvent;
import ScaleModel.util.Constants;
import ScaleModel.util.GrpcUtil;
import ScaleModel.util.Logging;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import ScaleModel.objects.Event;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MessageServer {
    private final MessageGrpc.MessageBlockingStub receiverOne;
    private final MessageGrpc.MessageBlockingStub receiverTwo;

    private static int idToPort(int id) {
        return Constants.MESSAGE_PORT + id;
    }

    public MessageServer(Channel first, Channel second) {
        // Initialize stub which makes API calls.
        receiverOne = MessageGrpc.newBlockingStub(first);
        receiverTwo = MessageGrpc.newBlockingStub(second);
    }

    /**
     * This constructor should only be called for testing.
     */
    public MessageServer() {
        // Initialize stub which makes API calls.
        receiverOne = null;
        receiverTwo = null;
    }

    public void sendTimeToFirst(int logicalTime) {
        MessageReply reply = receiverOne.sendMessage(MessageRequest.newBuilder().setLogicalTime(logicalTime).build());
        if (!reply.getSuccess()) {
            Logging.logService("Send message to first client failed.");
        }
    }

    public void sendTimeToSecond(int logicalTime) {
        MessageReply reply = receiverTwo.sendMessage(MessageRequest.newBuilder().setLogicalTime(logicalTime).build());
        if (!reply.getSuccess()) {
            Logging.logService("Send message to second client failed.");
        }
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
    }

    /**
     * Starts a service which allows the client to receive messages from the server.
     * @param port          The port on which to start the receiver
     * @throws IOException  Thrown on network exception.
     */
    private static Server startMessageReceiver(int port, MessageCore core) throws IOException {
        Server server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new MessageReceiverImpl(core))
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                interrupt();
                System.err.println("*** server shut down");
            }
        });
        return server;
    }

    public static void receiveMessage(MessageRequest message, MessageCore core) {
        int request_logical_time = message.getLogicalTime();
        int message_queue_length = core.getMessageQueueLength() - 1;

        core.setTimeToMax(request_logical_time);
        core.incrementTime();

        core.recordEvent(new MessageReceivedEvent(
                "Received a message.", core.getSecondsSinceStart(), message_queue_length, core.getTime()));
    }

    private static void prettyPrintStatus(int frequency, MessageCore core) {
        clearScreen();
        // Print status
        System.out.println("Polling " + frequency + " times per second.");
        // Create string to represent message queue length
        String messagesX = "X".repeat(core.getMessageQueueLength());

        // Create string for last 3 events
        String eventString = "Message Queue: (" + core.getMessageQueueLength() + ")\t" + messagesX + "\n";
        List<Event> eventList = core.getEventList();
        int lastIndex = Math.min(10, eventList.size());
        List<Event> subList = eventList.subList(eventList.size() - lastIndex, eventList.size() - 1);
        for (Event event : subList) {
            String paddedTime = String.format("%3d", event.getLogicalClockValue());
            String paddedSystemTime = String.format("%3d", event.getSystemTime());
            String paddedEventType = String.format("%20s", event.getEventType());
            long drift = event.getLogicalClockValue() - frequency*event.getSystemTime();
            String paddedDrift = String.format("%3d", drift);
            eventString += "Time: " + paddedTime + " (" + paddedSystemTime + "s, drift: " + paddedDrift + ")" +
                    "\t" + paddedEventType +
                    "\t" + event.getEventDescription() + "\n";
        }
        System.out.print(eventString);
    }

    public static void sendOneMessage(String target, MessageCore core) {
        core.incrementTime();
        core.recordEvent(new Event(
                Event.EventType.SENT_MESSAGE,
                "Sent message to " + target, core.getSecondsSinceStart(), core.getTime()));
    }

    public static void sendTwoMessages(String targetOne, String targetTwo, MessageCore core){
        core.incrementTime();
        core.recordEvent(new Event(
                Event.EventType.SENT_MESSAGE,
                "Sent message to " + targetOne + " and " + targetTwo,
                core.getSecondsSinceStart(), core.getTime()));
    }

    public static void internalEvent(MessageCore core) {
        core.incrementTime();
        core.recordEvent(new Event(
                Event.EventType.SENT_MESSAGE, "Internal event.", core.getSecondsSinceStart(), core.getTime()));
    }

    public static void main(String[] args) throws Exception {
        MessageCore core = new MessageCore();

        Set<Integer> offsets = new HashSet<>(Arrays.asList(0,1,2));
        int offset;
        while (true) {
            System.out.println("Enter a unique client identifier (0-2).");
            Scanner inputReader = new Scanner(System.in);
            String strOffset = inputReader.nextLine();
            try {
                offset = Integer.parseInt(strOffset);
                if (!offsets.contains(offset)) {
                    throw new NumberFormatException();
                } else {
                    offsets.remove(offset);
                    break;
                }
            } catch (NumberFormatException ex) {
                Logging.logService("Invalid client identifer.");
            }
        }

        // Decide clock frequency
        int frequency = ThreadLocalRandom.current().nextInt(1, 7);
        while (true) {
            System.out.println("Enter a poll rate (1-6) or random if unspecified.");
            Scanner inputReader = new Scanner(System.in);
            String strOffset = inputReader.nextLine();
            try {
                if (strOffset.isEmpty()) {
                    break;
                }
                int tmpFrequency = Integer.parseInt(strOffset);
                if (tmpFrequency > 6 || tmpFrequency < 1) {
                    throw new NumberFormatException();
                } else {
                    frequency = tmpFrequency;
                    break;
                }
            } catch (NumberFormatException ex) {
                Logging.logService("Invalid poll rate.");
            }
        }

        // Now start the receiver
        startMessageReceiver(idToPort(offset), core);

        Iterator<Integer> it = offsets.iterator();
        String targetOne = String.format("localhost:%d", idToPort(it.next()));
        String targetTwo = String.format("localhost:%d", idToPort(it.next()));

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.
        ManagedChannel channelOne = Grpc.newChannelBuilder(targetOne, InsecureChannelCredentials.create())
                .build();
        ManagedChannel channelTwo = Grpc.newChannelBuilder(targetTwo, InsecureChannelCredentials.create())
                .build();
        boolean serverOneConnected = false;
        boolean serverTwoConnected = false;
        Logging.logService("Waiting for connection...");
        while (!serverOneConnected || !serverTwoConnected) {
            Thread.sleep(1000);
            if (!serverOneConnected) {
                ConnectivityState state = channelOne.getState(true);
                serverOneConnected = state.equals(ConnectivityState.READY);
                if (serverOneConnected) {
                    Logging.logService("Connection one established");
                }
            }
            if (!serverTwoConnected) {
                ConnectivityState state = channelTwo.getState(true);
                serverTwoConnected = state.equals(ConnectivityState.READY);
                if (serverTwoConnected) {
                    Logging.logService("Connection two established");
                }
            }
        }

        try {
            MessageServer server = new MessageServer(channelOne, channelTwo);
            core.setStartTime();
            while (true) {
                Thread.sleep(1000/frequency);

                /**
                 * Logic Loop Here
                 */
                if(!core.messageQueueIsEmpty()) {
                    MessageRequest request = core.peakMessage();
                    receiveMessage(request, core);
                    prettyPrintStatus(frequency, core);
                    core.popMessage();
                } else {
                    int choice = ThreadLocalRandom.current().nextInt(1, 11); 
                    if (choice == 1) {
                        // Send a message to target one
                        server.sendTimeToFirst(core.getTime());
                        sendOneMessage(targetOne, core);
                    } else if (choice == 2) {
                        // Send a message to target two
                        server.sendTimeToSecond(core.getTime());
                        sendOneMessage(targetTwo, core);
                    } else if (choice == 3) {
                        // Send a message to target one and to target two
                        server.sendTimeToFirst(core.getTime());
                        server.sendTimeToSecond(core.getTime());
                        sendTwoMessages(targetOne, targetTwo, core);
                    } else if (choice > 3) {
                        internalEvent(core);
                    }
                    prettyPrintStatus(frequency, core);
                }
            } 
        }catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channelOne.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            channelTwo.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        }
    }

    /**
     * A simple message receiver implementation that prints out the messages
     * it receives from the Server.
     */
    class MessageReceiverImpl extends MessageGrpc.MessageImplBase {

        private final MessageCore core;

        public MessageReceiverImpl(MessageCore core) {
            this.core = core;
        }
        @Override
        public void sendMessage(MessageRequest req, StreamObserver<MessageReply> responseObserver) {
            core.queueMessage(req);
            responseObserver.onNext(GrpcUtil.genSuccessfulReply());
            responseObserver.onCompleted();
        }
    }
