package simpleSocialClient;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

import simpleSocialServer.User;

public class ClientRMI extends RemoteObject implements ContentClient {

	private static final long serialVersionUID = 3510900261103810945L;
	private User user;
	
	public ClientRMI(User s) {
		this.user = s;
	}
	
	@Override
	//send content to user when is online
	public boolean pushContent(String name, String content) throws RemoteException {
		System.out.println(new StringBuilder().append("Notify: ").append(name).append(" send a content").toString());
		return user.addContents(content, true);
	}

	@Override
	public String getName() throws RemoteException {
		return user.getUsername();
	}

}
