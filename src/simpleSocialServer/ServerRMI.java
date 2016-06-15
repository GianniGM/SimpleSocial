package simpleSocialServer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerRMI implements Runnable{
	Remote users = null;
	Registry registry = null;

	public ServerRMI(Users s, Registry registry) {
		this.users = s;
		this.registry = registry;
	}


	@Override
	public void run() {
		try{
			ContentsServer contentsServer = (ContentsServer) UnicastRemoteObject.exportObject(users , 0);
			registry.rebind(ContentsServer.OBJECT_NAME, contentsServer);
			System.out.println("RMI Server online");
		}catch (RemoteException e) {
			System.out.println("Server error:" + e.getMessage());
		}

	}


}
