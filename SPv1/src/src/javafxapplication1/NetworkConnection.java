package src.javafxapplication1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static src.javafxapplication1.JavaFXApplication1.aesEncrypt;
import sun.misc.BASE64Encoder;

public abstract class NetworkConnection {

    private ConnectionThread connThread = new ConnectionThread(); // connection thread for handling input/ouput
    private Consumer<Message> onReceiveCallback;//used to update the JavaFX application GUI
    public int step = 0;
    private SecretKeySpec skeySpec;
    private PublicKey serverPubKey;
    private PrivateKey serverPrivKey;
    private KeyPair keyPair;
    private String aesKeyString;
    String ch1;
    String ch2;
    private PublicKey clientPubKey;
    private PrivateKey clientPrivKey;
    
    public NetworkConnection(Consumer<Message> onReceiveCallback) {//constructor for NetworkConnection
        this.onReceiveCallback = onReceiveCallback;//    
        try
        {
            if(isServer()==false){
                System.out.println("Generating client keys ");
                //connThread.generateRSAKeys(); //generate fresh keys for the client
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//step 1 
                serverPubKey = (PublicKey) inputStream.readObject(); //tell client about servers public key
                inputStream = new ObjectInputStream(new FileInputStream("keys/clientpublic.key"));//step 1 
                clientPubKey = (PublicKey) inputStream.readObject();
                inputStream = new ObjectInputStream(new FileInputStream("keys/clientprivate.key"));//step 1 
                clientPrivKey = (PrivateKey) inputStream.readObject();
            }
            else //set the public and private keys for the server
            {
                System.out.println("Generating server keys ");
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//step 1 
                serverPubKey = (PublicKey) inputStream.readObject();
                inputStream = new ObjectInputStream(new FileInputStream("keys/private.key"));//step 1 
                serverPrivKey = (PrivateKey) inputStream.readObject();
                
                inputStream = new ObjectInputStream(new FileInputStream("keys/clientpublic.key"));//step 1 
                clientPubKey = (PublicKey) inputStream.readObject();
            } 
        }
        catch(Exception e){
            e.printStackTrace();
        }    
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
    
    /***********************SEND PICTURE HERE**************************/
    public void sendPic(String dir, boolean isEncrypted) throws Exception
    {
        File file = new File(dir);//create file object that points to pic file
        byte[] pic = new byte[(int) file.length()];//create a byte array of the size of the pic file

        FileInputStream fis = new FileInputStream(file);//file inputstream for reading bytes from pic file
        fis.read(pic); //read file and put into byte array
        fis.close();//close fileinputstream
        System.out.println("dir = " + dir);
        Path p = Paths.get(dir);
        String filename = p.getFileName().toString();
        
        
        String key = "Bar12345Bar12345"; // 128 bit aes key
        String initVector = "RandomInitVector"; // 16 bytes IV, each byte is a char

        if(isEncrypted)
        {
            System.out.println("Encrypting picture using aes key: " + aesKeyString);
            pic = aesEncryptPic(aesKeyString, "RandomInitVector", pic);   
        }
            
        //arg 1 = filename, arg2 = picbytes, arg 3 = encrypt False, arg4 = isPic True
        send(new Message(filename,pic,1,1));//passing the bytes containing the picture into the Message constructor
    }
    
    public byte[] aesEncryptPic(String key, String initVector, byte[] pic)
    {
        try {
            byte[] initVectorAsBytes = initVector.getBytes("UTF-8");
            IvParameterSpec iv = new IvParameterSpec(initVectorAsBytes);//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);//initialize cipher object with secret key and IV

            byte[] encrypted = cipher.doFinal(pic);//encrypt msg byte array using aes
           
            return encrypted;
            //return new BASE64Encoder().encode(encrypted);//encode ciphertext using Base64
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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
            
            try
            {
                ServerSocket server; //server socket
                if(isServer() == true) //if it is server running
                {
                    server = new ServerSocket(4097); //create server socket on port 4097
                    socket = server.accept();//wait for client to connect on port 4097
                    onReceiveCallback.accept(new Message("Connection established"));
                }
                else //else it is client running
                {
                    server = null;
                    socket = new Socket(getIP(), getPort());// call the clients implementation of getIP and getPort, (method is abstract within this class)
                    onReceiveCallback.accept(new Message("Connection established"));
                }
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());//create outputstream regardless of being server or client
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());//create inputstream regardless of being server or client
                
                //this.socket = socket; //assign the socket defined in the "resources" section to the socket attribute of our ConnectionThread class
                this.out = out;//assign the outputstream defined in the "resources" section to the "out" attribute of our ConnectionThread class
                this.socket.setTcpNoDelay(true);// disables/enables the use of Nagle's Algorithm to control the amount of buffering used when transferring data
                 
                
                try
                {
                    if(isServer()==false)
                    {
                        System.out.println("\n***STEP 1: client sends challenge and public key to server\n*************");
                        int randomNum = ThreadLocalRandom.current().nextInt(10000000, 90000000 + 1);
                        System.out.println("\nChallenge 1: "+ randomNum);

                        ch1 = Integer.toString(randomNum);//convert random int to string 
                        Message msg = rsaEncrypt(ch1, serverPubKey);//encrypt using server's public key 
                        send(msg);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                 
                do 
                {
                    try
                    {
                        Message m1 = (Message) in.readObject();
                        if(isServer())
                            ServerBusinessLogic(m1);
                        else
                            ClientBusinessLogic(m1);
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }while(initCon > 0);
                
                System.out.print("\n********** secure connection established*********\n");
                

                while (true) 
                {
                    try
                    {
                        Message m1 = (Message) in.readObject(); //read Message object from inputstream

                        if(m1.getIsPic() == 0){
                            System.out.println("pic set to false");
                            if(m1.getIsEncrypted()==0)
                            { //check the "isEncrypted" field of the message object
                                System.out.println("\nencryption set to false"); 
                            }
                            else{
                                System.out.println("\nencryption set to true");
                            }
                            onReceiveCallback.accept(m1);//update the GUI on Javafxapplication1
                        }
                        else if (m1.getIsPic() == 1)
                        {
                            System.out.println("pic set to true");
                            
                            String filenameFull = m1.plaintext();//get filename from hash field, (will be updated later)
                            String[] parts = filenameFull.split(Pattern.quote(".")); // Split on period.
                            
                            System.out.println(filenameFull);
                            System.out.println(parts[0]);
                            System.out.println(parts[1]);
                            FileOutputStream fos = new FileOutputStream("pic/"+filenameFull);//create a file with filenameFull in the pic directory
                            
                            byte[] decryptedPic;
                            
                            if(m1.getIsEncrypted()==0) //check the "isEncrypted" field of the message object
                            { 
                                System.out.println("\nencryption set to false"); 
                                fos.write(m1.getPayload()); //get the "byte [] payload" from the msg object. Then write those bytes into the  file
                            }
                            else{
                                System.out.println("\nencryption set to true");
                                decryptedPic = aesPicDecrypt(aesKeyString,"RandomInitVector", m1.getPayload());
                                System.out.println("\nPicture successfully decrypted\n");
                                fos.write(decryptedPic); //get the "byte [] payload" from the msg object. Then write those bytes into the  file
                            }     
                            fos.close();//close the fileoutput stream

                            onReceiveCallback.accept(new Message("File received"));//update the GUI on Javafxapplication1
                        }//end "else msg is a pic"              
                    }
                    catch(IOException io){
                        //io.printStackTrace();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                //onReceiveCallback.accept(new Message("testing", new byte[10]));
            }
        }//end run()
        
        
    private void ServerBusinessLogic(Message data) throws Exception
    {
        if(isServer())
        {
            switch(initCon)
            {
                case 2:
                    System.out.println("\n***STEP 2: server responds to challenge\n*************");
                    initCon--;
                    System.err.println("server initcon =" + initCon);
                    ch1 = rsaDecrypt(data.getPayload(),serverPrivKey);
                    System.out.println("decrypted challenge = " + ch1); 
                    int randomNum = ThreadLocalRandom.current().nextInt(10000000, 90000000 + 1);
                    System.out.println("\nServer's challenge: "+ randomNum);
                    ch2 = Integer.toString(randomNum);//convert random int to string
                    String response1 = ch1;
                    //System.out.println("server retreiving client's public key from plaintext field");
                    //clientPubKey = loadPublicKey(data.plaintext());//setting clientPubKey field
                    Message msg = rsaEncrypt(ch2 + response1, clientPubKey);//encrypt using clients public key
                    send(msg);
                    break;
                case 1: 
                    System.out.println("\n***STEP 4: server verifies challenge response\n*************");
                    initCon--;
                    System.err.println("server initcon =" + initCon);
                    String response2 = rsaDecrypt(data.getPayload(),serverPrivKey);
                    System.out.println("Decrypt Client's response to Server's challenge: " + response2);
                    verifyChallenge(ch2,response2);
                    generateAESKey(ch1,ch2);
                    Message m3 = rsaEncrypt("OK", clientPubKey);//encrypt using client's public key
                    send(m3);       
                    break;
                default:
                    break;
            }
        }
    }
              
    private void ClientBusinessLogic(Message data)
    {
        if(!isServer())
        {
            switch(initCon)
            {
                case 2:
                    System.out.println("\n***STEP 3:\n3.1 Client receives server's response to challenge\n3.2 Client generates AES key\n3.3 Client responds to server's challenge\n*************\n");
                    initCon--; 
                    System.out.println("client initcon =" + initCon);
                    String challengeResponse = rsaDecrypt(data.getPayload(),clientPrivKey);
                    ch2 = challengeResponse.substring(0, 8);
                    String response = challengeResponse.substring(8);
                    System.out.println("decrypted challenge: " + ch2);
                    verifyChallenge(ch1,response);           
                    Message m2 = rsaEncrypt(ch2, serverPubKey);
                    send(m2);
                    break;
                case 1:
                    System.out.println("\n***STEP 5: client checks if server accepted response to challenge\n");
                    System.out.println("client initcon =" + initCon);
                    System.out.println("Sever's response: " + rsaDecrypt(data.getPayload(),clientPrivKey));
                    generateAESKey(ch1,ch2);
                    initCon--;
                    break;
                default:
                    break;
            }
        }
    }
    
    private void verifyChallenge(String challenge, String response){
        try
        {
            if(response.equalsIgnoreCase(challenge))
            {
                System.out.println("Challenge response ACCEPTED");
            }
            else
            {
                System.out.println("Challenge response REJECTED\nterminating connection");
                closeConnection();
            }
        }
        catch(Exception e){
            
        }

    }
        
        
    public void f2()
    {
        try
        {
            generateRSAKeys();
//            int randomNum = ThreadLocalRandom.current().nextInt(10000000, 90000000 + 1);
//            System.out.println("\nChallenge 1: "+ randomNum);
//            String challenge1 = Integer.toString(randomNum);//convert random int to string 
//         
//            Message msg = rsaEncrypt(challenge1);
//         
//            try
//            {
//            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//step 1 
//            final PublicKey publicKey = (PublicKey) inputStream.readObject();
//            String pubkeyStr = savePublicKey(publicKey);
//            System.out.println("public key of sender = " + pubkeyStr);
//            msg.setPlaintext(pubkeyStr); //setting sender public key field, used to verify digital signature
//
//            }catch(Exception io){
//                io.printStackTrace();
//            }
//            System.out.println("\nSimulate sending message to Bob \n*******\n\nBob received message!");
//            //validateHash(msg);
//            //validateDigSig(msg);
//            String encryptedChallenge = new String(msg.getPayload(),"UTF-8");
//            System.out.println("\nEncrypted challenge:\n" + encryptedChallenge);
//            String decryptedChallenge = rsaDecrypt(msg.getPayload());
//            System.out.println("\nDecrypted challenge:\n" + decryptedChallenge);
//            
//            
//            System.out.println("\nBob generates session key\n");
//            String challenge2 = generateResponseChallenge(decryptedChallenge);
//            System.out.println("\nSimulate sending response to Alice \n*******\n\nAlice received message!");
//            System.out.println("\nAlice generates session key");
//            generateAESKey(challenge1,challenge2);
        }
        catch(Exception e){
            e.printStackTrace();
        }      
    }
        
        
    /******************Connection Class methods*********************************************************/
        
    public void validateHash(Message msg)
    {
        System.out.println("\n*********Validating Hash Function*****");
        PrivateKey pvtKey;
            if(isServer())
                pvtKey = serverPrivKey;
            else
                pvtKey = clientPrivKey;
        
        String challenge = rsaDecrypt(msg.getPayload(), pvtKey);
        String hashOfReceivedMsg = sha256(challenge);
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
            PrivateKey pvtKey;
            if(isServer())
                pvtKey = serverPrivKey;
            else
                pvtKey = clientPrivKey;
            
            String msgStr = rsaDecrypt(msg.getPayload(),pvtKey);
            byte[] data = msgStr.getBytes();

            Signature sig = Signature.getInstance("SHA1withRSA");
       
            byte[] signatureBytes = msg.getds();
            System.out.println("\nDigital Signature: \n" + new BASE64Encoder().encode(signatureBytes));

            PublicKey pubKeyOfSender;
            
            if(isServer())
                pubKeyOfSender = clientPubKey;
            else
                pubKeyOfSender = serverPubKey;
            
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
    
    public  void generateRSAKeys() 
    {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
          final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
          keyGen.initialize(1024,random);
          KeyPair key1 = keyGen.generateKeyPair();
          keyPair = key1;

          File privateKeyFile = new File("keys/clientprivate.key");
          File publicKeyFile = new File("keys/clientpublic.key");

          // Create files to store public and private key
          if (privateKeyFile.getParentFile() != null) {
            privateKeyFile.getParentFile().mkdirs();
          }
          privateKeyFile.createNewFile();

          if (publicKeyFile.getParentFile() != null) {
            publicKeyFile.getParentFile().mkdirs();
          }
          publicKeyFile.createNewFile();

          // Saving the Public key in a file
          ObjectOutputStream publicKeyOS = new ObjectOutputStream(
              new FileOutputStream(publicKeyFile));
          publicKeyOS.writeObject(keyPair.getPublic());
          publicKeyOS.close();

          // Saving the Private key in a file
          ObjectOutputStream privateKeyOS = new ObjectOutputStream(
              new FileOutputStream(privateKeyFile));
          privateKeyOS.writeObject(keyPair.getPrivate());
          privateKeyOS.close();

          clientPrivKey = keyPair.getPrivate();
          clientPubKey = keyPair.getPublic();
        } catch (Exception e) {
          e.printStackTrace();
        }
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

    public String rsaDecrypt(byte[] text, PrivateKey privateKey)//returns a string of the ciphertext decrypted
    {
        try 
        {   
//            ObjectInputStream inputStream = null;
//
//            // Decrypt the cipher text using the private key.
//            inputStream = new ObjectInputStream(new FileInputStream("keys/private.key"));//for reading from private key file
//            
//            final PrivateKey privateKey = (PrivateKey) inputStream.readObject();//create private key by reading from file
      
          final Cipher cipher = Cipher.getInstance("RSA");// get an RSA cipher object

          cipher.init(Cipher.DECRYPT_MODE, privateKey);//initialize cipher object using 
          byte[] decryptedText = cipher.doFinal(text);// decrypt the text using the private key

          return new String(decryptedText);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        return null;
   }

    private Message rsaEncrypt(String msg, PublicKey publicKey)
    {
        ObjectInputStream inputStream = null;
        byte[] cipherText;
        try{
//            inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//step 1 
//            final PublicKey publicKey = (PublicKey) inputStream.readObject();

            // get an RSA cipher object 
            final Cipher cipher = Cipher.getInstance("RSA");
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherText = cipher.doFinal(msg.getBytes("UTF-8"));
    
            String hashOfChallenge = sha256(msg);
            byte[] ds = sign(msg); // digitally sign the message

            return new Message(hashOfChallenge,cipherText, ds, 1,0); //setting "is encrypt" to true (4th arg)
        }catch(Exception io){
            io.printStackTrace();
        }
        return null;
    }
    
        
    public void generateAESKey(String ch1,String ch2){
        try 
        {
            System.out.println("\n ****Generate AES key function ******");
            aesKeyString = ch1+ch2;
            System.out.println("Challenge 1 = " + ch1);
            System.out.println("Challenge 2 = " + ch2);
            System.out.println("AES Key generated = " + aesKeyString);

            skeySpec = new SecretKeySpec(aesKeyString.getBytes("UTF-8"), "AES"); //assign the secretly generated key to skeySpec attribute
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
     public byte[] aesEncrypt(String key, String initVector, String msg)
    {
        try {
            byte[] initVectorAsBytes = initVector.getBytes("UTF-8");
            IvParameterSpec iv = new IvParameterSpec(initVectorAsBytes);//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);//initialize cipher object with secret key and IV

            byte[] msgAsBytes = msg.getBytes();//convert msg string to byte array
            byte[] encrypted = cipher.doFinal(msgAsBytes);//encrypt msg byte array using aes
           
            return encrypted;
            //return new BASE64Encoder().encode(encrypted);//encode ciphertext using Base64
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    public byte[] aesDecrypt(String key, String initVector, byte[] encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);//initialize cipher object in decrypt mode

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));//decrypt msg bytes using aes

            return original; //return decryption as a string
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    public byte[] aesPicDecrypt(String key, String initVector, byte[] encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);//initialize cipher object in decrypt mode

            byte[] original = cipher.doFinal(encrypted);//decrypt msg bytes using aes

            return original; //return decryption as a string
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    public String sha256(String msg) {
    try{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");//get a SHA-256 message digest object
        
        byte[] msgAsBytes = msg.getBytes("UTF-8");//put the message into byte array
        
        //Performs a final update on the digest using the specified array of bytes,
        //then completes the digest computation.
        byte[] hash = digest.digest(msgAsBytes);
        StringBuffer hexString = new StringBuffer();//create string buffer object
        
        //System.out.println("Hash length in bits =" + hash.length*8);

        for (int i = 0; i < hash.length; i++)//convert each byte in hash byte array to hex string
        {
            String hex = Integer.toHexString(0xff & hash[i]);//convert 1 byte to hex string
            if(hex.length() == 1) 
                hexString.append('0');
            
            hexString.append(hex);//append recently converted hex to hexString buffer
        }

        return hexString.toString();
    } catch(Exception ex){
       throw new RuntimeException(ex);
    }
    }
    
    

    }//ConnectionThread class
}//end NetworkConnection class

