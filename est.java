

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import basic.Packet;
import sun.security.rsa.RSASignature;
import sun.security.ssl.SunJSSE;

/**
 * Created by Shobhit on 14/4/2018.
 */

public class est {
    private static final String CA_CERT_PATH = "../CA.crt";
    public static void main(String args[]) throws Exception {
        String keyPath = "privateServer.der";
        File privKeyFile = new File(keyPath);
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(new FileInputStream(privKeyFile));
        } catch(FileNotFoundException e) {
            throw new Exception("Could not locate keyfile at '" + keyPath + "'", e);
        }
        byte[] privKeyBytes = new byte[(int)privKeyFile.length()];
        bis.read(privKeyBytes);
        bis.close();
        System.out.println(Arrays.toString(privKeyBytes));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(ks);
        Signature signature =Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        byte[] CDRIVES = hexStringToByteArray("e04fd020ea3a6910a2d808002b30309d");
        signature.update(CDRIVES);
    
        File cert = new File("server.crt");
        FileInputStream fileInputStream = new FileInputStream(cert);
        BufferedInputStream bis1 = new BufferedInputStream(fileInputStream);
        byte [] fromFileBuffer = new byte[(int) cert.length()];
        bis1.read(fromFileBuffer);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(fromFileBuffer));
    
        System.out.println("Received server certificate!");
        System.out.println("Verifying server certificate with " + CA_CERT_PATH + "...");
        X509Certificate CAcert = (X509Certificate) cf.generateCertificate(new FileInputStream("CA.crt"));
        PublicKey key = CAcert.getPublicKey();
    
        serverCert.checkValidity();
        serverCert.verify(key);
    
        System.out.println("Verified server certificate!");
        System.out.println("Verifying welcome message...");
        Signature dsa = Signature.getInstance("SHA1withRSA");
        dsa.initVerify(serverCert.getPublicKey());
        dsa.update(CDRIVES);
        if(!dsa.verify(signature.sign())) {
            System.err.println("Verification failed! Terminating file transfer");
            System.exit(-1);
        }
    
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    public static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }
    
}
