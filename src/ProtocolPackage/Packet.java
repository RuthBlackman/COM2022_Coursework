package ProtocolPackage;

public class Packet {
    private int ID;
    private int currentPacket;
    private int totalPackets;
    private int checksum;
    private byte[] payload;

    public Packet(int ID, int currentPacket, int totalPackets, int checksum, byte[] payload ){
        this.ID = ID;
        this.currentPacket = currentPacket;
        this.totalPackets = totalPackets;
        this.checksum = checksum;
        this.payload = payload;
    }

    public Packet(int ID, int currentPacket, byte[] payload){
        this.ID = ID;
        this.currentPacket = currentPacket;
        this.payload = payload;
    }

    public int getID(){
        return this.ID;
    }

    public int getCurrentPacket() {
        return currentPacket;
    }

    public int getTotalPackets() {
        return totalPackets;
    }

    public void setTotalPackets(int totalPackets) {
        this.totalPackets = totalPackets;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public byte[] getPayload() {
        return payload;
    }


}
