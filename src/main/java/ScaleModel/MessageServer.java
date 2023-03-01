package ScaleModel;

import ScaleModel.grpc.MessageGrpc;
import ScaleModel.grpc.MessageReply;
import ScaleModel.grpc.MessageRequest;
import ScaleModel.util.Constants;
import ScaleModel.util.GrpcUtil;
import ScaleModel.util.Logging;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.*;
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

        try {
            MessageServer server = new MessageServer(channelOne, channelTwo);
            while (true) {
                /**
                 * Logic Loop Here
                 */
            }
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channelOne.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            channelTwo.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * A simple message receiver implementation that prints out the messages
     * it receives from the Server.
     */
    static class MessageReceiverImpl extends MessageGrpc.MessageImplBase {

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
}
