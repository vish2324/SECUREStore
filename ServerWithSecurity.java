import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
	
	public static int port = 4321;
	public static Signature signature;

	public static void main(String[] args) {
    	if (args.length > 0) port = Integer.parseInt(args[0]);
    	init();

		ServerSocket welcomeSocket = null;
		Handler handler;


		try {
			welcomeSocket = new ServerSocket(port);
			System.out.println("Server started on port: "+ port);
			while(true){
				System.out.println("Listening on main thread.....");
				handler = new Handler(welcomeSocket.accept(),signature );
				handler.start();
			}
			
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
			System.out.println(Arrays.toString(privKeyBytes));
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
		
		PacketObj packet;
		Object obj;
		Packet type;
		int length;
		byte[] message;

		ObjectOutputStream toClient = null;
		ObjectInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;

		public Handler(Socket socket, Signature signature) {
			this.socket = socket;
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
							if(Arrays.equals(message, Strings.HELLO_MESSAGE.getBytes("UTF-8"))) {
								System.out.println("Sending welcome message.......");
								signature.update(Strings.WELCOME_MESSAGE.getBytes("UTF-8"));
								byte[] welcome_message = signature.sign();
								toClient.writeObject(new PacketObj(Packet.WELCOME,welcome_message.length,welcome_message));
							}
							
						
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
			} catch (Exception e){e.printStackTrace();}
		}
	}

}
