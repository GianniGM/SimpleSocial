package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import simpleSocialClient.ClientRMI;
import simpleSocialClient.ContentClient;
import simpleSocialServer.ContentsServer;
import simpleSocialServer.SocialServer;
import simpleSocialServer.User;
import simpleSocialShared.Operations;
import simpleSocialShared.SocialMessage;

public class SocialClientTest {
	private static UUID id;
	private static String address = "localhost";

	public static void main(String[] args) {


		ExecutorService es = Executors.newFixedThreadPool(1);
		User user;


		ContentsServer server = null;
		ContentClient callback = null;
		
		try {
			address = InetAddress.getLocalHost().getHostName();
			server = (ContentsServer) LocateRegistry.getRegistry(address, SocialServer.RMI_PORT).lookup(ContentsServer.OBJECT_NAME);
		} catch (RemoteException | NotBoundException e1) {
			e1.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}


		//TCPServer.address
		try(Socket client = new Socket(InetAddress.getLocalHost(), SocialServer.PORT);
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-16"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-16"));
				BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in))){
			
			boolean exit = false;
			SocialMessage request = new SocialMessage();
			SocialMessage response = new SocialMessage();
			while(!exit){
				try{
					System.out.println("TestServer please type a command");
					String[] input = localReader.readLine().split(" ");

					if(input[0].trim().toLowerCase().equals("exit")){
						exit = true;
					}else{
						String command = input[0];

						switch (command.toLowerCase().trim()) {
						case "register":
							System.out.println("insert your uname and password");
							input = localReader.readLine().split(" ");
							request.createMessage(Operations.REG_USER, input[0], input[1]);

							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());
							break;
						case "login":
							if(id != null){
								System.out.println("Already online");
								break;
							}
							
							System.out.println("insert your uname and password");
							input = localReader.readLine().split(" ");
							request.createMessage(Operations.LOGIN_USER, input[0], input[1]);
							
							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());

							id = UUID.fromString(response.getParameters()[0]);
							System.out.println(id);
							KeepAlive control = new KeepAlive(id, InetAddress.getLocalHost());
							es.submit(control);

							if(response.getOpCode() == Operations.OK){
								user = new User(input[0], input[1]);
								callback = (ContentClient)UnicastRemoteObject.exportObject(new ClientRMI(user), 0);
								System.out.println("registered");
							}else{
								//TODO CONTROLLARE TUTTO
								System.err.println("Error on registration");
							}
							
							break;

						case "search":
							System.out.println("Who you wanna search?");
							String searchString = localReader.readLine();
							String sid;

							if(id == null){
								sid = "NOTLOGGED";
								break;
							}else{
								sid = id.toString();
							}
							request.createMessage(Operations.SEARCH, sid, searchString);
							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());
							for (String string : response.getParameters()) {
								System.out.println(string);								
							}							
							break;

						case "friend":
							System.out.println("Type name you wanna friend");
							searchString = localReader.readLine();

							if(id == null){
								sid = "NOTLOGGED";
								break;
							}

							sid = id.toString();

							request.createMessage(Operations.FRIEND_REQ, sid, searchString);
							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());
							for (String string : response.getParameters()) {
								System.out.println(string);								
							}							
							break;

						case "friendls":

							if(id == null){
								sid = "NOTLOGGED";
								break;
							}
							sid = id.toString();

							request.createMessage(Operations.FRIENDS_LIST, sid);
							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());
							System.out.println(SocialMessage.parseFriends(response.getParameters()[0]));
							break;

						case "accept":
							System.out.println("Type name of name you want accept");
							searchString = localReader.readLine();

							if(id == null){
								sid = "NOTLOGGED";
								break;
							}else{
								sid = id.toString();
							}

							request.createMessage(Operations.FRIEND_ACCPT, sid, searchString);
							writer.write(request.toJSONString());
							writer.newLine();
							writer.flush();

							response.parseMessage(reader.readLine().trim());
							for (String string : response.getParameters()) {
								System.out.println(string);								
							}							
							break;

						case "follow":
							System.out.println("Type name of name you want accept");
							searchString = localReader.readLine();
							if(callback != null){
								if(server.follow(id, searchString)){
									System.out.println(searchString + " followed successfully");
								}
							}else{
								System.out.println("please login first callback not registered");
							}
							break;

						case "logout":

							break;

						default:
							break;
						}

					}
				}catch(org.json.simple.parser.ParseException e){
					e.printStackTrace();
				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println("Error: malformed message");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
