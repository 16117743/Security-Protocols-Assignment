package src.javafxapplication1;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class JavaFXApplication1 extends Application {

    private boolean isServer = false;
    //private boolean isServer = true;
    //private boolean isServer = false;
    
    private EncryptionUtil eu;
    private TextArea messages = new TextArea();
    private NetworkConnection connection = isServer ? createServer() : createClient();

    private Parent createContent() {
        messages.setPrefHeight(550);
        TextField input = new TextField();
        input.setOnAction(event -> {
            String message = isServer ? "Server:" : "Client:";
            message += input.getText();
            input.clear();
            messages.appendText(message + "\n");
            /******************/
            
            try {
                System.out.println("\nattempting to send " + message);
                Message m1 = new Message(message, "bye", 10);
                connection.send(m1);
            } catch (Exception e) {
                messages.appendText("Failed to Send mesage\n");
                e.printStackTrace();
            }
        });
        VBox root = new VBox(20, messages, input);
        root.setPrefSize(600, 600);
        return root;
    }

    @Override
    public void init() throws Exception {
        connection.startConnection();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
        if(!connection.isServer()){
            final String originalText = "Client challenge and public key";
            ObjectInputStream inputStream = null;

            // Encrypt the string using the public key
            inputStream = new ObjectInputStream(new FileInputStream(eu.PUBLIC_KEY_FILE));
            final PublicKey publicKey = (PublicKey) inputStream.readObject();
            final byte[] cipherText = eu.encrypt(originalText, publicKey);
            Message m1 = new Message("\n*************1 -> client sends public key and challenge\n*************", cipherText);
            System.out.println("\n*************1 -> client sends public key and challenge\n*************");
            connection.send(m1);
        }
        //testRSA();
    }

    @Override
    public void stop() throws Exception {
        connection.closeConnection();
    }

    private Server createServer() {
        System.out.println("creating server");
        return new Server(55555, data -> {
            Platform.runLater(() -> {
                Message m1 = (Message) data;
                
                messages.appendText(m1.getFirstName() + "\n");
                
            });
        });
    }

    public Client createClient() {
        System.out.println("creating client");
        return new Client("127.0.0.1 ", 55555, data -> {
            Platform.runLater(() -> {
                Message m1 = (Message) data;
                String str = m1.getFirstName();
                messages.appendText(str + "\n");
            });
        });
    }
    
    public void testRSA(){
         try {
             EncryptionUtil eu = new EncryptionUtil();
      // Check if the pair of keys are present else generate those.
      if (!eu.areKeysPresent()) {
        // Method generates a pair of keys using the RSA algorithm and stores it
        // in their respective files
        eu.generateKey();
      }

      final String originalText = "Text to be encrypted ";
      ObjectInputStream inputStream = null;

      // Encrypt the string using the public key
      inputStream = new ObjectInputStream(new FileInputStream(eu.PUBLIC_KEY_FILE));
      final PublicKey publicKey = (PublicKey) inputStream.readObject();
      final byte[] cipherText = eu.encrypt(originalText, publicKey);

      // Decrypt the cipher text using the private key.
      inputStream = new ObjectInputStream(new FileInputStream(eu.PRIVATE_KEY_FILE));
      final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
      final String plainText = eu.decrypt(cipherText, privateKey);

      // Printing the Original, Encrypted and Decrypted Text
      System.out.println("Original Text: " + originalText);
      System.out.println("Encrypted Text: " +cipherText.toString());
      System.out.println("Decrypted Text: " + plainText);

    } catch (Exception e) {
      e.printStackTrace();
    }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

