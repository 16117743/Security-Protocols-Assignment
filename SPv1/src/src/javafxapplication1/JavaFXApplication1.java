package src.javafxapplication1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.ThreadLocalRandom;
import sun.misc.BASE64Encoder;


public class JavaFXApplication1 extends Application {

    private boolean isServer = false;//boolean to determine if the application is running as server or client, (Bob or Alice)
    //private boolean isServer = true;

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
                connection.send(new Message(message,b1,0,0)); // passing "encrypt = false" and "isPic = false" into the Message constructor. (0 == false) 
                                                              //these flags are read by the receiver to determine how to handle the msg
            }
            else // else the security button is ON
            {
                connection.send(new Message(message,b1,1,0));// passing "encrypt = true" and isPic = false" into the Message constructor
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
        
        vbox.getChildren().add(global_ImgBtn);//add the img button to both Alice and Bob's GUI

        vbox.setPrefSize(600, 600);//set size of VBox
        return vbox; //return the vbox with all its nodes contained inside it
    }

    @Override
    public void init() throws Exception //performs initialization of the server connection
    {
        if(isServer) //bob can start straight away as he is waiting for someone to connect to him, Alice can't because user needs to enter ip address first
            connection.startConnection();//start the ConnectionThread attribute found in the NetworkConnection class
    }

    @Override
    public void start(Stage primaryStage) throws Exception //starts the program by creating a scene containing nodes, then setting the stage with that scene
                                                          
    {
        primaryStage.setScene(new Scene(createContent())); //set the scene for the primaryStage by adding a VBox to it (createContent() does this)
    
        //"why is it called global_img_button?" -> in order to reference "primaryStage" from within the "start method"
        //"why reference?" -> because we need to show the file chooser on the javafx scene when user hits "imgBtn"
        global_ImgBtn.setOnAction(event -> //lambda expression to define what happens when the imgBtn is pressed
        {
            FileChooser fileChooser = new FileChooser();//create a fileChooser object
            fileChooser.setTitle("Open Resource File");//set title of window
            File file1 = fileChooser.showOpenDialog(primaryStage);//Pops up an "Open File" file chooser dialog
            //file1 is initialized with the contents of the file chosen by the user
            
            try 
            {
                connection.sendPic(file1.getPath());
            } 
            catch (Exception ex) 
            {
                
            }    
        });
        
        primaryStage.show();
        
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
                    aliceTxt.appendText(data.getHash() + "\n");//messages are being pushed to the GUI using the "String hash" field (for the moment)
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
                System.err.println("msg.getHash() " + data.getHash());
                String str = data.getHash();//extract the message from the hash field
                bobTxt.appendText(str + "\n");//append it to the bobTxt area 
            });
        });
    }

    public static void main(String[] args) //program is launched from here
    {
        launch(args);  
    }
}

