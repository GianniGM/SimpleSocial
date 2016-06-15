package simpleSocialClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ContentClient extends Remote {
	public boolean pushContent(String name, String content) throws RemoteException;
	public String getName() throws RemoteException;
}
