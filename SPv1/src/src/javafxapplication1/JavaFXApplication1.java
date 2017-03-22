package src.javafxapplication1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import sun.misc.BASE64Encoder;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.security.InvalidKeyException;
import java.security.MessageDigest;//SHA
import java.security.Signature;//DS
import javax.crypto.Cipher;//AES, RSA
import java.security.PrivateKey;//RSA
import java.security.PublicKey;//RSA
import javax.crypto.spec.IvParameterSpec;//AES
import javax.crypto.spec.SecretKeySpec;//AES


public class JavaFXApplication1 extends Application {

    //private boolean isServer = false;//boolean to determine if the application is running as server or client, (Bob or Alice)
    private boolean isServer = true;

    Button secBtn;// security button
    Button global_ImgBtn; // send pic button
    String ipStr; // String to store ip address extracted from ipinput text field
    PublicKey pubKey;
    PrivateKey privKey;

    private TextArea aliceTxt = new TextArea();// text area for alice's messages
    private TextArea bobTxt = new TextArea();// text area for alice's messages
    
    private NetworkConnection connection = isServer ? createServer() : null;// if it is server(bob), call Server constructor to initialize "connection" variable
                                                    // else leave the connection null for client(alice), wait until alice enters the ip she wants to connect to

    private VBox createContent() // called to setup the GUI, returns a VBox that will be added to the "scene" in the "void start(Stage primaryStage)" method
    {
        System.out.println("createContent() called ");
        aliceTxt.setPrefHeight(400);//set the height of the alice text area
        bobTxt.setPrefHeight(400);//set the height of the bob text area
        
        HBox hbox = new HBox(10,bobTxt,aliceTxt);//create a HBox for bob and alice msgs
            
        secBtn = new Button();//call button constructor for security button to be intialized
        secBtn.setText("Security ON");//set security button label
        
        secBtn.setOnAction(event -> //lambda expression to define what happens when the security button is pressed
                                    // remember this code is only executed when someone presses the button and not when "createContent()" is called
        {
            if(secBtn.getText().equalsIgnoreCase("Security ON"))//toggle the label from on to off
                secBtn.setText("Security OFF");//toggle the label from on to off
            else if(secBtn.getText().equalsIgnoreCase("Security OFF"))//toggle the label from off to on
                secBtn.setText("Security ON");//toggle the label from off to on
        });
        
        TextField inputTextField = new TextField();//create text input field for users to type msgs
        inputTextField.setOnAction(event -> //lambda expression to define what happens when the user hits the "return key"
        {
            String message = isServer ? "Bob:" : "Alice:";//add the name tag to the message if it bob or alice
            message += inputTextField.getText();//add the actual message to the "message" variable by getting it from the input text field
            inputTextField.clear();//clear the msg input field afterwards
            
            if(isServer)//if its the server running, add the msg being sent to the bob text area 
                bobTxt.appendText(message + "\n");
            else    //if its the client running, add the msg being sent to the alice text area 
                aliceTxt.appendText(message + "\n");
            
            byte[] b1 = "hi".getBytes(); // unused byte array, used simply to match the constructor defined for the Message object, will be removed later
           
            if(secBtn.getText().equalsIgnoreCase("Security OFF"))//dont encrypt the message if security is set to off
            {
                //connection.send(new Message(message,b1,0,0)); // passing "encrypt = false" and "isPic = false" into the Message constructor. (0 == false) 
                                                              //these flags are read by the receiver to determine how to handle the msg
                connection.sendString(message,false);
            }
            else // else the security button is ON
            {
                //connection.send(new Message(message,b1,1,0));// passing "encrypt = true" and isPic = false" into the Message constructor
                connection.sendString(message, true);
            }
        });//end inputTextField.setOnAction
        
        Text enterMsgLabel = new Text(); //call constructor to init enterMsgLabel
        enterMsgLabel.setText("Enter message:");//set the title of the msg label
        VBox vbox = new VBox(20); //create an empty VBox for storing the "nodes" ie buttons, text area etc...
        
        vbox.getChildren().add(hbox);//vbox 0 - add hbox of alice and bob text area 
        vbox.getChildren().add(enterMsgLabel);//vbox 1 - add enter msg label        
        vbox.getChildren().add(inputTextField);//vbox 2 - add msg input field
        vbox.getChildren().add(secBtn);//vbox 3 - add security button
        
        global_ImgBtn = new Button(); // init variable by calling constructor
        global_ImgBtn.setText("Image"); //set text on button
  
        Text enterIpLabel = new Text(); // init variable by calling constructor
        enterIpLabel.setText("Enter ip to connect:"); //set text on label

        TextField ipinput = new TextField();//ipinput enter textfield
        ipinput.setOnAction(event -> //set "enter" command to create client object 
        {
            String ipAdressToConnectTo = ipinput.getText();// grab the ip address that the user enters from "ipinput" text field
            connection = createClient(ipAdressToConnectTo);// initialize the "connection" attribute for the JavaFXApplication1 class, 
                                                //createClient function returns a Client object. (Client extends NetworkConnection)
            
            try //the function below throws an exception at some point, so we have to put it in a try catch
            {
            connection.startConnection(); //start the ConnectionThread attribute found in the NetworkConnection class
                                         //try to understand that "connection" is an object of type NetworkConnection within the JavaFXApplication class
                                        //"connection" object has an attribute inside it called "connThread" of type ConnectionThread.
                                       //It is this variable/thread that is started by the line "connection.startConnection()"                              
            }
            catch(Exception e){          
            }
        });//end ipinput.setOnAction
          
        if(isServer==false){
            vbox.getChildren().add(enterIpLabel);//vbox 4 - add ip label to Alice only
            vbox.getChildren().add(ipinput);//vbox 5 - add ip address entry field to Alice only
        }
        
        Button aesBtn = new Button();
        aesBtn.setText("AES");
        
        aesBtn.setOnAction(event -> //set "enter" command to create client object 
        {
            testAES();
        });
        
        
        Button rsaBtn = new Button();
        rsaBtn.setText("RSA");
        
        rsaBtn.setOnAction(event -> //set command to 
        {
            try {
                testRSA();
            }
            catch(Exception e)
            {
            }
        });
 
        Button shahBtn = new Button();
        shahBtn.setText("SHA");
        shahBtn.setOnAction(event -> //set  command to 
        {
            testHash();
        });
        
        Button dsBtn = new Button();
        dsBtn.setText("DS");
        dsBtn.setOnAction(event -> //set  command to 
        {
             testDigitalSig();
        });
        
        
        vbox.getChildren().add(global_ImgBtn);//add the img button to both Alice and Bob's GUI
        
        
        HBox hbox2 = new HBox(25,rsaBtn,aesBtn);//create a HBox for aes and rsa buttons
        
        hbox2.getChildren().add(shahBtn);//add shah button to hbox
        hbox2.getChildren().add(dsBtn);//add ds button to hbox
        
        vbox.getChildren().add(hbox2);//add the hbox consisting of security buttons to the vbox

        vbox.setPrefSize(600, 600);//set size of VBox
        return vbox; //return the vbox with all its nodes contained inside it
    }

