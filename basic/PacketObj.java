import java.io.Serializable;

/**
 * Created by Shobhit on 14/4/2018.
 */

public class PacketObj implements Serializable{
    int type;
    int length;
    String mesage;

    public PacketObj(int type, int length, String mesage) {
        this.type = type;
        this.length = length;
        this.mesage = mesage;
    }
}
