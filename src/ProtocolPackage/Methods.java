package ProtocolPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

public class Methods {

    /**
     *
     * @param packetBuffer
     * @param option //either checksum, currentPacket, totalPackets
     * return int value representing one of the above options
     */
    public static int getValueFromHeader(byte[] packetBuffer, String option){
        byte tempArray[];
        int value;

        switch(option){
            case "ID":
                tempArray = Arrays.copyOfRange(packetBuffer, 0, 4);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "checksum":
                tempArray = Arrays.copyOfRange(packetBuffer, 4, 8);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "currentPacket":
                tempArray = Arrays.copyOfRange(packetBuffer, 8, 12);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "totalPackets":
                tempArray = Arrays.copyOfRange(packetBuffer, 12, 16);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "payload":
                tempArray = Arrays.copyOfRange(packetBuffer, 16, packetBuffer.length);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            default:
                throw new IllegalArgumentException("Invalid option: "+  option);
        }

        return value;

    }

    /**
     *
     * @param packetBuffer
     * @return String message
     */
    public static String getPayloadFromPacket(byte[] packetBuffer){
        String message;
        byte tempArray[];

        tempArray = Arrays.copyOfRange(packetBuffer, MessagingProtocolConfiguration.HEADERSIZE, packetBuffer.length);
        message = new String(tempArray, StandardCharsets.UTF_8);
        return message;
    }


    public static int calculateChecksum(byte[] buffer){
        CRC32 crc = new CRC32(); //CRC32 algorithm returns a 32-bit (4 byte) checksum value from the input data
        int checksum = 0;


//        System.out.println("Calculating checksum for packet " + Methods.getValueFromHeader(buffer,"currentPacket"));


        if(buffer.length == MessagingProtocolConfiguration.BUFFERSIZE){
            byte[] temp;
            temp = Arrays.copyOfRange(buffer, MessagingProtocolConfiguration.HEADERSIZE, buffer.length);

            crc.update(temp);
            checksum = (int)crc.getValue();
        }else{
            crc.update(buffer);
            checksum = (int)crc.getValue();

        }

        //System.out.println("checksum: " + checksum);
        return checksum;
    }

    public static void sendACK(InetAddress senderAddress, int senderPort, DatagramSocket serverSocket) throws
            IOException
    {
        /*
        byte[] message = new byte[MessagingProtocolConfiguration.BUFFERSIZE-MessagingProtocolConfiguration.HEADERSIZE];
        //   byte[] wholePacket = new byte[MessagingProtcolConfiguration.BUFFERSIZE];
        int checksum =0;
*/
        /*
        message = "ACK".getBytes(StandardCharsets.UTF_8);
        checksum = Methods.calculateChecksum(message);
        Packet p = new Packet(1, 1, checksum, message);
        ByteBuffer header = ByteBuffer.allocate(MessagingProtcolConfiguration.HEADERSIZE);
        header.putInt(p.getChecksum());
        header.putInt(p.getCurrentPacket());
        header.putInt(p.getTotalPackets());


        System.arraycopy(header.array(), 0, wholePacket, 0, 12);
        System.arraycopy(p.getPayload(), 0, wholePacket, 12, p.getPayload().length);
*/
        byte wholePacket[] = new byte[3];
        wholePacket = "ACK".getBytes(StandardCharsets.UTF_8);



        serverSocket.send(new DatagramPacket(
                wholePacket,
                wholePacket.length,
                senderAddress,
                senderPort));



    }

    /*
    public static void sendGotAll(InetAddress senderAddress, int senderPort, DatagramSocket serverSocket) throws
    IOException {
        byte wholePacket[] = new byte[3];
        wholePacket = "Got".getBytes(StandardCharsets.UTF_8);

        serverSocket.send(new DatagramPacket(
                wholePacket,
                wholePacket.length,
                senderAddress,
                senderPort));
    }
*/

