package basic;

/**
 * Created by Shobhit on 14/4/2018.
 */

public enum Packet {
    HELLO_SERVER(0);

    public int value;

    Packet(int i) {
        this.value = i;
    }

    public int getValue() {
        return value;
    }
}
