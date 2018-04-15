package basic;

/**
 * Created by Shobhit on 15/4/2018.
 */

public class Strings {
    public static final String HELLO_MESSAGE = "This is a hello message";
    public static final String WELCOME_MESSAGE = "This is the welcome message. Hi, I'm SecStore!!";
    public static final String CA_REQUEST = "Requesting certificate signed by certificate authority.";
    private static final Strings ourInstance = new Strings();
    
    public static Strings getInstance() {
        return ourInstance;
    }
    
    private Strings() {
    }
}
