/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.javafxapplication1;

public class Message
    implements java.io.Serializable
{
    private String textMsg;
    private String lastName;
    private int age;

    public Message(String fn, String ln, int a)
    {
        this.textMsg = fn; this.lastName = ln; this.age = a;
    }
    
    public String getFirstName() { return textMsg; }
    public String getLastName() { return lastName; }
    public int getAge() { return age; }
    
    private void readObject(java.io.ObjectInputStream ois)
        throws java.io.IOException, ClassNotFoundException
    {
        java.io.ObjectInputStream.GetField fields = ois.readFields();
        textMsg = (String)fields.get("textMsg", "(Nobody)");
        lastName = (String)fields.get("lastName", "(Nobody)");
        age = fields.get("age", 0);
    }
    
    private void writeObject(java.io.ObjectOutputStream oos)
        throws java.io.IOException
    {
        java.io.ObjectOutputStream.PutField fields = oos.putFields();
        fields.put("textMsg", textMsg);
        fields.put("lastName", lastName);
        fields.put("age", age);
        oos.writeFields();
    }
    
    public void setFirstName(String value) { textMsg = value; }
    public void setLastName(String value) { lastName = value; }
    public void setAge(int value) { age = value; }
    
    public String toString()
    {
        return "[Message: textMsg= " + textMsg + 
            " lastName=" + lastName +
            " age=" + age +
            "]";
    }    
    
    
}
