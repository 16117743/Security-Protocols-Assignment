package src.javafxapplication1;

import java.io.Serializable;
import java.util.function.Consumer;

public class Server extends NetworkConnection {

    private int port;

    public Server(int port, Consumer<Message> onReceiveCallback) {
        super(onReceiveCallback); //this is same as "new NetworkConnection(onReceivedCallbacl)". calling the constructor, passing 1 arg
        this.port = port;
    }

    @Override
    protected boolean isServer() {
        return true;
    }

    @Override
    protected String getIP() {
        return null;
    }

    @Override
    protected int getPort() {
        return port;
    }

}
