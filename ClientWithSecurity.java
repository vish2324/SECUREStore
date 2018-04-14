import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ClientWithSecurity {
	private static final String HI = "Hello SecStore, please prove your identity!";

	private static void AP(ObjectOutputStream toServer, ObjectInputStream fromServer) {
		try {
			System.out.println("Establishing connection to server...");

			/*
			 * Serialized packet obj
			 * HELLO PACKET
			 * */
			System.out.println("Sending hello message to server");
			PacketObj hiPacket = new PacketObj(Packet.HELLO_SERVER, HI.length(), HI);
			toServer.writeObject(hiPacket);
			toServer.flush();

			PacketObj replyPacket = (PacketObj) fromServer.readObject();
			System.out.println("Received Reply");

			System.out.println("Requesting CA signed certificate");
			toServer.writeInt(3);
			toServer.flush();

			int certLength = fromServer.readInt();
			byte[] cert = new byte[certLength];

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
			X509Certificate serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));

			System.out.println("Recieved server certificate");
			System.out.println("Verifying...");
			X509Certificate CAcert = (X509Certificate) cf.generateCertificate(new FileInputStream("CA.crt"));
			PublicKey key = CAcert.getPublicKey();

			System.out.println("Verified server certificate!");
			System.out.println("Verifying welcome message...");

			Signature dsa = Signature.getInstance("SHA1withRSA", "SunJSSE");
			dsa.initVerify(serverCert.getPublicKey());
			dsa.update("Hello, this is SecStore!".getBytes("UTF-8"));
			if (!dsa.verify(replyPacket.mesage.getBytes())) {
				System.err.println("Verification failed! Bye!");
				System.exit(-1);
			}

			System.out.println("Verified welcome message!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String filename = "rr.txt";
		if (args.length > 0) filename = args[0];

		String serverAddress = "localhost";
		if (args.length > 1) filename = args[1];

		int port = 4321;
		if (args.length > 2) port = Integer.parseInt(args[2]);

		int numBytes = 0;

		Socket clientSocket = null;

		ObjectOutputStream toServer = null;
		ObjectInputStream fromServer = null;

		FileInputStream fileInputStream = null;
		BufferedInputStream bufferedFileInputStream = null;

		long timeStarted = System.nanoTime();

		try {
			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new ObjectOutputStream(clientSocket.getOutputStream());
			fromServer = new ObjectInputStream(clientSocket.getInputStream());

			AP(toServer, fromServer);

			System.out.println("Sending file...");

			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);
			byte[] fromFileBuffer = new byte[117];

			// Send the file
			int count = 0;
			for (boolean fileEnded = false; !fileEnded; ) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < 117;

				toServer.writeInt(1);
				toServer.writeInt(numBytes);
				toServer.write(fromFileBuffer, 0, numBytes);
				toServer.flush();
				count++;
			}

			System.out.println("Sent " + count + " blocks");
			bufferedFileInputStream.close();
			fileInputStream.close();
			System.out.println("Closing connection...");
		} catch (Exception e) {
			e.printStackTrace();
		}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken / 1000000.0 + "ms to run");
	}
}
