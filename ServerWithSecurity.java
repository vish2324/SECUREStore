import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import basic.PacketObj;
import basic.Packet;
import basic.Strings;

import static basic.Packet.HELLO_SERVER;


public class ServerWithSecurity {
	
	public static int port = 4000;
	public static Signature signature;

	public static void main(String[] args) {
    	if (args.length > 0) port = Integer.parseInt(args[0]);
    	init();

		ServerSocket welcomeSocket = null;
		Handler handler;
		int client_no = 0;

		try {
			welcomeSocket = new ServerSocket(port);
			System.out.println("Server started on port: "+ port);
			while(true){
				System.out.println("Listening on main thread.....");
				handler = new Handler(welcomeSocket.accept(),signature , client_no);
				handler.start();
			}
			
		} catch (EOFException e) {
			System.out.println("Connection to client has ended on port " + port);
		} catch (Exception e) {e.printStackTrace();}

	}
	
	private static void init()  {
		try {
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
			System.out.println("Private key " + Arrays.toString(privKeyBytes));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
			PrivateKey privateKey = keyFactory.generatePrivate(ks);
			signature =Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			System.out.println("Completed initalization.....");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (Exception e)  {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}

	private static class Handler extends Thread {
		Socket socket;
		Signature signature;
		int client;
		
		PacketObj packet;
		Object obj;
		Packet type;
		int length;
		byte[] message;

		ObjectOutputStream toClient = null;
		ObjectInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;

		public Handler(Socket socket, Signature signature, int client) {
			this.socket = socket;
			this.client = client;
			this.signature = signature;
		}

		public void run() {
			try {
				fromClient = new ObjectInputStream(socket.getInputStream());
				toClient = new ObjectOutputStream(socket.getOutputStream());

				while (!socket.isClosed()) {
					
					obj = fromClient.readObject();
					if (obj instanceof PacketObj) {
						packet = (PacketObj) obj;
					}
					type = packet.getType();
					length = packet.getLength();
					message = packet.getMessage();
					
					switch(type) {
						case HELLO_SERVER:
							System.out.println("\nREceived hello message from client");
							if(Arrays.equals(message, Strings.HELLO_MESSAGE.getBytes("UTF-8"))) {
								System.out.println("Sending welcome message signed with private key.......");
								signature.update(Strings.WELCOME_MESSAGE.getBytes("UTF-8"));
								byte[] welcome_message = signature.sign();
								toClient.writeObject(new PacketObj(Packet.WELCOME,welcome_message.length,welcome_message));
								toClient.flush();
							}
							break;
						
						case REQ_CA_CERT:
							System.out.println("\nReceived request from client for certificate signed by CA");
							System.out.println("Responding appropriately.....");
							File cert = new File("server.crt");
							FileInputStream fileInputStream = new FileInputStream(cert);
							BufferedInputStream bis1 = new BufferedInputStream(fileInputStream);
							byte [] fromFileBuffer = new byte[(int) cert.length()];
							bis1.read(fromFileBuffer);
							toClient.writeObject(new PacketObj(Packet.SERVER_CERT, fromFileBuffer.length, fromFileBuffer));
							toClient.flush();
							System.out.println("Certificate has been sent!!");
							break;
					}
					
					/*
					int packetType = fromClient.readInt();

					// If the packet is for transferring the filename
					if (packetType == 0) {

						System.out.println("Receiving file...");

						int numBytes = fromClient.readInt();
						byte[] filename = new byte[numBytes];
						// Must use read fully!
						// See: https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
						fromClient.readFully(filename, 0, numBytes);

						fileOutputStream = new FileOutputStream("recv_" + new String(filename, 0, numBytes));
						bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

						// If the packet is for transferring a chunk of the file
					} else if (packetType == 1) {

						int numBytes = fromClient.readInt();
						byte[] block = new byte[numBytes];
						fromClient.readFully(block, 0, numBytes);

						if (numBytes > 0)
							bufferedFileOutputStream.write(block, 0, numBytes);

						if (numBytes < 117) {
							System.out.println("Closing connection...");

							if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
							if (bufferedFileOutputStream != null) fileOutputStream.close();
							fromClient.close();
							toClient.close();
							socket.close();
						}
					}
					*/
				}
			} catch (EOFException e) {
				System.out.println("unexpected EOF character. Session has ended for client "+client);
			}catch (Exception e){e.printStackTrace();}
		}
	}

}