    /**
     * Method to send the packets from the server to its own client
     * @param incomingPacket
     */
    /*
    public static void serverSendPacket(DatagramPacket incomingPacket, InetAddress clientAddress, int clientPort,
                                        DatagramSocket serverSocket) throws IOException {
        var message = new String(
                incomingPacket.getData(),
                12,
                incomingPacket.getLength()-12,
                StandardCharsets.UTF_8
        );

        System.out.println("length " + message.length());
        System.out.println(message.split("\0").length);

        var justMessage = message.split("\0");
        if(justMessage.length >0){
            System.out.println("Client: " + justMessage[0]);
            var outgoingPacket = new DatagramPacket(incomingPacket.getData(), incomingPacket.getLength(), clientAddress,
                    clientPort
            );
            serverSocket.send(outgoingPacket);
        }
    }
     */


    /**
     * Method to send the packets from the server to its own client
     * @param receivedPackets
     */
    public static List<DatagramPacket> serverAssembleMessage(List<DatagramPacket> receivedPackets){
        StringBuffer assembledMessage = new StringBuffer();

        for(DatagramPacket packet : receivedPackets){
            //System.out.println("PACKET: " + Methods.getValueFromHeader(packet.getData(), "currentPacket"));
            byte[] buffer = packet.getData();
            assembledMessage.append(new String(buffer, MessagingProtocolConfiguration.HEADERSIZE, buffer.length-MessagingProtocolConfiguration.HEADERSIZE, StandardCharsets.UTF_8)
                    .split("\0")[0]);
        }

        System.out.println("Client: "+ decryptMessage(assembledMessage.toString(),MessagingProtocolConfiguration.shift));
      //  System.out.println("Client: " + assembledMessage.toString());

        List<DatagramPacket> clearedList = new ArrayList<DatagramPacket>();
        return clearedList;
    }

    /**
     *
     * @return
     */
    public static String encryptMessage(String plaintext, int shift){
        String ciphertext = "";
        char alphabet;
        for(int i=0; i < plaintext.length();i++)
        {
            // Shift one character at a time
            alphabet = plaintext.charAt(i);

            // if alphabet lies between a and z
            if(alphabet >= 'a' && alphabet <= 'z')
            {
                // shift alphabet
                alphabet = (char) (alphabet + shift);
                // if shift alphabet greater than 'z'
                if(alphabet > 'z') {
                    // reshift to starting position
                    alphabet = (char) (alphabet+'a'-'z'-1);
                }
                ciphertext = ciphertext + alphabet;
            }

            // if alphabet lies between 'A'and 'Z'
            else if(alphabet >= 'A' && alphabet <= 'Z') {
                // shift alphabet
                alphabet = (char) (alphabet + shift);

                // if shift alphabet greater than 'Z'
                if(alphabet > 'Z') {
                    //reshift to starting position
                    alphabet = (char) (alphabet+'A'-'Z'-1);
                }
                ciphertext = ciphertext + alphabet;
            }
            else {
                ciphertext = ciphertext + alphabet;
            }

        }

        return ciphertext;
    }

    public static String decryptMessage(String ciphertext, int shift){
        String decryptedMessage = "";
        for(int i=0; i < ciphertext.length();i++)

        {
            // Shift one character at a time
            char alphabet = ciphertext.charAt(i);
            // if alphabet lies between a and z
            if(alphabet >= 'a' && alphabet <= 'z')
            {
                // shift alphabet
                alphabet = (char) (alphabet - shift);

                // shift alphabet lesser than 'a'
                if(alphabet < 'a') {
                    //reshift to starting position
                    alphabet = (char) (alphabet-'a'+'z'+1);
                }
                decryptedMessage = decryptedMessage + alphabet;
            }
            // if alphabet lies between A and Z
            else if(alphabet >= 'A' && alphabet <= 'Z')
            {
                // shift alphabet
                alphabet = (char) (alphabet - shift);

                //shift alphabet lesser than 'A'
                if (alphabet < 'A') {
                    // reshift to starting position
                    alphabet = (char) (alphabet-'A'+'Z'+1);
                }
                decryptedMessage = decryptedMessage + alphabet;
            }
            else
            {
                decryptedMessage = decryptedMessage + alphabet;
            }
        }

        return decryptedMessage;
    }

    private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static boolean validate(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

}
