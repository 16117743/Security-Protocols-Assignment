package src.javafxapplication1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import src.javafxapplication1.Security;

public abstract class NetworkConnection {

    private ConnectionThread connThread = new ConnectionThread();
    private Consumer<Serializable> onReceiveCallback;//
    public int step = 0;
    
    public NetworkConnection(Consumer<Serializable> onReceiveCallback) {//
        this.onReceiveCallback = onReceiveCallback;//
        connThread.setDaemon(true);//
    }
    
    public void startConnection() throws Exception {
        connThread.start();//
    }
    
    public void send(Serializable data) {
        try{
            System.out.print(data.toString());
            Person ted = new Person("Ted", "Neward", 39);
        System.out.print(ted.toString());
        connThread.out.writeObject(ted);//
        }
        catch(IOException io){
            io.printStackTrace();
            
        }
      
    }
    
    public void closeConnection() throws Exception {
        connThread.socket.close();
    }
    
    protected abstract boolean isServer();
    
    protected abstract String getIP();
    
    protected abstract int getPort();
    
    private class ConnectionThread extends Thread {
        
        private Socket socket;
        private ObjectOutputStream out;
        
        @Override
        public void run() {
            System.out.println((isServer() ? "server" : "client") + " running");
            try (ServerSocket server = isServer() ? new ServerSocket(getPort()) : null;
                    Socket socket = isServer() ? server.accept() : new Socket(getIP(), getPort());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                this.socket = socket;
                this.out = out;
                socket.setTcpNoDelay(true);
                while (true) {
                    //Serializable data = (Serializable) in.readObject();
                    try{
                        Person p1 = (Person) in.readObject();
                    System.out.println(p1);
                    onReceiveCallback.accept(p1);
                    }
                    catch(IOException io){
                        io.printStackTrace();
                    }
                    
         
                }
            } catch (Exception e) {
                onReceiveCallback.accept("Connection Closed");
            }
            
        }
    }
}
