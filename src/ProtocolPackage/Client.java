package ProtocolPackage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client {

    public static final String LOCAL_SERVER_HOSTNAME = MessagingProtocolConfiguration.ownAddress; //"localhost"

    public static final String REMOTE_SERVER_HOSTNAME = MessagingProtocolConfiguration.connectedToAddress;

    int PORT = MessagingProtocolConfiguration.PORT;
    DatagramSocket clientSocket;

    String name = null;
    DatagramSocket localSocketAddress;

    /*
        int ownPort = MessagingProtocolConfiguration.ownPort;
        int otherPort = MessagingProtocolConfiguration.otherPort;
    */
    int previousID = 0;

    public Client(DatagramSocket clientSocket) {
        // Initialize the client socket.
        this.clientSocket = clientSocket;
    }

    private static int _waitingForAcks = 0;

    public static synchronized void registerAck() {
        if (isWaitingForAck()) _waitingForAcks--;
    }

    public static synchronized void requestAck() {
        _waitingForAcks++;
    }

    public static synchronized boolean isWaitingForAck() {
        return _waitingForAcks > 0;
    }

    /**
     *
     * Bunch of setters and getters for the client object
     */

    /**
     * Start the client.
     * Once this is called, the client will constantly wait for user input in
     * the terminal, and for every line entered into the terminal will send it
     * to the server and print a response from the server.
     */
    public void start() {
        (new Thread(() -> {

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));


            // Attempt to identify the server address and set it if it was successfully identified.
            final InetAddress localServerAddress;
            final InetAddress remoteServerAddress;
            try {
                localServerAddress = InetAddress.getByName(LOCAL_SERVER_HOSTNAME);
                remoteServerAddress = InetAddress.getByName(REMOTE_SERVER_HOSTNAME);
            } catch (UnknownHostException ex) {
                System.err.println("Unknown host: " + LOCAL_SERVER_HOSTNAME);
                ex.printStackTrace();
                return;
            }


            //Create a buffer to receive data from the datagram socket.
            byte[] buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];

            System.out.println("Welcome to the simple chatting protocol");

            // While connected, read a new line and send it to the server and print the result.
            while (!clientSocket.isClosed()) {

                try {

                    /**
                     * Sending a message
                     */

                    // If our System.in has some bytes ready, then read the input.
                    if (System.in.available() > 0) {
                        String message = reader.readLine();
                        List<Packet> packetsToSend = new ArrayList<Packet>(); //stores the packets needed for the message

                        //     String encryptedMessage = Methods.encryptMessage(message, MessagingProtocolConfiguration.shift);
                        byte messageBuffer[] = message.getBytes(StandardCharsets.UTF_8); //bytes array for the whole message

                        int headerSize = MessagingProtocolConfiguration.HEADERSIZE; //16 bytes
                        byte packetBuffer[] = new byte[MessagingProtocolConfiguration.BUFFERSIZE]; //header + payload //
                        byte payload[] = new byte[packetBuffer.length - headerSize];

                        int sizeOfPayload = packetBuffer.length - headerSize;

                        int numberPacketsNeeded = 0; //set the default number of packets to be 1
                        int startOfSlice = 0; //start at the beginning of the message
                        int endOfSlice = sizeOfPayload; //

                        int currentID = 0; //ID of the current message
                        //ID of previous message's id - don't want this id to be repeated

                        Random rand = new Random();

                        if ((messageBuffer.length + headerSize) > packetBuffer.length) { //need multiple packets
                            while (startOfSlice <= messageBuffer.length) {
                                payload = new byte[packetBuffer.length - headerSize];


                                if (currentID == 0) {
                                    currentID = rand.nextInt();
                                    while (currentID == previousID) {
                                        currentID = rand.nextInt();
                                    }
                                }

                                payload = Arrays.copyOfRange(messageBuffer, startOfSlice, endOfSlice);
                                int currentPacket = numberPacketsNeeded++;

                                Packet packet = new Packet(currentID, currentPacket, Arrays.copyOfRange(payload, 0, payload.length));

                                packetsToSend.add(packet);

                                if (endOfSlice + sizeOfPayload > messageBuffer.length) {
                                    endOfSlice = messageBuffer.length;
                                } else {
                                    endOfSlice += sizeOfPayload;

                                }

                                startOfSlice += sizeOfPayload;
                            }


                        } else { //only need one packet
                            payload = messageBuffer;

                            currentID = Math.abs(rand.nextInt());
                            while (currentID == previousID) {
                                currentID = Math.abs(rand.nextInt());
                            }

                            int checksum = Methods.calculateChecksum(payload);

                            Packet packet = new Packet(currentID, numberPacketsNeeded++, 1, checksum, payload);
                            packet.setTotalPackets(1);
                            packetsToSend.add(packet);
                            //    System.out.println("set current packet: " + packet.getCurrentPacket());
                            //   System.out.println("set total packets: " + packet.getTotalPackets());
                        }


                        for (Packet p : packetsToSend) {

                            Boolean received = false;

                            while (!received) {

                                /*
                                Sending packet
                                 */
                                //                          System.out.println("packet: " + p.getCurrentPacket());
                                p.setTotalPackets(numberPacketsNeeded);

                                // if the payload doesn't take up the rest of the packet, then just add 0s to the array (after where the payload would be)
                                int sizePayload = p.getPayload().length;
                                if (sizePayload != packetBuffer.length - headerSize) {
                                    for (int b = 16 + sizePayload; b < packetBuffer.length; b++) {
                                        packetBuffer[b] = 0;
                                    }
                                }

                                //means that the last packet's payload won't include characters from the previous packet's payload
                                byte newPayload[] = new byte[packetBuffer.length - headerSize];
                                System.arraycopy(p.getPayload(), 0, newPayload, 0, p.getPayload().length);

                                p.setChecksum(Methods.calculateChecksum(packetBuffer));

                                ByteBuffer header = ByteBuffer.allocate(headerSize);
                                header.putInt(p.getID());
                                header.putInt(Methods.calculateChecksum(newPayload));
                                //header.putInt(3);
                                header.putInt(p.getCurrentPacket());
                                header.putInt(p.getTotalPackets());

                                // now need to combine header and payload into a byte array (packetBuffer), which is sent in
                                System.arraycopy(header.array(), 0, packetBuffer, 0, headerSize);
                                System.arraycopy(p.getPayload(), 0, packetBuffer, headerSize, p.getPayload().length);

                      //          System.out.println("ID:  " + Methods.getValueFromHeader(packetBuffer, "ID"));
                      //          System.out.println("Checksum: " + Methods.getValueFromHeader(packetBuffer, "checksum"));
                      //          System.out.println("Current: " + Methods.getValueFromHeader(packetBuffer, "currentPacket"));
                      //          System.out.println("Total: " + Methods.getValueFromHeader(packetBuffer, "totalPackets"));
                                //                               System.out.println("Message:" + Methods.getPayloadFromPacket(packetBuffer));
                                requestAck();
                                clientSocket.send(new DatagramPacket(
                                        packetBuffer,
                                        packetBuffer.length,
                                        remoteServerAddress,
                                        PORT
                                )); //otherPort
                                System.out.println("Sending packet");

                                //                             System.out.println("sent packet");
                            //    System.out.println("Client 1 sent packet  " + Methods.getValueFromHeader(packetBuffer, "currentPacket"));


                                /*
                                 Receiving ACK
                                 */

//                                System.out.println("Waiting for ACK");

                                //requestAck();
                                while (isWaitingForAck()) {
                                    try {
                                        //noinspection BusyWait
                                        Thread.sleep(50);
                                    } catch (InterruptedException ignored) {
                                    }
                                }

  //                              System.out.println("Received ACK");

//
//                                //receive packet from server
//                                var incomingPacket = new DatagramPacket(
//                                        buffer,
//                                        buffer.length,
//                                        remoteServerAddress,
//                                        PORT
//                                ); //OWN PORT
//
//                                clientSocket.receive(incomingPacket);
//
//                                // Convert the raw bytes into a String.
//                                var messageResponse = new String(
//                                        incomingPacket.getData(), 0, 3,
//                                        StandardCharsets.UTF_8
//                                );
//
//                                //    System.out.println("server response: " + messageResponse);
//                                //    System.out.println("\n");
//
//                                if (messageResponse.equals("ACK")) {
//                                    received = true;
//                                    System.out.println("Received ACK");
//                                }

                                received = true;
                            }
                            packetsToSend = new ArrayList<>();
                        }
                        numberPacketsNeeded = 0;
                    }

                } catch (IOException ex) {
                    // If we encounter an IOException, it means there was a
                    // problem communicating (IO = Input/Output) so we'll log
                    // the error.
                    System.err.println(
                            "A communication error occurred with the server."
                    );
                    ex.printStackTrace();
                    break;
                }

            }
        })).start();


    }
}


