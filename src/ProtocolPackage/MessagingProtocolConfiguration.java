package ProtocolPackage;

public class MessagingProtocolConfiguration {

//    public static int PORT = 5895; //static final

    public static int ownPort;
    public static int otherPort;

    public static int BUFFERSIZE = 1024; //change back to 1024
    public static int HEADERSIZE = 16;

    public static String connectedToAddress;
    public static String ownAddress = "localhost";

    public static String AngeliqueIP = "10.77.7.28";
    public static String ownIP = "10.77.12.134";
    public static String GhassanIP = "10.77.118.59";

    public static int shift = 13; //shift used to encyrpt/decrypt messages

}
