package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import org.json.simple.parser.ParseException;

import simpleSocialServer.SocialServer;
import simpleSocialShared.Operations;
import simpleSocialShared.SocialMessage;

public class KeepAliveTest {

	//TODO sistemare e migliorare sta cosa degli indirizzi
	//in un file conf dove si mette porta e indirizzi

	private static final int MAX_LENGTH = 512;
	private static InetAddress address;

	public static void main(String[] args) {
		UUID id = null;
		
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}
		
		
		try(Socket client = new Socket(address, SocialServer.PORT);
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-16"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-16"));
				BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in))){
			SocialMessage request = new SocialMessage();
			SocialMessage response = new SocialMessage();

			
			request.createMessage(Operations.LOGIN_USER, args[0], "scaccia");
			writer.write(request.toJSONString());
			writer.newLine();
			writer.flush();

			response.parseMessage(reader.readLine());
			if(response.getOpCode() != Operations.OK){
				request.createMessage(Operations.REG_USER, args[0], "gianni");
				writer.write(request.toJSONString());
				writer.newLine();
				writer.flush();

				response.parseMessage(reader.readLine());
				if(response.getOpCode() == Operations.OK){
					System.out.println("ok");
				}else{
					System.out.println("KOOOOO");
				}

				request.createMessage(Operations.LOGIN_USER, args[0], "gianni");
				writer.write(request.toJSONString());
				writer.newLine();
				writer.flush();

				response.parseMessage(reader.readLine());
				if(response.getOpCode() == Operations.OK){
					System.out.println("ok");
				}else{
					System.out.println("KOOOOO");
				}

			}else{
				System.out.println("Already logged");
			}

			
			id = UUID.fromString(response.getParameters()[0]);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		/**********************************************************************************************************************/
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
	
			DatagramPacket packet = new DatagramPacket(new byte[MAX_LENGTH],MAX_LENGTH );
			while(true){
				//receiveng packet from keep alive Server
				multicastClient.receive(packet);
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
				
				System.out.println("received " +  in.readUTF());

				//sending packet to keep alive server through udpSender Socket
				out.writeUTF(id.toString());
				byte[]data = byteOStream.toByteArray();
				packet = new DatagramPacket(data, data.length, address ,SocialServer.PORT);
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
