package ProtocolPackage;

import java.net.InetAddress;
import java.util.Objects;

public class ConnectedClient {

    public final InetAddress address;
    public final int port;

    public ConnectedClient(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectedClient) {
            return ((ConnectedClient) obj).address == address && ((ConnectedClient) obj).port == port;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.port);
    }

    @Override
    public String toString() {
        return String.format("{ address: %s, port: %d }", this.address.getHostName(), this.port);
    }

    public InetAddress getAddress(){
        return this.address;
    }

    public int getPort(){
        return this.port;
    }
}
