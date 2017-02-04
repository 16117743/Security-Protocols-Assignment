package src.javafxapplication1;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.function.Consumer;
import src.javafxapplication1.Security;

public abstract class NetworkConnection {

    private ConnectionThread connThread = new ConnectionThread();
    private Consumer<Serializable> onReceiveCallback;//
    public int step = 0;
    private EncryptionUtil eu;
    
    public NetworkConnection(Consumer<Serializable> onReceiveCallback) {//
        this.onReceiveCallback = onReceiveCallback;//
        connThread.setDaemon(true);//
    }
    
    public void startConnection() throws Exception {
        connThread.start();//
    }
    
    public void send(Serializable data) {
        try{
            Message m1 = (Message)data;
            //System.out.println("sending -> " + m1.getFirstName());
            //System.out.println(m1.getPayload().toString());
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
        public void run() 
        {
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
        }//end run()
           
            private void SeverbusinessLogic(Serializable data)
            {
                if(isServer())
                {
                    switch(initCon)
                    {
                        case 2:
                            System.out.println("\n*************2 -> server responds to challenge\n*************");
                            Message m1 = (Message)data;
                            System.out.println("decrypted msg = " + decrypt(m1));//m1.dcrptUsingPvtKey()
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
                            //System.out.println("data =" + data.toString());
                            System.out.println("server initcon =" + initCon);
                            Message m2 = encrypt("Server's Public key and response to challenge");
                            send(m2);
                            
                            break;
                        case 1: 
                            System.out.println("\n*************4 -> server verifies challenge response\n*************");
                            //check challenge response
                            initCon--;
                            //System.out.println("data =" + data.toString());
                            System.out.println("server initcon =" + initCon);
                            
                            //send(new Message("\n*************4 -> server verifies challenge response\n*************", "b", 2));
                            Message m3 = encrypt("Server sends ack of challenge to client");
                            send(m3);
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
                            System.out.println("\n*************2 -> client receives server's response to challenge\n*************");
                            //m1.dcrptUsingPvtKey()
                            //m1.decryptUsingSendersPublicKey()
                            //m1.compareChallengeToDS();
                            //m1.getHashofBothChallenges();
                            //generate session key
                            //respond to challenge
                            initCon--; 
                           
                            System.out.println("decrypted =" + decrypt((Message)data));
                            System.out.println("client initcon =" + initCon);
                            //System.out.println("\n*************3 -> client generates Session Key\n*************");
                            Message m1 = encrypt("client responds to server challenge");
                            send(m1);
                            break;
                        case 1:
                            System.out.println("\n***************5-> client checks if server accepted response to challenge\n");
                            System.out.println("client initcon =" + initCon);
                            System.out.println("decrypted = " + decrypt((Message)data));
                            initCon--; 
                            break;
                        default:
                            break;
                    }
                }
            }
            
            private String decrypt(Message msgObj)
            {
                try 
                {
                    EncryptionUtil eu = new EncryptionUtil();
                   // Check if the pair of keys are present else generate those.
                   if (!eu.areKeysPresent()) {
                     // Method generates a pair of keys using the RSA algorithm and stores it
                     // in their respective files
                     eu.generateKey();
                   }
                   // Decrypt the cipher text using the private key.
                   ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(eu.PRIVATE_KEY_FILE));
                   final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
                   final String plainText = eu.decrypt(msgObj.getPayload(), privateKey);
                   System.out.println("decrypt func -> " + plainText);
                   return plainText;
                }catch (Exception e) {
                e.printStackTrace();
                }
                return "oops";
            }//end connection thread class    
            
            private Message encrypt(String msg){
                ObjectInputStream inputStream = null;
                byte[] cipherText;
                try{
                    inputStream = new ObjectInputStream(new FileInputStream(eu.PUBLIC_KEY_FILE));
                    final PublicKey publicKey = (PublicKey) inputStream.readObject();
                    cipherText = eu.encrypt(msg, publicKey); 
                    return new Message(msg,cipherText);
                }catch(Exception io){
                    
                }
                return null;
            }
    }//ConnectionThread class
}//end NetworkConnection class

