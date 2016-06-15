package simpleSocialClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Listener implements Runnable {
	private InetSocketAddress address;
	private int timeout = 5000;
	public static boolean EXIT = false;
	
	public Listener( InetSocketAddress inetSocketAddress) {
		this.address = inetSocketAddress;
	}

	@Override
	//wait for a notify
	public void run() {
		System.out.println("started listener");
		//create a server socket listened to a random port
		try(ServerSocket listener = new ServerSocket(address.getPort());){
			listener.setSoTimeout(timeout);

			while(!EXIT){
				try{
					Socket s = listener.accept();
					BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-16"));

					String line = reader.readLine();
					if(line != null){
						System.out.println("Notify: " + line);
					}
					s.close();
					reader.close();
				}catch(IOException e){
				}
			}


		}catch(IOException e ){
			System.out.println("connecton refused");
		}finally{
			System.out.print(" Bye! ");
		}
	}

}
