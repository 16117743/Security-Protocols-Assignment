/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.javafxapplication1;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.KeyPair;

/**
 *
 * @author tom
 */
public class Security {
    private byte[] rcvdata;
    
    public Security(Serializable data) throws Exception{
        //ByteArrayInputStream bi = new ByteArrayInputStream(data);
        //ObjectInputStream oi = new ObjectInputStream(data);
        Object obj = data;
        //System.out.println("sending " + eu.key.getPublic().toString());
        if(obj instanceof KeyPair){
            System.out.println("true");
            System.out.println(obj.toString());
        }
        else
            System.out.println("false");
    }
    
    public String getMsg(){
        return "hi";
    }
}
