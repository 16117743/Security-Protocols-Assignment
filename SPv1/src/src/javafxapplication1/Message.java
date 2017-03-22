/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.javafxapplication1;

public class Message
    implements java.io.Serializable
{
    private String plaintext;//


    private String hash;
    private int isEncrypted;
    private byte[] payload;
    private byte[] ds;
    public boolean encptFlag;
    private int isPic;


    //constructor
    public Message(String msg, byte[]pl)
    {
        this.plaintext = msg; 
        this.hash = "lname";
        this.isEncrypted = 0;
        this.payload = pl;
        this.ds = pl;
    }
    
    public Message(String hash, byte[]pl, byte[] ds, int isEncrypted, int isPic)
    {
        this.plaintext = hash; 
        this.hash = hash;
        this.isEncrypted = isEncrypted;
        this.payload = pl;
        this.ds = ds;
    }
    
    public Message(String hash, byte[]pl, byte[] ds, String pubKey)
    {
        this.plaintext = pubKey; 
        this.hash = hash;
        this.payload = pl;
        this.ds = ds;
    }
    
     //constructor
    public Message(String msg, byte[]pl, int isEncrypted, int isPic)
    {
        this.plaintext = msg; 
        this.hash = "lname";
        this.isEncrypted = isEncrypted;
        this.isPic = isPic;
        this.payload = pl;
        this.ds = pl;
    }
    
    //constructor
    public Message(String msg)
    {
        this.plaintext = msg; 
    }
    
    //getters
    public String plaintext() { return plaintext; }
    
    public String getHash() { return hash; }
    public int getIsEncrypted() { return isEncrypted; }
    public byte[] getPayload(){return payload;}
    public byte[] getds(){return ds;}
    public int getIsPic() {return isPic;}
    
    public void setPlaintext(String change) { plaintext = change; }
    
    //since Message "implements Serializable" we have to implement the readObject function here
    //The readObject method is responsible for reading from the stream and restoring the classes fields
    private void readObject(java.io.ObjectInputStream ois)
        throws java.io.IOException, ClassNotFoundException
    {
        java.io.ObjectInputStream.GetField fields = ois.readFields();
        plaintext = (String)fields.get("plaintext", "(Nobody)");
        hash = (String)fields.get("hash", "(Nobody)");
        payload = (byte[])fields.get("payload", "(Nobody)");
        ds = (byte[])fields.get("ds", "(Nobody)");
        isEncrypted = fields.get("isEncrypted", 0);
        isPic = fields.get("isPic", 0);
        
    }
    
    //since Message "implements Serializable" we have to implement the writeObject function here
    //The writeObject method is responsible for writing the state of the object for its particular 
    //class so that the corresponding readObject method can restore it. 
    private void writeObject(java.io.ObjectOutputStream oos)
        throws java.io.IOException
    {
        java.io.ObjectOutputStream.PutField fields = oos.putFields();
        fields.put("plaintext", plaintext);
        fields.put("hash", hash);     
        fields.put("payload", payload);
        fields.put("ds", ds);
        fields.put("isEncrypted", isEncrypted);
        fields.put("isPic", isPic);
        oos.writeFields();
    }
    
    public String toString()
    {
        return "[Message: textMsg= \n" + plaintext + 
            "]";
    }    
    
    
}
