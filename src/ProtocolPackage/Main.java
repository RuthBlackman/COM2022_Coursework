package ProtocolPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter the IP address to connect: ");
        String connectedToAddress = reader.readLine();

        while(!Methods.validate(connectedToAddress)){
            System.out.println("Invalid IP address. Please try again: ");
            connectedToAddress = reader.readLine();
        }

        MessagingProtocolConfiguration.connectedToAddress = connectedToAddress;
        System.out.println("Connecting to: " + MessagingProtocolConfiguration.connectedToAddress);


        DatagramSocket clientSocket;

        try {

            clientSocket = new DatagramSocket();

        } catch (SocketException ex) {
            System.err.println(
                    "Failed to initialize the client socket. " +
                            "Is there a free port?"
            );
            ex.printStackTrace();
            return;
        }

        try {
            MessagingProtocolConfiguration.connectedToHost = InetAddress.getByName(MessagingProtocolConfiguration.connectedToAddress);
        } catch (UnknownHostException ex) {
            System.err.println("Unknown host: " + MessagingProtocolConfiguration.connectedToAddress);
            ex.printStackTrace();
            return;
        }

        System.out.println("Sending NEW signal");

        try {
            byte[] newBuffer = new byte[]{'N', 'E', 'W'};
            clientSocket.send(new DatagramPacket(newBuffer, newBuffer.length, MessagingProtocolConfiguration.connectedToHost, MessagingProtocolConfiguration.PORT));

            byte[] alvBuffer = new byte[3];
            var alvDatagram = new DatagramPacket(alvBuffer, alvBuffer.length);
            clientSocket.receive(alvDatagram);
            if (!new String(alvDatagram.getData(), StandardCharsets.UTF_8).equals("ALV")) {
                throw new RuntimeException("Server gave invalid handshake response");
            }
            System.out.println("Sending ALV signal");
            byte[] handshakeFinishBuffer = new byte[]{'A', 'L', 'V'};
            clientSocket.send(new DatagramPacket(handshakeFinishBuffer, handshakeFinishBuffer.length, MessagingProtocolConfiguration.connectedToHost, MessagingProtocolConfiguration.PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Client client = new Client(clientSocket);
        client.start();


        ClientReceive cr = new ClientReceive(clientSocket);
        cr.start();

    }



}
