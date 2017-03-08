package src.javafxapplication1;

import java.io.Serializable;
import java.util.function.Consumer;

public class Client extends NetworkConnection {

    private String ip;
    private int port;
    

    public Client(String ipadd, int port, Consumer<Message> onReceiveCallback) {
        super(onReceiveCallback);//calling the inherited class constructor new NetworkCOnnection(arg)
        this.ip = ipadd;
        this.port = port;
    }

    @Override
    protected boolean isServer() {
        return false;
    }

    @Override
    protected String getIP() {
        return ip;
    }

    @Override
    protected int getPort() {
        return port;
    }

}
