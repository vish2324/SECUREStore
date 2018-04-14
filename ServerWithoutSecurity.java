import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import basic.PacketObj;
import basic.Packet;

import static basic.Packet.HELLO_SERVER;


public class ServerWithoutSecurity {

	public static void main(String[] args) {

    	int port = 4321;
    	if (args.length > 0) port = Integer.parseInt(args[0]);

		ServerSocket welcomeSocket = null;
		Handler handler;


		try {
			welcomeSocket = new ServerSocket(port);
			System.out.println("Server started on port: "+ port);
			while(true){
				System.out.println("Listening on main thread.....");
				handler = new Handler(welcomeSocket.accept());
				handler.start();
			}
			
		} catch (Exception e) {e.printStackTrace();}

	}

	private static class Handler extends Thread {
		Socket socket;
		
		PacketObj packet;
		Object obj;
		Packet type;
		int length;
		String message;

		ObjectOutputStream toClient = null;
		ObjectInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;

		public Handler(Socket socket) {
			this.socket = socket;
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
					message = packet.getMesage();
					
					switch(type) {
						case HELLO_SERVER:
							
						
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
