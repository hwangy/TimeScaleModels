package ScaleModel;

import ScaleModel.grpc.MessageGrpc;
import ScaleModel.grpc.MessageReply;
import ScaleModel.grpc.MessageRequest;
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
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    

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

    public static void main(String[] args) throws Exception {
        MessageCore core = new MessageCore();
        int logicalTime = 0;

        Set<Integer> offsets = new HashSet<>(Arrays.asList(0,1,2));
        while (true) {
            System.out.println("Enter a unique client identifier (0-2).");
            Scanner inputReader = new Scanner(System.in);
            String strOffset = inputReader.nextLine();
            try {
                int offset = Integer.parseInt(strOffset);
                startMessageReceiver(idToPort(offset), core);
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

        // Decide clock frequency
        int frequency = ThreadLocalRandom.current().nextInt(1, 7);
        try {
            MessageServer server = new MessageServer(channelOne, channelTwo);
            while (true) {
                Thread.sleep(1000/frequency);

                /**
                 * Logic Loop Here
                 */
                if(!core.messageQueueIsEmpty()) {
                    MessageRequest request = core.popMessage();
                    int request_logical_time = request.getLogicalTime();
                    int message_queue_length = core.getMessageQueueLength();
                    // Check that this is how we should update the logical time -- especially when slides updated
                    logicalTime = Math.max(logicalTime, request_logical_time) + 1;
                    
                    // We may want to format the system time nicer (here and below).
                    String system_time = java.time.LocalDateTime.now().toString(); 
                    Event requestEvent = new Event("Received a message.", system_time, message_queue_length, logicalTime);
                    Logging.logService(requestEvent.toString());
                } else {
                    int choice = ThreadLocalRandom.current().nextInt(1, 11); 
                    if (choice == 1) {
                        // Send a message to target one
                        MessageRequest request = MessageRequest.newBuilder().setLogicalTime(logicalTime).build();
                        server.receiverOne.sendMessage(request); 

                        logicalTime++;
                        String system_time = java.time.LocalDateTime.now().toString(); 
                        Event requestEvent = new Event("Sent message to " + targetOne, system_time, logicalTime);
                        Logging.logService(requestEvent.toString());
                    } else if (choice == 2) {
                        // Send a message to target two
                        MessageRequest request = MessageRequest.newBuilder().setLogicalTime(logicalTime).build();
                        server.receiverTwo.sendMessage(request);

                        logicalTime++;
                        String system_time = java.time.LocalDateTime.now().toString(); 
                        Event requestEvent = new Event("Sent message to " + targetTwo, system_time, logicalTime);
                        Logging.logService(requestEvent.toString());
                    } else if (choice == 3) {
                        // Send a message to target one and to target two
                        MessageRequest request = MessageRequest.newBuilder().setLogicalTime(logicalTime).build();
                        server.receiverOne.sendMessage(request);
                        server.receiverTwo.sendMessage(request);

                        logicalTime++;
                        String system_time = java.time.LocalDateTime.now().toString(); 
                        Event requestEvent = new Event("Sent message to " + targetOne + " and " + targetTwo, system_time, logicalTime);
                        Logging.logService(requestEvent.toString());
                    } else if (choice > 3) {
                        logicalTime++;
                        String system_time = java.time.LocalDateTime.now().toString(); 
                        Event requestEvent = new Event("Internal event.", system_time, logicalTime);
                        Logging.logService(requestEvent.toString());
                    }
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
