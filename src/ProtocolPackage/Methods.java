package ProtocolPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
            case "checksum":
                tempArray = Arrays.copyOfRange(packetBuffer, 0, 4);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "currentPacket":
                tempArray = Arrays.copyOfRange(packetBuffer, 4, 8);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "totalPackets":
                tempArray = Arrays.copyOfRange(packetBuffer, 8, 12);
                value = ByteBuffer.wrap(tempArray).getInt();
                break;
            case "payload":
                tempArray = Arrays.copyOfRange(packetBuffer, 12, packetBuffer.length);
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

        tempArray = Arrays.copyOfRange(packetBuffer, 12, packetBuffer.length);
        message = new String(tempArray, StandardCharsets.UTF_8);
        return message;
    }


    public static int calculateChecksum(byte[] buffer){
        CRC32 crc = new CRC32(); //CRC32 algorithm returns a 32-bit (4 byte) checksum value from the input data
        int checksum = 0;



        if(buffer.length == MessagingProtocolConfiguration.BUFFERSIZE){
            byte[] temp;
            temp = Arrays.copyOfRange(buffer, 12, buffer.length);

            crc.update(temp);
            checksum = (int)crc.getValue();
        }else{
            crc.update(buffer);
            checksum = (int)crc.getValue();

        }

        return checksum;
    }

    public static void sendACK(InetAddress senderAddress, int senderPort, DatagramSocket serverSocket) throws IOException {
        byte[] message = new byte[MessagingProtocolConfiguration.BUFFERSIZE-MessagingProtocolConfiguration.HEADERSIZE];
        //   byte[] wholePacket = new byte[MessagingProtcolConfiguration.BUFFERSIZE];
        int checksum =0;

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
    public static void sendGotAll(InetAddress senderAddress, int senderPort, DatagramSocket serverSocket) throws IOException {
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
    public static void serverSendPacket(DatagramPacket incomingPacket, InetAddress clientAddress, int clientPort, DatagramSocket serverSocket) throws IOException {
        var message = new String(
                incomingPacket.getData(),
                12,
                incomingPacket.getLength()-12,
                StandardCharsets.UTF_8
        );

        System.out.println("Client: " + message.split("\0")[0]);
        var outgoingPacket = new DatagramPacket(incomingPacket.getData(), incomingPacket.getLength(), clientAddress, clientPort
        );

        serverSocket.send(outgoingPacket);
    }
}