class ClientReceive extends Thread {
    final DatagramSocket clientReceiveSocket;
    byte[] buffer = new byte[1024];

    ClientReceive(DatagramSocket clientReceiveSocket) throws SocketException {
        this.clientReceiveSocket = clientReceiveSocket;
    }


    @Override
    public void run() {
        while (!clientReceiveSocket.isClosed()) {
            try {
//                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
//                clientReceiveSocket.receive(dp);

//                if (dp.getLength() == 3) continue;


                List<DatagramPacket> receivedPackets = null;
                List<DatagramPacket> packetsForMessage = null;
                boolean receivedAllPackets = false;

                InetAddress senderAddress = null;
                int senderPort = 0;

                receivedPackets = new ArrayList<>();
                packetsForMessage = new ArrayList<>();

                while (!receivedAllPackets) {

                    buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];
                    var incomingPacket = new DatagramPacket(buffer, buffer.length);
                    clientReceiveSocket.receive(incomingPacket);

                    if (incomingPacket.getLength() == 3) {
                        var signal = new String(incomingPacket.getData(), 0, 3, StandardCharsets.UTF_8);
                        if (signal.equals("ACK")) {
                         //   System.out.println("client received ack: " + Client.isWaitingForAck());
                            Client.registerAck();
                            continue;
                        } else if (signal.equals("ALV")) {
                            // Respond with ALV
                            System.out.println("Sending ALV signal");
                            var signalResponse = new DatagramPacket(signal.getBytes(StandardCharsets.UTF_8), 3, incomingPacket.getAddress(), incomingPacket.getPort());
                            clientReceiveSocket.send(signalResponse);
                            continue;
                        }
                    }

                    senderAddress = incomingPacket.getAddress();
                    senderPort = incomingPacket.getPort();

                    // get packet details
                    int ID = Methods.getValueFromHeader(incomingPacket.getData(), "ID");
                    int checksum = Methods.getValueFromHeader(incomingPacket.getData(), "checksum");
                    int currentPacket = Methods.getValueFromHeader(incomingPacket.getData(), "currentPacket");
                    int totalPackets = Methods.getValueFromHeader(incomingPacket.getData(), "totalPackets");

                    int calculatedChecksum = Methods.calculateChecksum(incomingPacket.getData());

                    //if the checksum in the header matches the calculated checksum, send ACK:
                    if (calculatedChecksum == checksum) {
                        Methods.sendACK(senderAddress, senderPort, clientReceiveSocket); //send ACK to the sender client
                        receivedPackets.add(incomingPacket);
                    //    System.out.println("Client 2 received packet " + currentPacket);

                        if (currentPacket == totalPackets - 1) { //if the current packet is the last packet, then we want to break out of the while loop
                            receivedAllPackets = true;
                        }
                    } else {
                        System.out.println("Checksums don't match");
                        System.out.println("Packet not accepted");
                    }
                }

                for (DatagramPacket p : receivedPackets) {
                    if (packetsForMessage.size() == 0) {
                        packetsForMessage.add(p);
                    } else {
                        if (Methods.getValueFromHeader(packetsForMessage.get(0).getData(), "ID") == Methods.getValueFromHeader(p.getData(), "ID")) {
                            packetsForMessage.add(p);
                        }

                    }
                }

                //have now received all packets
                receivedPackets = Methods.serverAssembleMessage(packetsForMessage);
            } catch (IOException e) {
                e.printStackTrace();

            } catch (Exception ex) {
                System.err.println(
                        "Communication error. " +
                                "Is there a problem with the client?"
                );
                ex.printStackTrace();
            }
        }

    }
}

