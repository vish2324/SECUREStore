package basic;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.Serializable;

/**
 * Created by Shobhit on 14/4/2018.
 */

public class PacketObj implements Serializable{
    Packet type;
    int length;
    byte[] message;

    public PacketObj(Packet type, int length, byte[] message) {
        this.type = type;
        this.length = length;
        this.message = message;
    }
    
    public Packet getType() {
        return type;
    }
    
    public int getLength() {
        return length;
    }
    
    public byte[] getMessage() {
        return message;
    }
}
