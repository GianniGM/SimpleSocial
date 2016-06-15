package simpleSocialServer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import simpleSocialClient.ContentClient;

public interface ContentsServer extends Remote{

	public static final String OBJECT_NAME ="SIMPLE_SOCIAL";
	
	//RMI ACTIVITIES
	public boolean registerCallback(UUID id, ContentClient callback) throws RemoteException;
	public boolean follow(UUID id,String following) throws RemoteException;
	public boolean unfollow(UUID id, String following)throws RemoteException;
	public boolean sendContent(UUID id, String content) throws RemoteException;
	public boolean exit(UUID id) throws RemoteException;

}
