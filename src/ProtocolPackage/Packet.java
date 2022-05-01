package ProtocolPackage;

public class Packet {

    private int currentPacket;
    private int totalPackets;
    private int checksum;
    private byte[] payload;

    public Packet(int currentPacket, int checksum, byte[] payload){
        this.currentPacket = currentPacket;
        this.checksum = checksum;
        this.payload = payload;
    }

    public Packet(int currentPacket, int totalPackets, int checksum, byte[] payload ){
        this.currentPacket = currentPacket;
        this.totalPackets = totalPackets;
        this.checksum = checksum;
        this.payload = payload;
    }

    public Packet(int currentPacket, byte[] payload){
        this.currentPacket = currentPacket;
        this.payload = payload;
    }

    public int getCurrentPacket() {
        return currentPacket;
    }

    public void setCurrentPacket(int currentPacket) {
        this.currentPacket = currentPacket;
    }

    public int getTotalPackets() {
        return totalPackets;
    }

    public void setTotalPackets(int totalPackets) {
        this.totalPackets = totalPackets;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] messageBuffer) {
        this.payload = payload;
    }

}
