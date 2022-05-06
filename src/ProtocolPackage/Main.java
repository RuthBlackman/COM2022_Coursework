package ProtocolPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter your port: ");
        int ownPort = Integer.parseInt(reader.readLine());

        System.out.print("Enter port to connect to: ");
        int otherPort = Integer.parseInt(reader.readLine());

        MessagingProtocolConfiguration.ownPort = ownPort;
        MessagingProtocolConfiguration.otherPort = otherPort;

        System.out.println("your port is " + MessagingProtocolConfiguration.ownPort);
        System.out.println("Talking to port " + MessagingProtocolConfiguration.otherPort );

        System.out.println("Enter the IP address to connect: ");
        String connectedToAddress = reader.readLine();

        while(!Methods.validate(connectedToAddress)){
            System.out.println("Invalid IP address. Please try again: ");
            connectedToAddress = reader.readLine();
        }

        MessagingProtocolConfiguration.connectedToAddress = connectedToAddress;
        System.out.println("Connecting to: " + MessagingProtocolConfiguration.connectedToAddress);

        /*
        if (Methods.validate(MessagingProtocolConfiguration.connectedToAddress)) {
            System.out.println("Connecting to: " + MessagingProtocolConfiguration.connectedToAddress);
        }else{
            System.out.println("invalid IP address");
        }
*/



        Client client = new Client();
        client.start();

        Server server = new Server();
        server.start();

    }



}
