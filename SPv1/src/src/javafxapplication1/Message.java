/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.javafxapplication1;

import java.security.MessageDigest;

public class Message
    implements java.io.Serializable
{
    private String hashString;//
    private String lastName;
    private int isEncrypted;
    private byte[] payload;
    private byte[] ds;
    public boolean encptFlag;
    public boolean picFlag;

    public Message(String msg, byte[]pl)
    {
        this.hashString = msg; 
        this.lastName = "lname";
        this.isEncrypted = 0;
        this.payload = pl;
        this.ds = pl;
    }
    
    public Message(String hash, byte[]pl, int isEncrypted, int isPic)
    {
        this.hashString = hash; 
        this.lastName = "lname";
        this.isEncrypted = isEncrypted;
        this.payload = pl;
        this.ds = pl;
    }
    
    public Message(String hash, byte[]pl, byte[] ds, int isEncrypted, int isPic)
    {
        this.hashString = hash; 
        this.lastName = "lname";
        this.isEncrypted = isEncrypted;
        this.payload = pl;
        this.ds = ds;
    }
    
    public Message(String hash, byte[]pl, byte[] ds, String pubKey)
    {
        this.hashString = hash; 
        this.lastName = pubKey;
        this.payload = pl;
        this.ds = ds;
    }
    
    public Message(String msg, String p2, int a){
        this.hashString = msg;
        this.lastName = p2;
        this.isEncrypted = a;
    }
    
    public String getHash() { return hashString; }
    public String getLastName() { return lastName; }
    public int getIsEncrypted() { return isEncrypted; }
    public byte[] getPayload(){return payload;}
    public byte[] getds(){return ds;}
    
    private void readObject(java.io.ObjectInputStream ois)
        throws java.io.IOException, ClassNotFoundException
    {
        java.io.ObjectInputStream.GetField fields = ois.readFields();
        hashString = (String)fields.get("hashString", "(Nobody)");
        lastName = (String)fields.get("lastName", "(Nobody)");
        payload = (byte[])fields.get("payload", "(Nobody)");
        ds = (byte[])fields.get("ds", "(Nobody)");
        isEncrypted = fields.get("isEncrypted", 0);
        
        
//        encptFlag = (boolean)fields.get("encptFlag", "(Nobody)");
//        picFlag = (boolean)fields.get("picFlag", "(Nobody)");
    }
    
    private void writeObject(java.io.ObjectOutputStream oos)
        throws java.io.IOException
    {
        java.io.ObjectOutputStream.PutField fields = oos.putFields();
        fields.put("hashString", hashString);
        fields.put("lastName", lastName);
        fields.put("isEncrypted", isEncrypted);
        fields.put("payload", payload);
        fields.put("ds", ds);
//        fields.put("encptFlag", encptFlag);
//        fields.put("picFlag", picFlag);
        oos.writeFields();
    }
    
    public void setFirstName(String value) { hashString = value; }
    public void setLastName(String value) { lastName = value; }
    public void setAge(int value) { isEncrypted = value; }
    
    public String toString()
    {
        return "[Message: textMsg= \n" + hashString + 
            "]";
    }  
    
    public String sha256(String base) {
    try{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base.getBytes("UTF-8"));
        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    } catch(Exception ex){
       throw new RuntimeException(ex);
    }
    }
    
    
}
