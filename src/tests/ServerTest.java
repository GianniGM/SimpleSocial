package tests;

import java.io.BufferedReader;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import simpleSocialServer.SocialServer;
import simpleSocialShared.SocialMessage;
import simpleSocialShared.Operations;

public class ServerTest {
	private static UUID id;

	public static void main(String[] args) {

		ExecutorService es = Executors.newFixedThreadPool(1);
		
		
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
							for (String string : response.getParameters()) {
								System.out.println(string);								
							}
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

							break;

						case "search":
							System.out.println("Who you wanna search?");
							String searchString = localReader.readLine();
							String sid;
							
							if(id == null){
								sid = "NOTLOGGED";
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
