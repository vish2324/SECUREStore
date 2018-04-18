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

public class ClientWithSecurity {

	private static X509Certificate serverCert;
	private static Socket clientSocket;

	public static void main(String[] args) {
		String filename = "rr.txt";
		if (args.length > 0) filename = args[0];

		String serverAddress = "localhost";
		if (args.length > 1) filename = args[1];

		int port = 4600;
		if (args.length > 2) port = Integer.parseInt(args[2]);

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
			//start file transfer to server
			sendCP1(filename, toServer);

			System.out.println("Closing connection...");
			toServer.writeObject(new PacketObj(Packet.END, 0, null));
			toServer.flush();

			clientSocket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken / 1000000.0 + "ms to run");
	}

	private static void AP(ObjectOutputStream toServer, ObjectInputStream fromServer) {
		Object object;

		PacketObj WelcomePacket = null;
		Packet P = null;
		int L = 0;
		byte[] A = null;

		try {
			System.out.println("\nSending hello message to server");
			byte[] hello_message = Strings.HELLO_MESSAGE.getBytes("UTF-8");
			PacketObj hiPacket = new PacketObj(Packet.HELLO_SERVER, hello_message.length, hello_message);
			toServer.writeObject(hiPacket);
			toServer.flush();

			System.out.println("Waiting for reply from server");
			//hello message reply
			object = fromServer.readObject();
			if (object instanceof PacketObj) {
				WelcomePacket = (PacketObj) object;
			}
			System.out.println("\nReceived Reply with welcome message signed with private key");

			System.out.println("Requesting CA signed certificate of SecStore");

			byte[] carequest = Strings.CA_REQUEST.getBytes("UTF-8");
			PacketObj RequestCA = new PacketObj(Packet.REQ_CA_CERT, carequest.length, carequest);
			toServer.writeObject(RequestCA);
			toServer.flush();

			System.out.println("\nReceiving servercert");
			object = fromServer.readObject();
			if (object instanceof PacketObj) {
				PacketObj servercertObj = (PacketObj) object;
				P = servercertObj.getType();
				L = servercertObj.getLength();
				A = servercertObj.getMessage();
			}

			System.out.println("Now verifying certificate");
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(A));
			X509Certificate CAcert = (X509Certificate) cf.generateCertificate(new FileInputStream("CA.crt"));
			PublicKey CApubkey = CAcert.getPublicKey();

			serverCert.checkValidity();
			serverCert.verify(CApubkey);

			System.out.println("Verified that the server certificate is indeed authorized by the CA");

			System.out.println("decrypting Welcome message using the public key");

			A = WelcomePacket.getMessage();

			Signature signer = Signature.getInstance("SHA1withRSA");

			signer.initVerify(serverCert.getPublicKey());

			signer.update(Strings.WELCOME_MESSAGE.getBytes("UTF-8"));

			if (signer.verify(A)){
				System.out.println("Verified welcome message!");
			} else {
				System.out.println("Welcome message unverifiable");
				clientSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendCP1 (String filename, ObjectOutputStream fileToServer){
		try {
			PacketObj fileObj;
			fileObj = new PacketObj( Packet.FILE_NAME, filename.getBytes("UTF-8").length, filename.getBytes("UTF-8"));
			fileToServer.writeObject(fileObj);
			fileToServer.flush();

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

			FileInputStream fileInputStream = new FileInputStream(filename);
			BufferedInputStream bufferedFileInputStream = new BufferedInputStream(fileInputStream);
			byte[] fromFileBuffer = new byte[117];
			int numBytes;

			// Send the file
			int count = 0;
			for (boolean fileEnded = false; !fileEnded; ) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < fromFileBuffer.length;

				byte[] encryptedBytes = encrypt(fromFileBuffer);

				fileObj = new PacketObj( Packet.FILE_BLOCK , numBytes, encryptedBytes);
				fileToServer.writeObject(fileObj);
				fileToServer.flush();

				count++;
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
}
