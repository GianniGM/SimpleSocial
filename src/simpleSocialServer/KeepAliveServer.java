package simpleSocialServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


import simpleSocialShared.SimpleSocialException;

import java.util.Collections;

public class KeepAliveServer implements Runnable{

	//TODO USARE METODI ALTERNATIVI ALLA SLEEP, PER ESEMPIO RIVEDERE LA SLIDE
	//SULLO SCHEDULING DEI THREADS

	private Users users;
	private static final int timeout = 7000;//10000
	public static final int MAX_LENGTH = 512;
	private static final long LIMIT_LOGIN_HOURS = 24;
	private long friendLimitMinutes = 60;

	public KeepAliveServer(Users s, long friendLimitMinutes) {
		this.users = s;
		this.friendLimitMinutes = friendLimitMinutes;
		//TODO SETTARE I MINUTI
	}

	@Override
	public void run() {

		ByteArrayOutputStream byteOStream = null;
		DataOutputStream out = null;
		//autcloseable
		try(MulticastSocket multicastSocket = new MulticastSocket(SocialServer.MULTICAST_PORT);
				DatagramSocket udpReceiver = new DatagramSocket(SocialServer.PORT)){

			multicastSocket.setTimeToLive(1);
			multicastSocket.setLoopbackMode(false);
			multicastSocket.setReuseAddress(true);
			multicastSocket.setSoTimeout(timeout);//NEW
			
			udpReceiver.setSoTimeout(timeout);
			udpReceiver.setReuseAddress(true);

			InetAddress multicastGroup = InetAddress.getByName(SocialServer.MULTICAST_GROUP);

			Set<UUID> list = Collections.synchronizedSet(new HashSet<>()); 
			Set<UUID> online = Collections.synchronizedSet(new HashSet<>()); 

			
			while(true){
				
				for (UUID uuid : users.getOnlineUsers().keySet()) {
					online.add(uuid);
				}
				
				try{
					//TODO da sistemare
					Thread.sleep(timeout);

					byteOStream = new ByteArrayOutputStream();
					out = new DataOutputStream(byteOStream);
					out.writeUTF("AWAKE_SIGNAL");				

					byte[]data = byteOStream.toByteArray();

					DatagramPacket packet = new DatagramPacket(data, data.length, multicastGroup, SocialServer.MULTICAST_PORT);
					multicastSocket.send(packet);

					System.out.println("Sended awake signal");

					//waiting the N users already logged in plus one in case of logging in the meantime
					int maxTimeOut = 1;
					while(maxTimeOut > 0){
						try {

							packet = new DatagramPacket(new byte[MAX_LENGTH], MAX_LENGTH);	
							udpReceiver.receive(packet);
							DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));

							String name = in.readUTF();
							UUID id = UUID.fromString(in.readUTF());
							System.out.println("RECEIVED " + id + " " + name);
							list.add(id);

							in.close();
						} catch (SocketTimeoutException e ) {
							System.out.println("time out!");
							maxTimeOut--;
						} catch (IOException e){
							e.printStackTrace();
							System.out.println("error" + e.getMessage());
							maxTimeOut--;
						}
					}

					System.out.println("logged users: " + online.toString());
					System.out.println("users still online: " + list.toString());
					
					for(UUID  u : online){
						//TODO IL GETUSER QUI POTREBBE CREARE CASINI
						
						User user = users.getUser(u);
						
						if(user != null && user.checkExpiration(LIMIT_LOGIN_HOURS, friendLimitMinutes)){
							try {
								users.logOff(u);
							} catch (SimpleSocialException e) {
							}
						}else if (!list.contains(u)){
							try {
								System.out.println("loggin off " + u);
								users.logOff(u);
							} catch (SimpleSocialException e) {
								System.out.println("user already logged off");
							}
						}
					}
					list.clear();
					online.clear();

					
				} catch (InterruptedException e1) {
					System.err.println("Interrupted...");
				} catch (IOException e1){
					System.err.println("connection refused");
				} catch(NullPointerException e1){
					System.out.println("!!!!!!!!!!!ERRORE!!!!! " + e1.getMessage());
					e1.printStackTrace();
				}
			}
			
		} catch (IOException e) {			
			System.err.println("KeepAlive: Connecton refused!");
			e.printStackTrace();
		}finally{
			System.out.println("KEEP ALIVE SHUTTED DOWN");
			try {
				byteOStream.close();
				out.close();
			} catch (IOException e) {	}
		}
	}
}