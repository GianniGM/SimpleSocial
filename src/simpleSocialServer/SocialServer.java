package simpleSocialServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.simple.parser.ParseException;


public class SocialServer {

	public static  int PORT = 2000;
	public static  int MULTICAST_PORT = 3000;
	public static  int RMI_PORT = 4444;
	public static long FRIEND_REQ_VAL = 60;
	public static  String MULTICAST_GROUP = "225.3.0.1";
	public static  String IP_ADDRESS = "localhost";
	private static long SAVE_TIME_INTERVAL = 60;

	private static final int SERVICE_THREADS = 3;

	private static InetAddress address = null;

	public static void main(String[] args) {
		loadConfigurations();
		
		Users s = SocialServer.loadState();
		ExecutorService es = Executors.newFixedThreadPool(SERVICE_THREADS);
		Registry registry = null;

		if(args.length == 1){
			FRIEND_REQ_VAL = Long.parseLong(args[0]);
		}


		try {
			address = InetAddress.getByName(IP_ADDRESS);
			
			System.out.println("connected on " + address.getHostAddress());
			
			TCPServer tcpServer = new TCPServer(s, address);
			KeepAliveServer multicastServer = new KeepAliveServer(s, FRIEND_REQ_VAL);
			registry =  LocateRegistry.createRegistry(SocialServer.RMI_PORT);
			ServerRMI serverRMI = new ServerRMI(s,registry);

			es.submit(tcpServer);
			es.submit(multicastServer);
			es.submit(serverRMI);

		} catch (UnknownHostException e) {
			System.err.println("Unknow host");
			return;

		} catch (RemoteException e) {
			System.err.println("RMI: connection refused");
		}

		System.out.println("Server Online!!!");
		es.shutdown();

		boolean exit = false;
		while(!exit){
			try {
				exit = es.awaitTermination(SAVE_TIME_INTERVAL, TimeUnit.SECONDS);
				System.out.println("Saving server state...");
				SocialServer.saveState(s);
				System.out.println("done");
			}
			catch (InterruptedException e) {
				SocialServer.saveState(s);
				System.out.println("Interrupted...");
				es.shutdownNow();
				try {
					registry.unbind(ContentsServer.OBJECT_NAME);
				} catch (RemoteException | NotBoundException e1) {
				}
			}
		}
	}

	public static void loadConfigurations(){

		try(BufferedReader reader = new BufferedReader(new FileReader("./conf"))){
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

					case "FRIEND_REQUEST_MIN":
						FRIEND_REQ_VAL = Long.parseLong(values[1].trim());
						break;

					case "SAVE_TIME_SECONDS":
						SAVE_TIME_INTERVAL = Long.parseLong(values[1].trim());

					}
				}

			}
			reader.close();

		}catch(IOException e){
			System.out.println("configuration file not found! Set Default values");
		}
	}

	public static Users loadState(){
		Users s = new Users();

		try(BufferedReader reader = new BufferedReader(new FileReader("./users"))){
			String line;


			while((line = reader.readLine()) != null){
				s.loadUser(line);
			}
			reader.close();

		}catch(IOException e){
			System.out.println("file users not found! Resetting Server");
		} catch (ParseException e) {
			System.out.println("critical error: parsing file");
		}

		return s;
	}

	public static void saveState(Users s) {

		if(s.getRegisteredUsers() == null)
			return;


		try(BufferedWriter fileWriter = new BufferedWriter(new FileWriter("./users", false))){

			for(String u : s.getRegisteredUsers()){
				fileWriter.write(s.getUser(u).toJSONString());
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

}
