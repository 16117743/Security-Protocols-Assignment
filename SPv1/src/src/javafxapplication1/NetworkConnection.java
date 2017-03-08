package src.javafxapplication1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Encoder;

public abstract class NetworkConnection {

    private ConnectionThread connThread = new ConnectionThread(); // connection thread for handling input/ouput
    private Consumer<Message> onReceiveCallback;//used to update the JavaFX application GUI
    public int step = 0;
    private EncryptionUtil eu; //encryption util object for performing encryption, hashing, digital signatures
    private SecretKeySpec skeySpec;
    
    public NetworkConnection(Consumer<Message> onReceiveCallback) {//constructor for NetworkConnection
        eu  = new EncryptionUtil();
        this.onReceiveCallback = onReceiveCallback;//
        connThread.setDaemon(true);//
    }
    
    public void startConnection() throws Exception {
        connThread.start();//starts "run()" method found below in ConnectionThread
    }
    
    public void send(Message data)
    {
        try 
        {
            if(data != null){
                connThread.out.writeObject(data); //accessing the OutputStream (out) attribute of connThread,
                                                  //using out's writeObject method to send our message object which implements serializable
            }
            else
                System.err.println("ERROR: Data is null ");  
            }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public void closeConnection() throws Exception {
        connThread.socket.close();
    }
    
    public void sendPic(String dir) throws Exception
    {
        File file = new File(dir);
        byte[] pic = new byte[(int) file.length()];//create a byte array of the size of the pic file

        FileInputStream fis = new FileInputStream(file);
        fis.read(pic); //read file into bytes[]
        fis.close();
        send(new Message("pic1",pic));//passing the bytes containing the picture into the Message constructor

    }
    
    protected abstract boolean isServer();
    
    protected abstract String getIP();
    
    protected abstract int getPort();
    
    private class ConnectionThread extends Thread //this a private class of the NetworkConnection class, below is only the implementation.
                                                 // the actual object is the attribute  "private ConnectionThread connThread" defined at the top
                                                //think of it as NetworkConnection has a ConnectionThread object
                                               //also this class extends thread, which means that it can run seperately to main application, think of it as "run in the background"
    {
        //these are the attributes of the ConnectionThread class
        private Socket socket;//socket for the "full duplex" connection.
        private ObjectOutputStream out;//output stream. Inputstream is declared locally within the run() method.
        private int initCon = 2;
        
        @Override
        public void run() // this is what runs when we say connThread.start() within the startConnection() method.
        {
            System.out.println((isServer() ? "server" : "client") + " running"); // "if this is true?" then do this : else do this
            
            
            if(isServer()){
                f2();
            }
            //below is a "try-with-resources"
            //the resources are within the try(resources) {code}
            //Resources will automatically be close after the try at the bottom of this code
            
            try (ServerSocket server = isServer() ? new ServerSocket(getPort()) : null; // if it's the server running create a ServerSocket
                    Socket socket = isServer() ? server.accept() : new Socket(getIP(), getPort());//if it is the server wait for client to connect, else create a new Socket for the Client
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());//create outputstream regardless of being server or client
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))//create inputstream regardless of being server or client
            {
                this.socket = socket; //assign the socket defined in the "resources" section to the socket attribute of our ConnectionThread class
                this.out = out;//assign the outputstream defined in the "resources" section to the "out" attribute of our ConnectionThread class
                socket.setTcpNoDelay(true);
                 
                System.out.print("secure connection established");

                while (true) 
                {
                    try
                    {
                        Message m1 = (Message) in.readObject(); //read Message object from inputstream
                        
                        if(m1.getIsEncrypted()==0) //check the "isEncrypted" field of the message object
                            System.out.println("encryption set to false");
                        else
                            System.out.println("encryption set to true");
                        
                        if(m1.getHash().equalsIgnoreCase("pic1")) //pic is currently only checked by assigning the "hash" field the value "pic1", will be updated
                        {
                            System.out.println("PIC 1 RECEIVED");
                            FileOutputStream fos = new FileOutputStream("pic/received.png");//create a file called "received.png" in the pic directory
                            fos.write(m1.getPayload()); //get the "byte [] payload" from the msg object. Then write those bytes into the "received.png" file
                            fos.close();//close the fileoutput stream
                        }
                        
                        onReceiveCallback.accept(m1);//update the GUI on Javafxapplication1
                    }
                    catch(IOException io){
                        io.printStackTrace();
                    }
                }
            }//this is where our resources would be close, as it is the end of the initial "try"
            catch (Exception e) {
                e.printStackTrace();
                //onReceiveCallback.accept(new Message("testing", new byte[10]));
            }
        }//end run()
        
        private void validateNull(Message m1)
        {
            if(m1.getPayload()==null)
                System.err.println("payload 1 is null");
        }
        
        
    public void f1()
    {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10000 + 1);
        System.out.println("\nChallenge is "+ randomNum);
        Message m1 = encrypt2(Integer.toString(randomNum));

