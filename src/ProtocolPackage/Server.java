package ProtocolPackage;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Server extends Thread {

    /**
     * Start the server.
     * Once this is called, the server will constantly wait for new messages,
     * until it receives a message of "exit".
     */
    public void start() {
        (new Thread() {
            @Override
            public void run(){

                DatagramSocket serverSocket = null;
                int PORT = MessagingProtocolConfiguration.PORT;

                List<String> clientAddresses = new ArrayList<String>();
                List<String> clientPorts = new ArrayList<String>();
                HashSet<ConnectedClient> existingClients = new HashSet<ConnectedClient>();

                InetAddress ownAddress;

                if (serverSocket != null) return;

                try {
                    serverSocket = new DatagramSocket(PORT);

                    System.out.println("Starting server...");

                    System.out.println("Now listening on port " + PORT + "!");
                    System.out.println("The address is " + serverSocket.getLocalAddress());


                    byte[] buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];

                    while (!serverSocket.isClosed()) {

                        try {

                            List<DatagramPacket> receivedPackets = null;
                            List<DatagramPacket> packetsForMessage = null;
                            Boolean receivedAllPackets = false;

                            InetAddress senderAddress = null;
                            int senderPort = 0;

                            receivedPackets = new ArrayList<DatagramPacket>();
                            packetsForMessage = new ArrayList<DatagramPacket>();

                            while(!receivedAllPackets){

                                buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];
                                var incomingPacket = new DatagramPacket(buffer, buffer.length);
                                serverSocket.receive(incomingPacket);

                                senderAddress = incomingPacket.getAddress();
                                senderPort = incomingPacket.getPort();

                                //check whether client sent NEW, ALV or BYE
                                if(incomingPacket.getLength() == 3){
                                    var signal = new String(incomingPacket.getData(), 0, 3, StandardCharsets.UTF_8);
                                    ConnectedClient connect = new ConnectedClient(incomingPacket.getAddress(), incomingPacket.getPort());
                                    switch (signal) {
                                        case "NEW" -> {
                                            System.out.println("NEW signal received");
                                            byte[] payload = new byte[]{'A', 'L', 'V'};
                                            existingClients.add(connect);
                                            for (var connectedClient : existingClients) {
                                                try {
                                                    serverSocket.send(new DatagramPacket(payload, payload.length, connectedClient.address, connectedClient.port));
                                                } catch (Exception ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                            existingClients.clear();
                                        }
                                        case "ALV" -> {
                                            System.out.println("ALV signal received");
                                            existingClients.add(connect);
                                        }

                                        case "BYE" -> existingClients.remove(connect);
                                    }

                                }else {


                                    // get packet details
                                    int ID = Methods.getValueFromHeader(incomingPacket.getData(), "ID");
                                    int checksum = Methods.getValueFromHeader(incomingPacket.getData(), "checksum");
                                    int currentPacket = Methods.getValueFromHeader(incomingPacket.getData(), "currentPacket");
                                    int totalPackets = Methods.getValueFromHeader(incomingPacket.getData(), "totalPackets");

                                    int calculatedChecksum = Methods.calculateChecksum(incomingPacket.getData());
                                    //                 System.out.println("current packet: " +currentPacket);

                                    System.out.println("Server calculated checksum: " + calculatedChecksum);
                                    System.out.println("Checksum sent in header: " + checksum);

                                    //if the checksum in the header matches the calculated checksum, send ACK:
                                    if (calculatedChecksum == checksum) {
                                        byte[] wholePacket = new byte[3];
                                        wholePacket = "ACK".getBytes(StandardCharsets.UTF_8);

                                        serverSocket.send(new DatagramPacket(
                                                wholePacket,
                                                wholePacket.length,
                                                senderAddress,
                                                senderPort));

                                        receivedPackets.add(incomingPacket);
                                        System.out.println(senderAddress + " " + Methods.getPayloadFromPacket(incomingPacket.getData()).split("\0")[0]);

                                        for(ConnectedClient CC : existingClients){
                                            serverSocket.send(new DatagramPacket(buffer, buffer.length, CC.address, CC.port));
                                        }

                                        if (currentPacket == totalPackets) { //if the current packet is the last packet, then we want to break out of the while loop
                                            receivedAllPackets = true;

                                        }
                                    } else {
                                        System.out.println("Checksums don't match");
                                        System.out.println("Packet not accepted");
                                    }
                                }
                            }

                            for(DatagramPacket p : receivedPackets){
                                if(packetsForMessage.size() == 0){
                                    packetsForMessage.add(p);
                                }else{
                                    if(Methods.getValueFromHeader(packetsForMessage.get(0).getData(), "ID") == Methods.getValueFromHeader(p.getData(), "ID")){
                                        packetsForMessage.add(p);

                                    }

                                }
                            }

                            //have now received all packets
                            System.out.println("before assemble method");

                            for(ConnectedClient CC : existingClients){
                                serverSocket.send(new DatagramPacket(buffer, buffer.length, CC.address, CC.port));
                            }



                        }  catch (Exception ex) {
                            System.err.println(
                                    "Communication error. " +
                                            "Is there a problem with the client?"
                            );
                            ex.printStackTrace();
                        }

                    }

                } catch (SocketException ex) {
                    System.err.println(
                            "Failed to start the server. " +
                                    "Is the port already taken?"
                    );
                    ex.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static void main(String[] args){
        Server server = new Server();
        server.start();
    }

}