    @Override
    public void init() throws Exception //performs initialization of the server connection
    {
        System.out.println("Init() called ");
        if(isServer) //bob can start straight away as he is waiting for someone to connect to him, Alice can't because user needs to enter ip address first
            connection.startConnection();//start the ConnectionThread attribute found in the NetworkConnection class
    }

    @Override
    public void start(Stage primaryStage) throws Exception //starts the program by creating a scene containing nodes, then setting the stage with that scene
                                                          
    {
        System.out.println("start() called ");
        primaryStage.setScene(new Scene(createContent())); //set the scene for the primaryStage by adding a VBox to it (createContent() does this)
    
        //"why is it called global_img_button?" -> in order to reference "primaryStage" from within the "start method"
        //"why reference?" -> because we need to show the file chooser on the javafx scene when user hits "imgBtn"
        global_ImgBtn.setOnAction(event -> //lambda expression to define what happens when the imgBtn is pressed
        {
            FileChooser fileChooser = new FileChooser();//create a fileChooser object
            fileChooser.setTitle("Open Resource File");//set title of window
            File file1 = fileChooser.showOpenDialog(primaryStage);//Pops up an "Open File" file chooser dialog
            //file1 is initialized with the contents of the file chosen by the user
            
            boolean encryptFlag = false;
            
            if(secBtn.getText().equalsIgnoreCase("Security ON"))//toggle the label from on to off
                encryptFlag = true;
            else if(secBtn.getText().equalsIgnoreCase("Security OFF"))//toggle the label from off to on
                encryptFlag = false;
            
            try 
            {
                connection.sendPic(file1.getPath(), encryptFlag);//send the picture using connection thread
                if(isServer == true)
                    bobTxt.appendText("File Sent");
                else
                    aliceTxt.appendText("File Sent"); 
            } 
            catch (Exception ex) 
            {
                
            }    
        });
        
        primaryStage.show();//show the GUI
        
    }
  
