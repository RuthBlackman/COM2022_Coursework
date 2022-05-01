package ProtocolPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
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
              //  int PORT = MessagingProtocolConfiguration.PORT;

                int ownPort = MessagingProtocolConfiguration.ownPort;
                int otherPort = MessagingProtocolConfiguration.otherPort;

                InetAddress ownAddress;

                if (serverSocket != null) return;

                try {

                    serverSocket = new DatagramSocket(ownPort);

                    System.out.println("Starting server...");

                    System.out.println("Now listening on port " + ownPort + "!");

                    byte[] buffer = new byte[MessagingProtocolConfiguration.BUFFERSIZE];



                    //first packet from own client
                    var firstPacket = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(firstPacket);

                    ownAddress = firstPacket.getAddress();
                    ownPort = firstPacket.getPort();



                    while (!serverSocket.isClosed()) {

                        try {

                            List<byte[]> receivedBuffers = new ArrayList<byte[]>();
                            Boolean receivedAllPackets = false;

                            InetAddress senderAddress = null;
                            int senderPort = 0;

                            while(!receivedAllPackets){
                                var incomingPacket = new DatagramPacket(buffer, buffer.length);
                                serverSocket.receive(incomingPacket);

                                senderAddress = incomingPacket.getAddress();
                                senderPort = incomingPacket.getPort();

                                // get packet details
                                int checksum = Methods.getValueFromHeader(incomingPacket.getData(),"checksum");
                                int currentPacket = Methods.getValueFromHeader(incomingPacket.getData(),"currentPacket");
                                int totalPackets = Methods.getValueFromHeader(incomingPacket.getData(),"totalPackets");

                                int calculatedChecksum = Methods.calculateChecksum(incomingPacket.getData());


                                //if the checksum in the header matches the calculated checksum, send ACK:
                                if(calculatedChecksum == checksum){
                                    Methods.sendACK(senderAddress, senderPort, serverSocket); //send ACK to the sender client
                                    Methods.serverSendPacket(incomingPacket, senderAddress, ownPort, serverSocket); //send message to own client
                                    receivedBuffers.add(incomingPacket.getData());


                                    if(currentPacket == totalPackets){
                                        receivedAllPackets = true;
                                    }
                                }//else do nothing
                            }

                            //have now received all packets
/*
                            ByteBuffer messageBuffer = null;
                            for(byte[] b : receivedBuffers){
                                messageBuffer.put(b);
                            }
                            DatagramPacket wholeMessage = new DatagramPacket("hi".getBytes(StandardCharsets.UTF_8), "hi".getBytes(StandardCharsets.UTF_8).length);
                            Methods.serverSendPacket(wholeMessage, senderAddress, ownPort, serverSocket);
*/

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

}