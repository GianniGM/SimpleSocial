package simpleSocialClient;

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

public class KeepAlive implements Runnable {

	private UUID id;
	private String address;
	private String name;
	private MulticastSocket multicastClient;
	private String multicastAddressGroup;
	private int port;

	public KeepAlive(UUID id, String name, String address, MulticastSocket multicastClient, String multicastGroup, int port) {
		this.id = id;
		this.name = name;
		this.address = address;
		this.multicastClient = multicastClient;
		this.multicastAddressGroup = multicastGroup;
		this.port = port;
	}

	@Override
	public void run() {
		ByteArrayOutputStream byteOStream= new ByteArrayOutputStream();
		DataOutputStream out= new DataOutputStream(byteOStream);


		try(DatagramSocket udpSender = new DatagramSocket()){

			//multicast settings
			InetAddress multicastGroup = InetAddress.getByName(multicastAddressGroup);

			multicastClient.setReuseAddress(true);
			multicastClient.joinGroup(multicastGroup);

			udpSender.setReuseAddress(true);

			DatagramPacket packet = new DatagramPacket(new byte[KeepAliveServer.MAX_LENGTH],KeepAliveServer.MAX_LENGTH );

			boolean exit = false;
			while(!exit){
				try{

					//receiving packet from keep alive Server
					multicastClient.receive(packet);
					DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));

					String s = in.readUTF();
					if(s.isEmpty()){
						System.err.println("problem with connection, your status is offline!");
					}
					//DEBUG System.out.println("received " +  s);

					//sending packet to keep alive server through udpSender Socket
					if(id != null){
						out.writeUTF(name);
						out.writeUTF(id.toString());
					}

					byte[]data = byteOStream.toByteArray();
					packet = new DatagramPacket(data, data.length, InetAddress.getByName(address) , port);

					udpSender.send(packet);
					in.close();
				}catch(IOException e){
					System.err.println("...your status is offline!");	
					exit = true;
				}
			}

			multicastClient.close();
			udpSender.close();

		} catch (IOException e) {
			System.err.println("Keep Alive: Connection refused");
		}finally{
			try {
				byteOStream.close();
				out.close();
			} catch (IOException e) {			}
		}
	}
}