    @Override
    public void stop() throws Exception {
        connection.closeConnection();
    }

    private Server createServer() //returns Server object (extends NetworkConnection)
    {
        System.out.println("creating server");
        return new Server //calling the Server Constructor, passing 2 args. the port and the Consumer definition (lambda expression) 
        (4097, //arg 1 the port 
            data -> //arg 2 the consumer object
            { 
                // "data" is an object of type Message, this lambda expression describes the functionality of the "Consumer"
                // The consumer consumes messages produced by "connection.connThread", 
                // messages are produced by the "onReceiveCallback.accept(new Message))"

                Platform.runLater(() -> //safely update the GUI from a non GUI thread (NetworkConnection's connthread)
                {            
                    aliceTxt.appendText(data.plaintext() + "\n");//messages are being pushed to the GUI using the "String hash" field (for the moment)
                                                               // text messages should ideally be contained within "byte [] payload" field of the message object  
                });
            } //end consumer lambda expression 
        );//end return statement
    }

    public Client createClient(String ipstr) //returns Client object (extends NetworkConnection)
    {
        System.out.println("creating client");
        return new Client(ipstr,4097, data -> { // passing 3 args to Client Constructor. ipString, port, consumer object
            Platform.runLater(() -> {
                String str = data.plaintext();//extract the message from the hash field
                bobTxt.appendText(str + "\n");//append it to the bobTxt area 
            });
        });
    }

    public static void main(String[] args) //program is launched from here
    {
        System.out.println("static void main() called");
        launch(args);   
    }
    
    public static void testAES(){
        System.out.println("*******AES Function***********\n");
        
        String key = "Bar12345Bar12345"; // 128 bit aes key
        String initVector = "RandomInitVector"; // 16 bytes IV, each byte is a char
        String plaintext = "plaintext msg 1";
        String encryptedString = aesEncrypt(key, initVector, plaintext);//encrypt message
        System.out.println("message = " + plaintext );
        System.out.println("message encrypted = " + encryptedString);
        System.out.println("decrypted msg = " + aesDecrypt(key, initVector, encryptedString));
    }
    
