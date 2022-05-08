package ProtocolPackage;

import java.net.InetAddress;

public class MessagingProtocolConfiguration {

    public static int PORT = 4545; //static final

    public static int BUFFERSIZE = 1024; //change back to 1024
    public static int HEADERSIZE = 16;

    public static InetAddress connectedToHost;
    public static String connectedToAddress;
    public static String ownAddress = "localhost";

    public static int shift = 13; //shift used to encyrpt/decrypt messages

}
