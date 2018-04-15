import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import basic.Packet;
import basic.PacketObj;
import basic.Strings;


public class ClientWithSecurity {

	private static X509Certificate serverCert;

	private static void AP(ObjectOutputStream toServer, ObjectInputStream fromServer) {
		try {
			/*
			 * Serialized packet obj
			 * HELLO PACKET
			 * */
			System.out.println("Sending hello message to server");
			byte[] hello_message = Strings.HELLO_MESSAGE.getBytes("UTF-8");
			PacketObj hiPacket = new PacketObj(Packet.HELLO_SERVER, hello_message.length, hello_message);
			toServer.writeObject(hiPacket);
			toServer.flush();

			PacketObj WelcomePacket = (PacketObj) fromServer.readObject();
			System.out.println("Received Reply with welcome message signed with private key");

			System.out.println("Requesting CA signed certificate of Secstore");
			byte[] carequest = Strings.CA_REQUEST.getBytes("UTF-8");
			PacketObj RequestCA = new PacketObj(Packet.REQ_CA_CERT, carequest.length, carequest);
			/*

			int certLength = fromServer.readInt();
			byte[] cert = new byte[certLength];
			fromServer.read(cert);

			int read = fromServer.readInt();
			int offset = 0;

			while (read == 1) {
				int length = fromServer.readInt();
				if (length == -1)
					break;
				byte[] block = new byte[length];
				fromServer.read(block);
				for (int i = 0; i < length; i++) {
					cert[offset + i] = block[i];
				}
				offset += length;
				read = fromServer.readInt();
			}

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));

			System.out.println("Recieved server certificate");
			System.out.println("Verifying...");
			X509Certificate CAcert = (X509Certificate) cf.generateCertificate(new FileInputStream("CA.crt"));
			PublicKey key = CAcert.getPublicKey();

			serverCert.checkValidity();
			serverCert.verify(key);

			System.out.println("Verified server certificate!");
			System.out.println("Verifying welcome message...");

			Signature dsa = Signature.getInstance("SHA1withRSA", "SunJSSE");
			dsa.initVerify(serverCert.getPublicKey());
			dsa.update("Hello, this is SecStore!".getBytes("UTF-8"));*/
			/*
			if (!dsa.verify(replyPacket.getMessage().getBytes())) {
				System.err.println("Verification failed! Bye!");
				System.exit(-1);
			}
*/
			System.out.println("Verified welcome message!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
/*
	private static void sendCP1 (String filename, ObjectOutputStream toServer){
		try {
			FileInputStream fileInputStream;
			BufferedInputStream bufferedFileInputStream;

			int numBytes;

			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Open the file
			File file = new File(filename);
			if (!file.exists()) {
				System.err.println("File has problem");
				System.exit(-1);
			}
			if (file.length() == 0) {
				System.err.println("Empty file");
				System.exit(-1);
			}

			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);
			byte[] fromFileBuffer = new byte[117];

			// Send the file
			int count = 0;
			for (boolean fileEnded = false; !fileEnded; ) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < 117;

				byte[] encryptedBytes = encrypt(fromFileBuffer);
				if (encryptedBytes != null) {
					toServer.writeInt(1);
					toServer.writeInt(numBytes);
					toServer.writeInt(encryptedBytes.length);
					toServer.write(encryptedBytes, 0, encryptedBytes.length);
					toServer.flush();
					count++;
				}
			}

			System.out.println("Sent " + count + " blocks");
			bufferedFileInputStream.close();
			fileInputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static byte[] encrypt(byte[] plaintext) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, serverCert.getPublicKey());
			return cipher.doFinal(plaintext);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] decrypt(byte[] ciphertext) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, serverCert.getPublicKey());
			return cipher.doFinal(ciphertext);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
*/
	public static void main(String[] args) {
		String filename = "rr.txt";
		if (args.length > 0) filename = args[0];

		String serverAddress = "localhost";
		if (args.length > 1) filename = args[1];

		int port = 4000;
		if (args.length > 2) port = Integer.parseInt(args[2]);

		Socket clientSocket;

		ObjectOutputStream toServer;
		ObjectInputStream fromServer;

		long timeStarted = System.nanoTime();

		try {
			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new ObjectOutputStream(clientSocket.getOutputStream());
			fromServer = new ObjectInputStream(clientSocket.getInputStream());

			System.out.println("Establishing connection to server...");

			AP(toServer, fromServer);

			System.out.println("Sending file...");

			//sendCP1(filename, toServer);

			System.out.println("Closing connection...");
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken / 1000000.0 + "ms to run");
	}
}