    public static String aesEncrypt(String key, String initVector, String msg)
    {
        try {
            byte[] initVectorAsBytes = initVector.getBytes("UTF-8");
            IvParameterSpec iv = new IvParameterSpec(initVectorAsBytes);//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);//initialize cipher object with secret key and IV

            byte[] msgAsBytes = msg.getBytes();//convert msg string to byte array
            byte[] encrypted = cipher.doFinal(msgAsBytes);//encrypt msg byte array using aes
           
            return new BASE64Encoder().encode(encrypted);//encode ciphertext using Base64
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    public static String aesDecrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));//class specifies an initialization vector 
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");//create aes key

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");//get AES cipher object, Cipher Block Chaining mode, using PKCS5PADDING
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);//initialize cipher object in decrypt mode

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));//decrypt msg bytes using aes

            return new String(original); //return decryption as a string
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    
    
    public static void testRSA() throws Exception{
    try {
      System.out.println("*******RSA Function***********\n");
      byte[] originalText  = "plaintext message 1 ".getBytes("UTF8");//encode msg into bytes using UTF8
      ObjectInputStream inputStream = null;

      // Encrypt the string using the public key
      inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//for reading from public key file
      final PublicKey publicKey = (PublicKey) inputStream.readObject();//create public key by reading from file
      final byte[] cipherText = encrypt(new String(originalText,"UTF-8"), publicKey);//encrypt originalText using publicKey

      // Decrypt the cipher text using the private key.
      inputStream = new ObjectInputStream(new FileInputStream("keys/private.key"));//for reading from private key file
      final PrivateKey privateKey = (PrivateKey) inputStream.readObject();//create private key by reading from file
      

      // Printing the Original, Encrypted and Decrypted Text
      System.out.println("Original Text: " + new String(originalText,"UTF-8"));//convert originalText to string format using UTF8 decoding
      System.out.println("Encrypted Text: \n" + new String(cipherText,"UTF-8"));//convert cipherText to string format using UTF8 decoding

      final String decryptedText = decrypt(cipherText, privateKey);//encrypt cipherText using privateKey
      System.out.println("Decrypted Text: " + decryptedText);

    } catch (Exception e) {
      e.printStackTrace();
    }
    }   
    
    public static byte[] encrypt(String text, PublicKey pubkey)//returns a ciphertext as byte array
    {
    byte[] cipherText = null;
    try {
      final Cipher cipher = Cipher.getInstance("RSA");     
      cipher.init(Cipher.ENCRYPT_MODE, pubkey);// encrypt the plain text using the public key
      cipherText = cipher.doFinal(text.getBytes());//perform encryption on text
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cipherText;
  }
    
    public static String decrypt(byte[] text, PrivateKey key)//returns a string of the ciphertext decrypted
    {
    byte[] decryptedText = null;
    try 
    {
      final Cipher cipher = Cipher.getInstance("RSA");// get an RSA cipher object
 
      cipher.init(Cipher.DECRYPT_MODE, key);//initialize cipher object using 
      decryptedText = cipher.doFinal(text);// decrypt the text using the private key

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return new String(decryptedText);
  }
    
    public static void testHash(){
        System.out.println("*******Hash Function***********\n");
        String msg = "the message to be hashed";
        String hash = sha256(msg);
        System.out.println("message = " + msg);
        System.out.println("hash of the message = " + hash);
    }
    
    public static String sha256(String msg) {
    try{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");//get a SHA-256 message digest object
        
        byte[] msgAsBytes = msg.getBytes("UTF-8");//put the message into byte array
        
        //Performs a final update on the digest using the specified array of bytes,
        //then completes the digest computation.
        byte[] hash = digest.digest(msgAsBytes);
        StringBuffer hexString = new StringBuffer();//create string buffer object
        
        System.out.println("Hash length in bits =" + hash.length*8);

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
    
    public static void testDigitalSig() 
    {
        System.out.println("*******Digital Signature Function***********\n");
        try
        {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("keys/public.key"));//create inputstream to public key file
        final PublicKey pubKey = (PublicKey) inputStream.readObject();//get the public key from the public.key file

        byte[] dataToBeSigned = "data to be signed".getBytes("UTF8");//encode the byte array in the UTF8 format
        
        inputStream = new ObjectInputStream(new FileInputStream("keys/private.key"));//create input stream for reading private key file
        final PrivateKey privateKey = (PrivateKey) inputStream.readObject();//get private key from file

        Signature sig = Signature.getInstance("MD5WithRSA");//Signatuer object isusing MD5withRSA alogrithm
        sig.initSign(privateKey);//initialize signature object with private key
        sig.update(dataToBeSigned);//suppy signature object with data to be signed
        byte[] signatureBytes = sig.sign();// sign method returns a digital signature as a byte array 
        System.out.println("\nSignature:\n" + new BASE64Encoder().encode(signatureBytes));

        sig.initVerify(pubKey);//initialize the signature object with the public key for verification
        sig.update(dataToBeSigned);//Supply the Signature Object With the Data to be Verified 

        System.out.println("signature verifcation = " + sig.verify(signatureBytes));

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
  
   
}

