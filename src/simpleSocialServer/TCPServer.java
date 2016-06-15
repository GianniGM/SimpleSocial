package simpleSocialServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class TCPServer implements Runnable {


	private static final int TCP_THREADS = 20;

	private Users s;
	private InetAddress address;

	public TCPServer(Users s, InetAddress address){
		this.s = s;
		this.address = address;
	}

	@Override
	public void run() {
		ExecutorService clients = Executors.newFixedThreadPool(TCP_THREADS);

		try(ServerSocket server = new ServerSocket()){
			InetSocketAddress source = new InetSocketAddress(address, SocialServer.PORT);
			server.bind(source);
			while(true){
				try{
					Socket client = server.accept();
					TCPHandler handler = new TCPHandler(client, s);
					clients.submit(handler);
				}catch(NoSuchFieldError e){
					
				} catch (IOException e) {
					System.out.println("Client connection Error");
				}
			}

		} catch (IOException e1) {
			System.out.println("Connection refused");
		}
	}
}
