package ProtocolPackage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client  {

    public static final String SERVER_HOSTNAME = "localhost";

    //int PORT = MessagingProtcolConfiguration.PORT;
    DatagramSocket clientSocket;

    String name = null;
    DatagramSocket localSocketAddress;

    int ownPort = MessagingProtocolConfiguration.ownPort;
    int otherPort = MessagingProtocolConfiguration.otherPort;

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
        (new Thread() {
            @Override
            public void run(){

                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));


                // Initialize the client socket.
                try {

                    clientSocket = new DatagramSocket();

                } catch(SocketException ex) {
                    System.err.println(
                            "Failed to initialize the client socket. " +
                                    "Is there a free port?"
                    );
                    ex.printStackTrace();
                }

                // Attempt to identify the server address and set it if it was successfully identified.
                final InetAddress serverAddress;
                try {
                    serverAddress = InetAddress.getByName(SERVER_HOSTNAME);
                } catch (UnknownHostException ex) {
                    System.err.println("Unknown host: " + SERVER_HOSTNAME);
                    ex.printStackTrace();
                    return;
                }

                byte[] initialise = "Hello".getBytes(StandardCharsets.UTF_8);

                //when client is first created, send a message to its server so that the server knows what the address and port are
                try {
                    clientSocket.send(new DatagramPacket(
                            initialise,
                            initialise.length,
                            serverAddress,
                            ownPort));

                } catch (IOException e) {
                    e.printStackTrace();
                }


                //Create a buffer to receive data from the datagram socket.
                byte[] buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];

                // While connected, read a new line and send it to the server and print the result.
                while (!clientSocket.isClosed()) {

                    try {
                        // If our System.in has some bytes ready, then read the input.
                        if (System.in.available() > 0) {
                            String message = reader.readLine();

                            List<Packet> packetsToSend = new ArrayList<Packet>(); //stores the packets needed for the message

                            byte messageBuffer[] = message.getBytes(StandardCharsets.UTF_8); //bytes array for the whole message

                            int headerSize = MessagingProtocolConfiguration.HEADERSIZE; //12 bytes
                            byte packetBuffer[] = new byte[MessagingProtocolConfiguration.BUFFERSIZE]; //header + payload //
                            byte payload[] = new byte[packetBuffer.length-headerSize];

                            int sizeOfPayload = packetBuffer.length - headerSize;

                            int numberPacketsNeeded = 0; //set the default number of packets to be 0
                            int startOfSlice = 0; //start at the beginning of the message
                            int endOfSlice = sizeOfPayload; //

                            if((messageBuffer.length + headerSize) > packetBuffer.length){ //need multiple packets
                                while(startOfSlice <= messageBuffer.length){
                                    payload = new byte[packetBuffer.length-headerSize];
                                    numberPacketsNeeded++;

                                    payload = Arrays.copyOfRange(messageBuffer, startOfSlice, endOfSlice);
                                    int currentPacket = numberPacketsNeeded;

                                    Packet packet = new Packet(currentPacket, Arrays.copyOfRange(payload, 0, payload.length));

                                    packetsToSend.add(packet);

                                    if(endOfSlice + sizeOfPayload > messageBuffer.length){
                                        endOfSlice = messageBuffer.length;
                                    }else{
                                        endOfSlice += sizeOfPayload;
                                    }

                                    startOfSlice += sizeOfPayload;
                                }



                            }else{ //only need one packet
                                payload = messageBuffer;

                                int checksum = Methods.calculateChecksum(payload);

                                Packet packet = new Packet(1, 1, checksum, payload);
                                packet.setTotalPackets(1);
                                packetsToSend.add(packet);
                            }

                            for(Packet p : packetsToSend){
                                System.out.println("packet: " + p.getCurrentPacket());
                                p.setTotalPackets(numberPacketsNeeded);

                                // if the payload doesn't take up the rest of the packet, then just add 0s to the array (after where the payload would be)
                                int sizePayload = p.getPayload().length;
                                if(sizePayload != packetBuffer.length - headerSize){
                                    for(int b = 12+sizePayload; b<packetBuffer.length; b++){
                                        packetBuffer[b] = 0;
                                    }
                                }

                                //means that the last packet's payload won't include characters from the previous packet's payload
                                byte newPayload[] = new byte[packetBuffer.length-headerSize];
                                System.arraycopy(p.getPayload(), 0, newPayload, 0, p.getPayload().length);

                                p.setChecksum(Methods.calculateChecksum(packetBuffer));

                                ByteBuffer header = ByteBuffer.allocate(headerSize);
                                header.putInt(Methods.calculateChecksum(newPayload));
                                header.putInt(p.getCurrentPacket());
                                header.putInt(p.getTotalPackets());

                                // now need to combine header and payload into a byte array (packetBuffer), which is sent in
                                System.arraycopy(header.array(), 0, packetBuffer, 0, headerSize);
                                System.arraycopy(p.getPayload(), 0, packetBuffer, headerSize, p.getPayload().length);

                                clientSocket.send(new DatagramPacket(
                                        packetBuffer,
                                        packetBuffer.length,
                                        serverAddress,
                                        otherPort));

                                System.out.println("sent packet");

                                //receive packet from server
                                var incomingPacket = new DatagramPacket(
                                        buffer,
                                        buffer.length,
                                        serverAddress,
                                        ownPort
                                );

                                clientSocket.receive(incomingPacket);

                                // Convert the raw bytes into a String.
                                var serverResponse = Methods.getPayloadFromPacket(incomingPacket.getData());

                                var messageResponse = new String(
                                        incomingPacket.getData(), 0, incomingPacket.getLength(),
                                        StandardCharsets.UTF_8
                                );

                                System.out.println("server response: " + messageResponse);
                                System.out.println("\n");
                            }
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
            }

        }).start();


    }

}