        System.out.println("\nSimulate sending message\n*******\n\nMessage received!");
        String challenge = decrypt(m1);
        System.out.println("\nDecrypted challenge:\n" + challenge);
    }
    
    public void f2()
    {
        try
        {
            int randomNum = ThreadLocalRandom.current().nextInt(1, 10000 + 1);
            System.out.println("\nChallenge 1: "+ randomNum);
            String challenge1 = Integer.toString(randomNum);//convert random int to string 
         
            Message msg = encrypt2(challenge1);
         
            try
            {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//step 1 
            final PublicKey publicKey = (PublicKey) inputStream.readObject();
            String pubkeyStr = savePublicKey(publicKey);
            System.out.println("public key of sender = " + pubkeyStr);
            msg.setLastName(pubkeyStr); //setting sender public key field, used to verify digital signature

            }catch(Exception io){
                io.printStackTrace();
            }
            System.out.println("\nSimulate sending message to Bob \n*******\n\nBob received message!");
            validateHash(msg);
            validateDigSig(msg);
            String encryptedChallenge = new String(msg.getPayload(),"UTF-8");
            System.out.println("\nEncrypted challenge:\n" + encryptedChallenge);
            String decryptedChallenge = decrypt(msg);
            System.out.println("\nDecrypted challenge:\n" + decryptedChallenge);
            
            
            System.out.println("\nBob generates session key\n");
            String challenge2 = generateResponseChallenge(decryptedChallenge);
            System.out.println("\nSimulate sending response to Alice \n*******\n\nAlice received message!");
            System.out.println("\nAlice generates session key");
            generateAESKey(challenge1,challenge2);
        }
        catch(Exception e){
            
        }      
    }

    public void validateHash(Message msg)
    {
        System.out.println("\n*********Validating Hash Function*****");
        String challenge = decrypt(msg);
        String hashOfReceivedMsg = msg.sha256(challenge);
        String appendedHash = msg.getHash();

        System.out.println("\nHash Of Received Msg: \n" + hashOfReceivedMsg);
        System.out.println("\nAppended hash: \n" + appendedHash);

        if(hashOfReceivedMsg.equals(appendedHash)){
            System.out.println("\nAppended hash is the same as the hash of the payload received!\n");
        }
    }
        
    public void validateDigSig(Message msg)
    {
        try
        {
            System.out.println("\n*********Validating Digital Signature Function*****");

            String msgStr = decrypt(msg);
            byte[] data = msgStr.getBytes();

            Signature sig = Signature.getInstance("SHA1withRSA");
       
            byte[] signatureBytes = msg.getds();
            System.out.println("\nDigital Signature: \n" + new BASE64Encoder().encode(signatureBytes));

            System.out.println("\nGet public key of sender appended to message object");
            String pubKeyOfSenderString = msg.getLastName();
            System.out.println("\nPublic key of sender = " + pubKeyOfSenderString);
            System.out.println("\nConverting public key string to PublicKey object");
            PublicKey pubKeyOfSender = loadPublicKey(pubKeyOfSenderString);
            
            sig.initVerify(pubKeyOfSender);
            System.out.println("\nVerifying Digital Signature using Public Key of the sender");
            sig.update(data);

            System.out.println("\nsignature verifcation = " + sig.verify(signatureBytes)+ "!!" );
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
        
    public byte[] sign(String data) throws InvalidKeyException, Exception
    {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/private.key"));
                final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(privateKey);
		rsa.update(data.getBytes());
		return rsa.sign();
    }
    
    public String savePublicKey(PublicKey publ) throws Exception {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
        
        //return new BASE64Encoder().encode(spec.getEncoded());
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }
    
    public PublicKey loadPublicKey(String stored) throws Exception {
        byte[] data =  Base64.getDecoder().decode(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        
        return fact.generatePublic(spec);
    }
    
    public String generateResponseChallenge(String ch1){
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10000 + 1);
        System.out.println("\nChallenge 2 = "+ randomNum);
        String ch2 = Integer.toString(randomNum);//convert random int to string
        generateAESKey(ch1,ch2);
        return ch2;
    }
    
    public void generateAESKey(String ch1,String ch2){
        try 
        {
            System.out.println("\n ****Generate AES key function ******");
            String aesKey = ch1+ch2;
            System.out.println("Challenge 1 = " + ch1);
            System.out.println("Challenge 2 = " + ch2);
            System.out.println("AES Key generated = " + aesKey);

            skeySpec = new SecretKeySpec(aesKey.getBytes("UTF-8"), "AES"); //assign the secretly generated key to skeySpec attribute
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
 
    private String decrypt(Message msgObj)
    {
        try 
        {
           // Decrypt the cipher text using the private key.
           ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(eu.PRIVATE_KEY_FILE));
           final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
           final String plainText = eu.decrypt(msgObj.getPayload(), privateKey);
           return plainText;
        }catch (Exception e) {
        e.printStackTrace();
        }
        return "oops\n";
    }//end connection thread class    
            
    
    
    private Message encrypt2(String msg)
    {
        ObjectInputStream inputStream = null;
        byte[] cipherText;
        try{
            inputStream = new ObjectInputStream(new FileInputStream(eu.PUBLIC_KEY_FILE));//step 1 
            final PublicKey publicKey = (PublicKey) inputStream.readObject();

            // get an RSA cipher object 
            final Cipher cipher = Cipher.getInstance("RSA");
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherText = cipher.doFinal(msg.getBytes("UTF-8"));
    
            String hashOfChallenge = eu.sha256(msg);
            byte[] ds = sign(msg); // digitally sign the message

            return new Message(hashOfChallenge,cipherText, ds, 1,0); //setting "is encrypt" to true (4th arg)
        }catch(Exception io){

        }
        return null;
    }

    }//ConnectionThread class
}//end NetworkConnection class

