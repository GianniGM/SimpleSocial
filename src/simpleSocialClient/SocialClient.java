package simpleSocialClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.parser.ParseException;


import simpleSocialServer.ContentsServer;
import simpleSocialServer.User;
import simpleSocialShared.Operations;
import simpleSocialShared.SocialMessage;

public class SocialClient {
	private static UUID id;
	private static User me = null;


	private static String IP_ADDRESS = "localhost";
	private static String MULTICAST_GROUP = "225.3.0.1";
	private static int PORT = 2000;
	private static int MULTICAST_PORT = 3000;
	private static int RMI_PORT = 4444;

	public static boolean logged = false;
	private static final int THREADS = 2;




	public static void main(String[] args) {

		//load configurations from ./confClient file
		loadConfigurations();

		//sending welcome message
		System.out.println(splashScreen());

		//creating threadpool
		ExecutorService es = Executors.newFixedThreadPool(THREADS);

		//Rmi remote object server
		ContentsServer server = null;

		//rmi clent class
		ClientRMI clientRMI = null;

		//multicast socket for keepAlive
		MulticastSocket multicastClient = null;

		//cointains not viewed contents
		ArrayList<String> localContents = null;

		if(args.length != 3){
			System.out.println("please type:\n [register | login] [username] [password]  ");
			return;
		}


		//RMIServer connection
		try {
			server = (ContentsServer) LocateRegistry.getRegistry(IP_ADDRESS, RMI_PORT).lookup(ContentsServer.OBJECT_NAME);
		} catch (IOException e1) {
			System.err.println("Connection refused: Not connected");
			return;
		} catch (NotBoundException e) {
			System.err.println("Connection refused: Not connected");
			return;
		}


		//messages used for comunication with server
		SocialMessage request = new SocialMessage();
		SocialMessage response = new SocialMessage();

		//reading command login, register or exit
		try(Socket client = new Socket(IP_ADDRESS, PORT);
				BufferedWriter writerTCP = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-16"));
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-16"))){


			//NOTE PHASE ONE: registration XOR login
			//if registration closing program
			//if login i can access to Socuial functions
			
			switch(args[0]){
			case "register":

				request.createMessage(Operations.REG_USER, args[1], args[2]);

				//send and receive TCP Register connection...
				writerTCP.write(request.toJSONString());
				writerTCP.newLine();
				writerTCP.flush();


				try {
					response.parseMessage(reader.readLine().trim());
				} catch (ParseException e1) {
					System.out.println("Received corrupted message");
				}

				System.out.println(new StringBuilder().append("Server: ").append(response.getParameters()[0]));
				
				//... then close the program
				return;

			case "login":

				if(id != null){
					System.out.println("Already online");
					break;
				}

				if(args.length != 3){
					System.err.println("Please, type: \"login [username] [password]\"");
					break;
				}

				//randomize a port
				Integer port = new Random(System.currentTimeMillis()).nextInt(65000) ;
				request.createMessage(Operations.LOGIN_USER, args[1], args[2], IP_ADDRESS, port.toString());

				//Sending login message
				writerTCP.write(request.toJSONString());
				writerTCP.newLine();
				writerTCP.flush();

				try {
					response.parseMessage(reader.readLine().trim());
				} catch (ParseException e1) {
					System.out.println("Received corrupted message");
					return;
				}

				if(response.getOpCode() == Operations.OK){
					id = UUID.fromString(response.getParameters()[0]);


					//synchronize local information with server
					request.createMessage(Operations.SYNC_USER, id.toString());

					writerTCP.write(request.toJSONString());
					writerTCP.newLine();
					writerTCP.flush();

					try {
						response.parseMessage(reader.readLine().trim());
					} catch (ParseException e) {
						System.out.println("Received corrupted message");
					}


					if(response.getOpCode() == Operations.OK){
						//load saveed local contents
						localContents = loadLocalContents(args[1]);

						try {
							me = new User(response.getParameters()[0]);
						} catch (ParseException e) {
							System.out.println("Received corrupted message");
						}

						//merge local contents with remote.
						for (String string : localContents) {
							me.addContents(string, false);
						}

					}else if(response.getOpCode() == Operations.USER_NOT_FOUND_ERROR){

						//the user received an empty json
						me = new User(args[1], args[2]);

					}else{

						System.out.println(response.getParameters()[0]);
						return;
					}

					//setting local address and port
					me.setAddress(new InetSocketAddress(IP_ADDRESS, port));

					System.out.println("logged in...");							
					logged = true;
				}else{
					System.out.println("please register you account first");
					return;
				}
				break;

			default:
				System.out.println("please type \"register\", \"login\"");
				return;

			}

			client.close();
		} catch (UnknownHostException e2) {
			System.err.println("Unwknow Host");
			return;
		} catch (IOException e2) {
			System.err.println("connection refused");
			return;
		}


		//NOTE PHASE 2: FROM HERE CLIENT IS ONLINE
		//DEBUG System.out.println("logged in! - id received:" + id);

		try(BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));){

			//parameters i receive with localReader
			String[] params;

			//welcome message
			System.out.println(new StringBuilder().append("Welcome back ").append(me.getUsername()));

			//creating keepAlive Client
			multicastClient = new MulticastSocket(MULTICAST_PORT);
			KeepAlive keepAlive = new KeepAlive(id, me.getUsername(), IP_ADDRESS ,multicastClient, MULTICAST_GROUP, PORT);

			//registration of callback
			clientRMI = new ClientRMI(me);
			me.callback = (ContentClient)UnicastRemoteObject.exportObject(clientRMI, 0);
			server.registerCallback(id, me.callback);

			//creating notifier
			Listener listener = new Listener(me.getAddress());

			es.submit(keepAlive);
			es.submit(listener);


			//the user logged in, now have access to the social functions
			boolean exit = false;
			while(!exit){

				//TODO lista di comandi da stampare

				try{
					params = localReader.readLine().split(" ");

					String command = params[0];

					switch (command.toLowerCase().trim()) {

					case "help":
						System.out.println("*******************Simple**********************\n"
								+ "Type:\n\"find [username]\": find username.\n"
								+ "\"friend [username]\": send friend request.\n"
								+ "\"friends\": friends online/offline list.\n"
								+ "\"getreq\": see your received friend request.\n"
								+ "\"accept [username]\": accept a friend request.\n"
								+ "\"unfriend [username]\": remove a friend request or a friendship.\n"
								+ "\"follow [username]\": follow a friend.\n"
								+ "\"send -> [ENTER] -> [type message]\": send a content.\n"
								+ "\"logout\": logout and closing this program\n"
								+ "*******************Social**********************");
						break;

					case "find":
						String searchString = "";
						if(params.length == 1){
							searchString = "***";
						}else{
							searchString = params[1];
						}

						String sid;

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}

						request.createMessage(Operations.SEARCH, sid, searchString);
						response = SendToServer(request);

						StringBuilder sb = new StringBuilder().append("Server:\n");
						for (String string : response.getParameters()) {
							sb.append(string).append("\n");
						}
						System.out.println(sb.toString());

						break;

					case "friend":

						searchString = params[1];

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}

						if(!me.getFriends().contains(params[1])){

							request.createMessage(Operations.FRIEND_REQ, sid, searchString);
							response = SendToServer(request);

							sb = new StringBuilder().append("Server:\n");
							for (String string : response.getParameters()) {
								sb.append(string).append("\n");
							}
							System.out.println(sb.toString());
						}else{
							System.out.println("you are already friend");
						}

						break;

					case "friends":

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}

						request.createMessage(Operations.FRIENDS_LIST, sid);
						System.out.println(printFriends(request));





						break;

					case "getreq":

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}


						request.createMessage(Operations.GET_FRIENDS_REQS, sid);
						response = SendToServer(request);

						sb = new StringBuilder().append("Server:\n");
						for (String string : response.getParameters()) {
							sb.append(string).append("\n");
						}
						System.out.println(sb.toString());


						break;	


					case "accept":
						//						System.out.println("Type name of name you want accept");
						//						searchString = localReader.readLine();
						searchString = params[1];

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}

						if(!me.getFriends().contains(params[1])){

							request.createMessage(Operations.FRIEND_ACCPT, sid, searchString);
							response = SendToServer(request);

							sb = new StringBuilder().append("Server:\n");
							for (String string : response.getParameters()) {
								sb.append(string).append("\n");
							}
							System.out.println(sb.toString());

						}else{
							System.out.println("already accepted");
						}
						break;

					case "unfriend":
						searchString = params[1];

						if(id == null){
							sid = "NOTLOGGED";
						}else{
							sid = id.toString();
						}

						if(me.getFriends().contains(params[1])){

							request.createMessage(Operations.FRIEND_DENY, sid, searchString);
							response = SendToServer(request);

							sb = new StringBuilder().append("Server:\n");
							for (String string : response.getParameters()) {
								sb.append(string).append("\n");
							}
							System.out.println(sb.toString());

						}else{
							System.out.println("is not your friend");
						}

						break;
					case "logout":
						request.createMessage(Operations.LOGOUT_USER, id.toString());
						response = SendToServer(request);

						sb = new StringBuilder().append("Server:\n");
						for (String string : response.getParameters()) {
							sb.append(string).append("\n");
						}
						System.out.println(sb.toString());

						try{
							server.exit(id);
						}catch(NullPointerException e){
							System.out.println("already logged out");
						}finally{
							exit = true;
						}

						break;

						//NOTE RMI REQUESTS FROM HERE
					case "follow":
						searchString = params[1];

						if(me.callback != null){
							if(server.follow(id, searchString)){
								System.out.println(searchString + " followed successfully");
							}else{
								System.out.println("can't follow, send a friend request first");
							}
						}else{
							System.out.println("please login first callback not registered");
						}
						break;

					case "send":
						System.out.println("Type content to send");
						searchString = localReader.readLine();

						if(me.callback != null){
							if(server.sendContent(id, searchString)){
								System.out.println(searchString + " followed successfully");
							}
						}else{
							System.out.println("please login first callback not registered");
						}

						System.out.println("Done :)");
						break;

						//FORMATTARE COME SI DEVE CON NOME UTENTE
					case "contents":
						ArrayList<String> contents = me.getReceivedContents();
						if(!contents.isEmpty())
							for (String string : contents) {
								System.out.println(string);
							}
						else
							System.out.println("empty :(");
						break;


					default:
						System.out.println("Uncorrected command! type \"help\" for further informations");
						break;
					}

				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println("Server error, cant receive requests");
				}
			}

			System.out.print("await closing...");
			localReader.close();
		} catch (IOException e1) {
			System.err.println("Connection Refused");
		}finally{
			if(me != null)
				saveLocalContents(me.getReceivedContents(), me.getUsername());

			try {
				es.shutdown();
				Listener.EXIT = true;
				multicastClient.close();
				UnicastRemoteObject.unexportObject(clientRMI, true);
				if(!es.awaitTermination(3, TimeUnit.SECONDS))
					es.shutdownNow();

			} catch (IOException e) {} catch (NullPointerException e){} catch (InterruptedException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}
		System.out.println(" Bye! ");
	}

	//save received contents locally
	private static void saveLocalContents(ArrayList<String> contents, String name){
		try(BufferedWriter fileWriter = new BufferedWriter(new FileWriter("./"+name, false))){

			for (String string : contents) {

				fileWriter.write(string);
				fileWriter.newLine();
			}
			fileWriter.flush();
			fileWriter.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//if i send a friend list request i must parse a more complicated message
	//so i must created a special function for print friend list and its status
	private static String printFriends(SocialMessage request) {
		SocialMessage response = new SocialMessage();
		StringBuilder sb = new StringBuilder();
		String responseMessage = "No friends in the list";

		try(Socket client = new Socket(IP_ADDRESS, PORT);
				BufferedWriter writerTCP = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-16"));
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-16"))){

			writerTCP.write(request.toJSONString());
			writerTCP.newLine();
			writerTCP.flush();

			String line = reader.readLine();

			if(line != null ){
				response.parseMessage(line.trim());
				if(response.getOpCode() == Operations.OK){
					sb = new StringBuilder();

					HashMap<String, Boolean> friends = new HashMap<>();
					friends = SocialMessage.parseFriends(response.getParameters()[0]);

					sb = new StringBuilder();
					sb.append("Friends list:\n");
					for(String s : friends.keySet()){
						sb.append(s).append(" is ");
						if(friends.get(s)){
							sb.append("online");
						}else{
							sb.append("offline");
						}
						sb.append("\n");
					}
					responseMessage =  sb.toString();
				}

			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.err.println("Unknown Host");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Server: connection refused!");
		} catch (ParseException e) {
			System.err.println("Error: malformed message");
		}

		return responseMessage;
	}


	//ONE SHOT: open a tcp connection, send request and close
	private static SocialMessage SendToServer(SocialMessage request) {

		SocialMessage response = new SocialMessage();

		try(Socket client = new Socket(IP_ADDRESS, PORT);
				BufferedWriter writerTCP = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-16"));
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-16"))){

			//send request
			writerTCP.write(request.toJSONString());
			writerTCP.newLine();
			writerTCP.flush();

			
			//read line
			String line = reader.readLine();

			//sending an ACK message
			request.createMessage(Operations.OK, "done");
			writerTCP.write(request.toJSONString());
			writerTCP.newLine();
			writerTCP.flush();

			client.close();
			writerTCP.close();
			reader.close();

			if(line != null ){
				response.parseMessage(line.trim());
			}else{
				response = null;
			}

		} catch (UnknownHostException e) {
			response = null;
			e.printStackTrace();
			System.err.println("Unknown Host");
		} catch (IOException e) {
			response = null;
			e.printStackTrace();
			System.err.println("Server: connection refused!");
		} catch (ParseException e) {
			response = null;
			System.err.println("Error: malformed message");
		}

		return response;
	}

	//load local contents into a file
	private static ArrayList<String> loadLocalContents(String name){
		ArrayList<String> localContents = new ArrayList<>();

		try(BufferedReader reader = new BufferedReader(new FileReader("./" + name))){
			String line;
			while((line = reader.readLine()) != null){
				localContents.add(line);
			}
			reader.close();

		}catch(IOException e){
			System.out.println("first login on this computer!");
		}

		return localContents;
	}

	//load a file "confClient" containing all network settings
	private static void loadConfigurations(){
		try(BufferedReader reader = new BufferedReader(new FileReader("./confClient"))){
			String[] values;
			String line;

			while((line = reader.readLine()) != null){
				values = line.trim().split(":");
				if (values.length == 2) {
					switch (values[0].trim()) {
					case "IP_ADDRESS":
						IP_ADDRESS = values[1].trim();
						break;

					case "IP_MULTICAST_GROUP":
						MULTICAST_GROUP = values[1].trim();
						break;

					case "TCP_PORT":
						PORT = Integer.parseInt(values[1].trim());
						break;

					case "MULTICAST_PORT":
						MULTICAST_PORT = Integer.parseInt(values[1].trim());
						break;

					case "RMI_PORT":
						RMI_PORT = Integer.parseInt(values[1].trim());
						break;


					}
				}
			}
		} catch (IOException e) {
			System.out.println("configuration file not found! Set Default values");
		}
	}
	
	private static String splashScreen() {
		StringBuilder sb = new StringBuilder();
		sb.append(" ______    ____    ___     _  _____    ____     ______ \n|   ___|  |    |  |    \\  / ||     |  |    |   |   ___|\n `-.`-.   |    |  |     \\/  ||    _|  |    |_  |   ___|\n|______|  |____|  |__/\\__/|_||___|    |______| |______|");
		sb.append("\n");
		sb.append(" ______   _____    ______    ____    ____     ____   \n|   ___| /     \\  |   ___|  |    |  |    \\   |    |  \n `-.`-.  |     |  |   |__   |    |  |     \\  |    |_ \n|______| \\_____/  |______|  |____|  |__|\\__\\ |______|\n");
		return sb.toString();
	}

}
