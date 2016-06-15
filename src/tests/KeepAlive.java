package tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.UUID;

import simpleSocialServer.KeepAliveServer;
import simpleSocialServer.SocialServer;

public class KeepAlive implements Runnable {

	private UUID id;
	private InetAddress address;

	public KeepAlive(UUID id,InetAddress address) {
		this.id = id;
		this.address = address;
	}
	
	@Override
	public void run() {
		ByteArrayOutputStream byteOStream= new ByteArrayOutputStream();
		DataOutputStream out= new DataOutputStream(byteOStream);

		System.out.println("TCP: id assigned " + id.toString());

		try(MulticastSocket multicastClient = new MulticastSocket(SocialServer.MULTICAST_PORT);
				DatagramSocket udpSender = new DatagramSocket()){

			InetAddress multicastGroup = InetAddress.getByName(SocialServer.MULTICAST_GROUP);

			int timeout = 20000;
			multicastClient.setSoTimeout(timeout);
			multicastClient.setReuseAddress(true);
			multicastClient.joinGroup(multicastGroup);

			udpSender.setReuseAddress(true);

			DatagramPacket packet = new DatagramPacket(new byte[KeepAliveServer.MAX_LENGTH],KeepAliveServer.MAX_LENGTH );
			while(true){
				//receiveng packet from keep alive Server
				multicastClient.receive(packet);
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));

				System.out.println("received " +  in.readUTF());

				//sending packet to keep alive server through udpSender Socket
				out.writeUTF(id.toString());
				
				byte[]data = byteOStream.toByteArray();
				packet = new DatagramPacket(data, data.length, address ,SocialServer.PORT);
				//FIXME null pointer exception
				udpSender.send(packet);

				in.close();
			}

		} catch (IOException e) {
			System.err.println("Keep Alive: Connection refused");
			e.printStackTrace();
		}finally{
			try {
				byteOStream.close();
				out.close();
			} catch (IOException e) {			}
		}
	}
}

