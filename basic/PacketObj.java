package basic;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.Serializable;

/**
 * Created by Shobhit on 14/4/2018.
 */

public class PacketObj implements Serializable{
    Packet type;
    int length;
    String mesage;

    public PacketObj(Packet type, int length, String mesage) {
        this.type = type;
        this.length = length;
        this.mesage = mesage;
    }
    
    public Packet getType() {
        return type;
    }
    
    public int getLength() {
        return length;
    }
    
    public String getMesage() {
        return mesage;
    }
}
