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
            System.out.println("sending -> " + data.toString());
            connThread.out.writeObject(data);//
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
    
    private class ConnectionThread extends Thread 
    {
        
        private Socket socket;
        private ObjectOutputStream out;
        private int initCon = 2;
        
        @Override
        public void run() {
            System.out.println((isServer() ? "server" : "client") + " running");
            try (ServerSocket server = isServer() ? new ServerSocket(getPort()) : null;
                    Socket socket = isServer() ? server.accept() : new Socket(getIP(), getPort());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
            {
                this.socket = socket;
                this.out = out;
                socket.setTcpNoDelay(true);
                do {
                    Serializable m1 = (Serializable) in.readObject();
                        if(isServer())
                            SeverbusinessLogic(m1);
                        else
                            ClientBusinessLogic(m1);
                    }
                    while(initCon > 0);
                System.out.print("secure connection established");
                while (true) 
                {
                    //Serializable data = (Serializable) in.readObject();
                    try{
                        Message m1 = (Message) in.readObject();
                        //System.out.println("received " + m1.toString());
                        onReceiveCallback.accept(m1);
                    }
                    catch(IOException io){
                        io.printStackTrace();
                    }
                    
         
                }
            }
            catch (Exception e) {
                onReceiveCallback.accept("Connection Closed");
            }
    }
           
            private void SeverbusinessLogic(Serializable data)
            {
                if(isServer())
                {
                    switch(initCon)
                    {
                        case 2:
                            
                            //m1.dcrptUsingPvtKey()
                            //m1.decryptUsingSendersPublicKey()
                            //m1.compareChallengeToDS();
                            //generateChallenge
                            //generate session key using both challenges
                            //set respChallenge field
                            //set new challenge field
                            //set message as hash of both challenge
                            //digitally sign message field
                            //encrypt object using clients public key
                            initCon--; 
                            System.out.println("data =" + data.toString());
                            System.out.println("server initcon =" + initCon);
                            send(new Message("\n*************2 -> server responds to challenge\n*************", "b", 2));
                            System.out.println("\n*************2 -> server responds to challenge\n*************");
                            break;
                        case 1:         
                            //check challenge response
                            initCon--;
                            System.out.println("data =" + data.toString());
                            System.out.println("server initcon =" + initCon);
                            System.out.println("\n*************4 -> server verifies challenge response\n*************");
                            send(new Message("\n*************4 -> server verifies challenge response\n*************", "b", 2));
                            break;
                        default:
                            break;
                    }
                }
            }
                
            private void ClientBusinessLogic(Serializable data)
            {
                if(!isServer())
                {
                    switch(initCon)
                    {
                        case 2:
                            //m1.dcrptUsingPvtKey()
                            //m1.decryptUsingSendersPublicKey()
                            //m1.compareChallengeToDS();
                            //m1.getHashofBothChallenges();
                            //generate session key
                            //respond to challenge
                            initCon--; 
                            System.out.println("\n*************2 -> server responds to challenge\n*************");
                            System.out.println("data =" + data.toString());
                            System.out.println("client initcon =" + initCon);
                            System.out.println("\n*************3 -> client generates SK\n*************");
                            send(new Message("\n***********4 -> client responds to server challenge","f",12));
                            break;
                        case 1:
                            System.out.println("data =" + data.toString());
                            System.out.println("client initcon =" + initCon);
                            System.out.println("\n***************5-> check if server accepted response to challenge");
                            //check if server accepted response to challenge
                            initCon--; 
                            break;
                        default:
                            break;
                    }
                }
            }
        }//end connection thread class       
}

